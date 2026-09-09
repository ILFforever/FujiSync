package com.ilfforever.fujisync.data.ptp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PtpContainerReaderTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun container(
        type: Int,
        code: Int,
        transactionId: Int,
        payload: ByteArray = ByteArray(0),
    ): ByteArray {
        val length = PtpContainer.HEADER_BYTES + payload.size
        return ByteBuffer.allocate(length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(length)
            .putShort(type.toShort())
            .putShort(code.toShort())
            .putInt(transactionId)
            .put(payload)
            .array()
    }

    private fun response(transactionId: Int) =
        container(PtpConstants.CONTAINER_RESPONSE, PtpConstants.RESPONSE_OK, transactionId)

    // ── whole containers ──────────────────────────────────────────────────────

    @Test
    fun `returns null until a full header has arrived`() {
        val reader = PtpContainerReader()
        reader.append(response(1).copyOf(11))
        assertNull(reader.next())
    }

    @Test
    fun `reads a container delivered in one append`() {
        val reader = PtpContainerReader()
        reader.append(response(7))

        val container = reader.next()
        assertNotNull(container)
        assertEquals(PtpConstants.CONTAINER_RESPONSE, container!!.type)
        assertEquals(PtpConstants.RESPONSE_OK, container.code)
        assertEquals(7, container.transactionId)
        assertEquals(0, reader.buffered)
    }

    // ── split across reads ────────────────────────────────────────────────────

    @Test
    fun `reassembles a container split across two appends`() {
        val bytes = container(PtpConstants.CONTAINER_DATA, 0x1001, 3, ByteArray(40) { it.toByte() })
        val reader = PtpContainerReader()

        reader.append(bytes.copyOf(20))
        assertNull(reader.next())

        reader.append(bytes.copyOfRange(20, bytes.size))
        val container = reader.next()
        assertNotNull(container)
        assertArrayEquals(ByteArray(40) { it.toByte() }, container!!.payload)
    }

    @Test
    fun `reassembles a container delivered one byte at a time`() {
        val bytes = container(PtpConstants.CONTAINER_DATA, 0x1015, 9, byteArrayOf(1, 2, 3, 4))
        val reader = PtpContainerReader()

        bytes.dropLast(1).forEach { byte ->
            reader.append(byteArrayOf(byte))
            assertNull(reader.next())
        }

        reader.append(byteArrayOf(bytes.last()))
        assertEquals(9, reader.next()?.transactionId)
    }

    // ── the case the old reader lost ──────────────────────────────────────────

    @Test
    fun `keeps a second container that arrived in the same read`() {
        val data = container(PtpConstants.CONTAINER_DATA, 0x1015, 5, byteArrayOf(7, 0))
        val reader = PtpContainerReader()
        reader.append(data + response(5))

        assertEquals(PtpConstants.CONTAINER_DATA, reader.next()?.type)
        // Before the framer existed this container was read into the buffer and thrown away, and
        // the next exchange started mid-stream.
        assertEquals(PtpConstants.CONTAINER_RESPONSE, reader.next()?.type)
        assertNull(reader.next())
    }

    @Test
    fun `honours append length rather than array size`() {
        val bytes = response(2)
        val padded = bytes + ByteArray(64) { 0xFF.toByte() }
        val reader = PtpContainerReader()

        reader.append(padded, bytes.size)
        assertEquals(2, reader.next()?.transactionId)
        assertEquals(0, reader.buffered)
    }

    // ── desync ────────────────────────────────────────────────────────────────

    @Test
    fun `rejects a container length below the header size`() {
        val reader = PtpContainerReader()
        reader.append(
            ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(4).putShort(3).putShort(0x2001).putInt(1).array(),
        )

        val error = assertThrows(PtpTransportException::class.java) { reader.next() }
        assertEquals(PtpTransportException.Kind.Desync, error.kind)
    }

    @Test
    fun `rejects a container length beyond the maximum`() {
        val reader = PtpContainerReader(maxContainerBytes = 1024)
        reader.append(
            ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(4096).putShort(2).putShort(0x1001).putInt(1).array(),
        )

        val error = assertThrows(PtpTransportException::class.java) { reader.next() }
        assertEquals(PtpTransportException.Kind.Desync, error.kind)
    }

    @Test
    fun `rejects a high length that would read as a negative int`() {
        val reader = PtpContainerReader()
        val bytes = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(-1) // 0xFFFFFFFF — 4294967295 unsigned
            .putShort(3).putShort(0x2001).putInt(1).array()
        reader.append(bytes)

        val error = assertThrows(PtpTransportException::class.java) { reader.next() }
        assertEquals(PtpTransportException.Kind.Desync, error.kind)
    }

    @Test
    fun `rejects buffered input that never forms a container`() {
        val reader = PtpContainerReader(maxContainerBytes = 512)
        // A plausible header, then a stream that never completes it.
        reader.append(
            ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(512).putShort(2).putShort(0x1001).putInt(1).array(),
        )

        val error = assertThrows(PtpTransportException::class.java) {
            repeat(10) { reader.append(ByteArray(128)) }
        }
        assertEquals(PtpTransportException.Kind.Desync, error.kind)
    }

    // ── reset ─────────────────────────────────────────────────────────────────

    @Test
    fun `reset drops a partial container`() {
        val bytes = container(PtpConstants.CONTAINER_DATA, 0x1001, 1, ByteArray(32))
        val reader = PtpContainerReader()

        reader.append(bytes.copyOf(20))
        assertEquals(20, reader.buffered)

        reader.reset()
        assertEquals(0, reader.buffered)
        assertNull(reader.next())

        reader.append(response(2))
        assertEquals(2, reader.next()?.transactionId)
    }

    // ── growth ────────────────────────────────────────────────────────────────

    @Test
    fun `handles a container larger than the initial capacity`() {
        val payload = ByteArray(200_000) { (it % 251).toByte() }
        val bytes = container(PtpConstants.CONTAINER_DATA, 0x1001, 4, payload)
        val reader = PtpContainerReader()

        var offset = 0
        while (offset < bytes.size) {
            val size = minOf(16_384, bytes.size - offset)
            reader.append(bytes.copyOfRange(offset, offset + size))
            offset += size
        }

        val container = reader.next()
        assertNotNull(container)
        assertArrayEquals(payload, container!!.payload)
    }
}
