package com.ilfforever.fujisync.data.ptp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PtpPacketTest {

    // ── PtpContainer.parse ────────────────────────────────────────────────────

    @Test
    fun `parse minimal 12-byte header with no payload`() {
        val bytes = buildHeader(length = 12, type = 3, code = 0x2001, txId = 1)
        val container = PtpContainer.parse(bytes)
        assertEquals(12, container.length)
        assertEquals(3, container.type)
        assertEquals(0x2001, container.code)
        assertEquals(1, container.transactionId)
        assertEquals(0, container.payload.size)
    }

    @Test
    fun `parse packet with payload extracts correct payload bytes`() {
        val payload = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte())
        val bytes = buildHeader(length = 12 + 4, type = 2, code = 0x1015, txId = 42) + payload
        val container = PtpContainer.parse(bytes)
        assertEquals(16, container.length)
        assertArrayEquals(payload, container.payload)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse throws on too-short byte array`() {
        PtpContainer.parse(ByteArray(8))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `PtpContainer init rejects invalid type 0`() {
        PtpContainer(length = 12, type = 0, code = 0x2001, transactionId = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `PtpContainer init rejects invalid type 5`() {
        PtpContainer(length = 12, type = 5, code = 0x2001, transactionId = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `PtpContainer init rejects length below 12`() {
        PtpContainer(length = 8, type = 1, code = 0x1001, transactionId = 0)
    }

    // ── buildCommandPacket ────────────────────────────────────────────────────

    @Test
    fun `buildCommandPacket no params produces 12-byte packet`() {
        val bytes = buildCommandPacket(code = 0x1001, transactionId = 1)
        assertEquals(12, bytes.size)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(12, buf.int)            // length
        assertEquals(PtpConstants.CONTAINER_COMMAND.toShort(), buf.short) // type=1
        assertEquals(0x1001.toShort(), buf.short) // code
        assertEquals(1, buf.int)             // txId
    }

    @Test
    fun `buildCommandPacket with two params produces 20-byte packet`() {
        val bytes = buildCommandPacket(code = 0x1002, transactionId = 5, params = listOf(0xDEAD, 0xBEEF))
        assertEquals(20, bytes.size)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(20, buf.int)      // length
        buf.short                      // type
        buf.short                      // code
        buf.int                        // txId
        assertEquals(0xDEAD, buf.int)
        assertEquals(0xBEEF, buf.int)
    }

    @Test
    fun `buildCommandPacket result is parseable by PtpContainer`() {
        val bytes = buildCommandPacket(code = 0x1002, transactionId = 3)
        val container = PtpContainer.parse(bytes)
        assertEquals(0x1002, container.code)
        assertEquals(3, container.transactionId)
        assertEquals(PtpConstants.CONTAINER_COMMAND, container.type)
    }

    // ── buildDataOutPacket ────────────────────────────────────────────────────

    @Test
    fun `buildDataOutPacket encodes payload and sets DATA type`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        val bytes = buildDataOutPacket(code = 0x1016, transactionId = 7, payload = payload)
        assertEquals(12 + 3, bytes.size)
        val container = PtpContainer.parse(bytes)
        assertEquals(PtpConstants.CONTAINER_DATA, container.type)
        assertEquals(0x1016, container.code)
        assertArrayEquals(payload, container.payload)
    }

    @Test
    fun `buildDataOutPacket with empty payload produces 12-byte packet`() {
        val bytes = buildDataOutPacket(code = 0x1016, transactionId = 1, payload = byteArrayOf())
        assertEquals(12, bytes.size)
    }

    // ── PtpTransaction.isOk ───────────────────────────────────────────────────

    @Test
    fun `PtpTransaction isOk true when response code is RESPONSE_OK`() {
        val response = PtpContainer(12, 3, PtpConstants.RESPONSE_OK, 1)
        val tx = PtpTransaction(data = null, response = response)
        assertTrue(tx.isOk)
    }

    @Test
    fun `PtpTransaction isOk false for non-OK response code`() {
        val response = PtpContainer(12, 3, 0x2005, 1)
        val tx = PtpTransaction(data = null, response = response)
        assertTrue(!tx.isOk)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildHeader(length: Int, type: Int, code: Int, txId: Int): ByteArray =
        ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(length)
            .putShort(type.toShort())
            .putShort(code.toShort())
            .putInt(txId)
            .array()

    private operator fun ByteArray.plus(other: ByteArray): ByteArray {
        val result = ByteArray(size + other.size)
        copyInto(result)
        other.copyInto(result, size)
        return result
    }
}
