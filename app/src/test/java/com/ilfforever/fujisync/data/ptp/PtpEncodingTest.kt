package com.ilfforever.fujirecipes.data.ptp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PtpEncodingTest {

    // ── encodePtpString ───────────────────────────────────────────────────────

    @Test
    fun `encodePtpString empty string returns single zero byte`() {
        val result = encodePtpString("")
        assertArrayEquals(byteArrayOf(0), result)
    }

    @Test
    fun `encodePtpString single ASCII char has correct length byte and LE UTF-16 codeunit plus null terminator`() {
        // "A" → length byte = 2 (char + null terminator), then 'A'=0x41,0x00, then 0x00,0x00
        val result = encodePtpString("A")
        assertEquals(1 + 2 * 2, result.size) // 1 length + 2 shorts
        assertEquals(2.toByte(), result[0])   // count includes null terminator
        assertEquals(0x41.toByte(), result[1]) // 'A' low byte
        assertEquals(0x00.toByte(), result[2]) // 'A' high byte
        assertEquals(0x00.toByte(), result[3]) // null terminator low
        assertEquals(0x00.toByte(), result[4]) // null terminator high
    }

    @Test
    fun `encodePtpString three chars has correct size`() {
        val result = encodePtpString("Hi!")
        // length byte=1, then (3 chars + 1 null) * 2 bytes each
        assertEquals(1 + (3 + 1) * 2, result.size)
        assertEquals(4.toByte(), result[0]) // "Hi!" + null = 4
    }

    @Test
    fun `encodePtpString truncates at 254 chars`() {
        val long = "X".repeat(300)
        val result = encodePtpString(long)
        // length byte = 255, then 254 chars + null terminator = 255 shorts
        assertEquals(255.toByte(), result[0])
        assertEquals(1 + 255 * 2, result.size)
    }

    // ── decodeInt16Le / decodeUInt16Le ────────────────────────────────────────

    @Test
    fun `decodeInt16Le reads signed negative value`() {
        val bytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(-1).array()
        assertEquals(-1, decodeInt16Le(bytes))
    }

    @Test
    fun `decodeInt16Le returns null for too-short array`() {
        assertNull(decodeInt16Le(byteArrayOf(0x01)))
    }

    @Test
    fun `decodeUInt16Le treats 0xFFFF as 65535`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
        assertEquals(65535, decodeUInt16Le(bytes))
    }

    @Test
    fun `decodeUInt16Le reads little-endian 0x0102 correctly`() {
        // LE: low byte first → 0x02 0x01 = value 0x0102 = 258
        val bytes = byteArrayOf(0x02, 0x01)
        assertEquals(0x0102, decodeUInt16Le(bytes))
    }

    // ── hexDump ───────────────────────────────────────────────────────────────

    @Test
    fun `hexDump empty array returns marker string`() {
        assertEquals("<empty>", hexDump(byteArrayOf()))
    }

    @Test
    fun `hexDump single byte formats correctly`() {
        assertEquals("AB", hexDump(byteArrayOf(0xAB.toByte())))
    }

    @Test
    fun `hexDump two bytes space-separated`() {
        assertEquals("0A FF", hexDump(byteArrayOf(0x0A, 0xFF.toByte())))
    }

    @Test
    fun `hexDump truncates and appends continuation marker`() {
        val bytes = ByteArray(40) { it.toByte() }
        val result = hexDump(bytes, maxBytes = 4)
        assertTrue(result.endsWith("…(+36)"))
        // Only 4 bytes shown before the ellipsis
        assertEquals("00 01 02 03 …(+36)", result)
    }

    // ── uint16Le ──────────────────────────────────────────────────────────────

    @Test
    fun `uint16Le encodes zero`() {
        assertArrayEquals(byteArrayOf(0, 0), uint16Le(0))
    }

    @Test
    fun `uint16Le encodes 256 as LE`() {
        // 256 = 0x0100 → LE = 00 01
        assertArrayEquals(byteArrayOf(0x00, 0x01), uint16Le(256))
    }

    @Test
    fun `uint16Le encodes 0xFFFF`() {
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte()), uint16Le(0xFFFF))
    }
}
