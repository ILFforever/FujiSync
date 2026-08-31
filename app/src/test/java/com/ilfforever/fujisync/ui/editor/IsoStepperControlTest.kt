package com.ilfforever.fujisync.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsoStepperControlTest {
    @Test
    fun `ISO advances through camera values instead of adding 100`() {
        assertEquals(50, nextIso(null))
        assertEquals(125, nextIso(100))
        assertEquals(160, nextIso(125))
        assertEquals(12800, nextIso(10000))
    }

    @Test
    fun `ISO supports full extended range and unset state`() {
        assertEquals(51200, nextIso(40000))
        assertNull(nextIso(51200))
        assertEquals(40000, previousIso(51200))
        assertNull(previousIso(50))
    }
}
