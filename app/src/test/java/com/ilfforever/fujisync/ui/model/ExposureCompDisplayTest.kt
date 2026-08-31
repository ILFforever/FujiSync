package com.ilfforever.fujisync.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ExposureCompDisplayTest {
    @Test
    fun `formats exposure compensation in camera thirds`() {
        assertEquals("+0", formatExposureComp(0f))
        assertEquals("+1/3", formatExposureComp(1f / 3f))
        assertEquals("+2/3", formatExposureComp(2f / 3f))
        assertEquals("+1 1/3", formatExposureComp(4f / 3f))
        assertEquals("−2/3", formatExposureComp(-2f / 3f))
    }

    @Test
    fun `normalizes imported decimal thirds for display`() {
        assertEquals("+1/3", formatExposureComp(0.3f))
        assertEquals("+2/3", formatExposureComp(0.7f))
    }
}
