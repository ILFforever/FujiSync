package com.ilfforever.fujisync.data.ptp

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies that encodePtpString + parsePtpString form a lossless round-trip
 * for all strings the camera accepts.
 */
class PtpStringRoundTripTest {

    private fun roundTrip(input: String): String = parsePtpString(encodePtpString(input))

    @Test
    fun `round-trip empty string`() {
        assertEquals("", roundTrip(""))
    }

    @Test
    fun `round-trip single ASCII character`() {
        assertEquals("A", roundTrip("A"))
    }

    @Test
    fun `round-trip typical recipe name`() {
        assertEquals("Kodak 400TX", roundTrip("Kodak 400TX"))
    }

    @Test
    fun `round-trip name with apostrophe`() {
        assertEquals("Nat's Recipe", roundTrip("Nat's Recipe"))
    }

    @Test
    fun `round-trip maximum length name`() {
        val name = "A".repeat(CameraPresetName.MAX_LENGTH)
        // encodePtpString takes 254 chars max; the camera preset name limit is well within that.
        assertEquals(name, roundTrip(name))
    }

    @Test
    fun `round-trip string with digits`() {
        assertEquals("C1 500T v2", roundTrip("C1 500T v2"))
    }
}
