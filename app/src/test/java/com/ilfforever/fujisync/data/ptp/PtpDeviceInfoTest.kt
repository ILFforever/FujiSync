package com.ilfforever.fujisync.data.ptp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * DeviceInfo is the first payload we parse and the one a body most often returns in a shape we did
 * not expect, so the layout is pinned here rather than only being exercised against hardware.
 */
class PtpDeviceInfoTest {

    // ── payload builder ───────────────────────────────────────────────────────

    private class DeviceInfoBuilder {
        private val out = ByteArrayOutputStream()

        fun uint16(value: Int) = apply { out.write(le(2) { putShort(value.toShort()) }) }
        fun uint32(value: Int) = apply { out.write(le(4) { putInt(value) }) }

        /** [terminated] off models a body that declares only the characters it actually sends. */
        fun string(value: String, terminated: Boolean = true) = apply {
            if (value.isEmpty() && terminated) {
                out.write(0)
                return@apply
            }
            val count = value.length + if (terminated) 1 else 0
            out.write(count)
            value.forEach { out.write(le(2) { putShort(it.code.toShort()) }) }
            if (terminated) out.write(le(2) { putShort(0) })
        }

        fun uint16Array(values: List<Int>) = apply {
            uint32(values.size)
            values.forEach { uint16(it) }
        }

        /** Writes a count that does not match the values that follow. */
        fun lyingUint16Array(declared: Int, values: List<Int>) = apply {
            uint32(declared)
            values.forEach { uint16(it) }
        }

        fun build(): ByteArray = out.toByteArray()

        private fun le(size: Int, write: ByteBuffer.() -> Unit): ByteArray =
            ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).apply(write).array()
    }

    private fun deviceInfo(
        operations: List<Int> = listOf(0x1001, 0x1002, 0x1015, 0x1016),
        properties: List<Int> = listOf(PtpConstants.FUJI_SLOT_SELECTOR, 0xD18D),
        manufacturer: String = "FUJIFILM",
        model: String = "X-T30 II",
        version: String = "1.20",
        serial: String = "ABC123",
        modelTerminated: Boolean = true,
    ): ByteArray = DeviceInfoBuilder()
        .uint16(100)          // StandardVersion
        .uint32(0x0000000C)   // VendorExtensionID
        .uint16(100)          // VendorExtensionVersion
        .string("fujifilm.co.jp: 1.0;")
        .uint16(0)            // FunctionalMode
        .uint16Array(operations)
        .uint16Array(listOf(0x4002, 0x4006)) // EventsSupported
        .uint16Array(properties)
        .uint16Array(emptyList())            // CaptureFormats
        .uint16Array(listOf(0x3801))         // ImageFormats
        .string(manufacturer)
        .string(model, terminated = modelTerminated)
        .string(version)
        .string(serial)
        .build()

    // ── layout ────────────────────────────────────────────────────────────────

    @Test
    fun `parses the four identity strings in order`() {
        val info = parseDeviceInfo(deviceInfo())

        assertEquals("FUJIFILM", info.manufacturer)
        assertEquals("X-T30 II", info.model)
        assertEquals("1.20", info.deviceVersion)
        assertEquals("ABC123", info.serialNumber)
    }

    @Test
    fun `reads operations and device properties past the arrays it skips`() {
        val info = parseDeviceInfo(deviceInfo())

        assertEquals(listOf(0x1001, 0x1002, 0x1015, 0x1016), info.supportedOperations)
        assertEquals(listOf(PtpConstants.FUJI_SLOT_SELECTOR, 0xD18D), info.supportedDeviceProperties)
        assertTrue(info.supportsFujiRecipeSlots)
        assertTrue(info.supportsOperation(PtpConstants.SET_DEVICE_PROP_VALUE))
    }

    @Test
    fun `reports no recipe slots when the slot selector is absent`() {
        val info = parseDeviceInfo(deviceInfo(properties = listOf(0x5001, 0x5003)))

        assertFalse(info.supportsFujiRecipeSlots)
    }

    // ── strings ───────────────────────────────────────────────────────────────

    @Test
    fun `keeps every character of a string with no null terminator`() {
        // The old reader dropped the last code unit unconditionally, turning this into "X-T30 I".
        val info = parseDeviceInfo(deviceInfo(model = "X-T30 II", modelTerminated = false))

        assertEquals("X-T30 II", info.model)
        // Fields after the unterminated one must still line up.
        assertEquals("1.20", info.deviceVersion)
        assertEquals("ABC123", info.serialNumber)
    }

    @Test
    fun `handles empty strings without consuming the next field`() {
        val info = parseDeviceInfo(deviceInfo(manufacturer = "", model = "X-H2"))

        assertEquals("", info.manufacturer)
        assertEquals("X-H2", info.model)
        assertEquals("1.20", info.deviceVersion)
    }

    // ── malformed payloads ────────────────────────────────────────────────────

    @Test
    fun `throws on a payload too short to be DeviceInfo`() {
        assertThrows(PtpProtocolException::class.java) { parseDeviceInfo(ByteArray(8)) }
    }

    @Test
    fun `throws when an array count overruns the payload`() {
        val bytes = DeviceInfoBuilder()
            .uint16(100)
            .uint32(0x0000000C)
            .uint16(100)
            .string("x")
            .uint16(0)
            .lyingUint16Array(declared = 100_000, values = listOf(0x1001))
            .build()

        assertThrows(PtpProtocolException::class.java) { parseDeviceInfo(bytes) }
    }

    @Test
    fun `throws when the payload ends mid-string`() {
        val full = deviceInfo()
        assertThrows(PtpProtocolException::class.java) {
            parseDeviceInfo(full.copyOf(full.size - 6))
        }
    }
}
