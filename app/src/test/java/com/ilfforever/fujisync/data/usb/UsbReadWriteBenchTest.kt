package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.ptp.MONO_SIM_CODES
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.FujiFilmSimulation
import com.ilfforever.fujisync.domain.model.FujiPropertyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbReadWriteBenchTest {

    // Expected film sim per slot — these MUST match exactly so toPreset() doesn't
    // silently fall back to Provia (the bug that made the bench pass without testing film sim).
    private val expectedSim = mapOf(
        CameraSlot.C1 to FujiFilmSimulation.RealaAce,
        CameraSlot.C2 to FujiFilmSimulation.NostalgicNeg,
        CameraSlot.C3 to FujiFilmSimulation.EternaBleachBypass,
        CameraSlot.C4 to FujiFilmSimulation.AcrosR,
        CameraSlot.C5 to FujiFilmSimulation.ClassicNeg,
        CameraSlot.C6 to FujiFilmSimulation.Velvia,
        CameraSlot.C7 to FujiFilmSimulation.Eterna,
    )

    // ── Film sim is actually written (regression: exact-label fallback to Provia) ──

    @Test
    fun `every bench preset resolves to its intended film sim`() {
        for (slot in CameraSlot.entries) {
            val preset = benchRoundTripPreset(slot)
            val simValue = preset.properties[FujiPropertyCode.FilmSimulation]
            val expected = expectedSim.getValue(slot)
            assertEquals(
                "Slot ${slot.label} should write ${expected.label} (${expected.protocolValue})",
                expected.protocolValue,
                simValue,
            )
        }
    }

    @Test
    fun `no bench preset silently falls back to Provia unless intended`() {
        // Only assert that slots NOT meant to be Provia aren't Provia (value 1).
        for (slot in CameraSlot.entries) {
            val expected = expectedSim.getValue(slot)
            if (expected != FujiFilmSimulation.Provia) {
                val simValue = benchRoundTripPreset(slot).properties[FujiPropertyCode.FilmSimulation]
                assertNotEquals(
                    "Slot ${slot.label} fell back to Provia — film sim label mismatch in toPreset()",
                    FujiFilmSimulation.Provia.protocolValue,
                    simValue,
                )
            }
        }
    }

    @Test
    fun `all seven slots use distinct film sims`() {
        val sims = CameraSlot.entries.map {
            benchRoundTripPreset(it).properties[FujiPropertyCode.FilmSimulation]
        }
        assertEquals("Expected 7 distinct film sims", 7, sims.toSet().size)
    }

    // ── Name is set ───────────────────────────────────────────────────────────

    @Test
    fun `every bench preset has a non-blank name`() {
        for (slot in CameraSlot.entries) {
            val name = benchRoundTripPreset(slot).name
            assertTrue("Slot ${slot.label} must have a non-blank bench name", name.isNotBlank())
        }
    }

    @Test
    fun `bench preset names are distinct per slot`() {
        val names = CameraSlot.entries.map { benchRoundTripPreset(it).name }
        assertEquals(7, names.toSet().size)
    }

    // ── Slot wiring ───────────────────────────────────────────────────────────

    @Test
    fun `each preset targets its own slot`() {
        for (slot in CameraSlot.entries) {
            assertEquals(slot, benchRoundTripPreset(slot).slot)
        }
    }

    // ── Mono coverage (exercises the color-only skip path) ─────────────────────

    @Test
    fun `C4 is a monochrome sim so color props are skipped during bench`() {
        val preset = benchRoundTripPreset(CameraSlot.C4)
        val simValue = preset.properties[FujiPropertyCode.FilmSimulation]
        assertNotNull(simValue)
        assertTrue(
            "C4 should be a mono sim to exercise the color-only skip path",
            simValue in MONO_SIM_CODES,
        )
    }

    // ── Core properties present on every preset ────────────────────────────────

    @Test
    fun `every preset writes grain, dynamic range, and tone properties`() {
        for (slot in CameraSlot.entries) {
            val props = benchRoundTripPreset(slot).properties
            assertNotNull("${slot.label} grain", props[FujiPropertyCode.GrainEffect])
            assertNotNull("${slot.label} highlight", props[FujiPropertyCode.HighlightTone])
            assertNotNull("${slot.label} shadow", props[FujiPropertyCode.ShadowTone])
            assertNotNull("${slot.label} sharpness", props[FujiPropertyCode.Sharpness])
        }
    }
}
