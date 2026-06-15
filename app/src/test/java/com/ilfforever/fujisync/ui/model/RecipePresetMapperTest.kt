package com.ilfforever.fujirecipes.ui.model

import com.ilfforever.fujirecipes.domain.model.CameraSlot
import com.ilfforever.fujirecipes.domain.model.FujiPropertyCode
import com.ilfforever.fujirecipes.domain.model.RecipePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipePresetMapperTest {

    private fun preset(
        slot: CameraSlot = CameraSlot.C1,
        name: String = "Test",
        props: Map<FujiPropertyCode, Int> = emptyMap(),
    ) = RecipePreset(slot = slot, name = name, properties = props)

    // ── slot & name ───────────────────────────────────────────────────────────

    @Test
    fun `toUiModel propagates slot label`() {
        val ui = preset(slot = CameraSlot.C3).toUiModel()
        assertEquals("C3", ui.slot)
    }

    @Test
    fun `toUiModel uses slot label as name when name is blank`() {
        val ui = preset(slot = CameraSlot.C5, name = "  ").toUiModel()
        assertEquals("C5", ui.name)
    }

    @Test
    fun `toUiModel preserves non-blank name`() {
        val ui = preset(name = "Velvia Street").toUiModel()
        assertEquals("Velvia Street", ui.name)
    }

    // ── film simulation ───────────────────────────────────────────────────────

    @Test
    fun `toUiModel maps Provia protocolValue 1 to correct label`() {
        val ui = preset(props = mapOf(FujiPropertyCode.FilmSimulation to 1)).toUiModel()
        assertEquals("Provia / Standard", ui.sim)
    }

    @Test
    fun `toUiModel maps Velvia protocolValue 2`() {
        val ui = preset(props = mapOf(FujiPropertyCode.FilmSimulation to 2)).toUiModel()
        assertEquals("Velvia / Vivid", ui.sim)
    }

    @Test
    fun `toUiModel falls back to Provia label for unknown sim code`() {
        val ui = preset(props = mapOf(FujiPropertyCode.FilmSimulation to 99)).toUiModel()
        assertEquals("Provia / Standard", ui.sim)
    }

    @Test
    fun `toUiModel identifies mono simulation for Monochrome code 6`() {
        val ui = preset(props = mapOf(FujiPropertyCode.FilmSimulation to 6)).toUiModel()
        // Mono simulations: Color key absent from tone map, WB shifts absent from wb map
        assertFalse(ui.tone.containsKey("Color"))
        assertFalse(ui.wb.containsKey("WB Shift R"))
    }

    @Test
    fun `toUiModel includes Color in tone for non-mono simulation`() {
        val ui = preset(props = mapOf(FujiPropertyCode.FilmSimulation to 1)).toUiModel()
        assertTrue(ui.tone.containsKey("Color"))
    }

    // ── dynamic range ─────────────────────────────────────────────────────────

    @Test
    fun `toUiModel maps DR200 correctly`() {
        val ui = preset(props = mapOf(FujiPropertyCode.DynamicRange to 200)).toUiModel()
        assertEquals("DR200%", ui.effects["Dynamic Range"])
    }

    @Test
    fun `toUiModel uses DR Auto for unmapped value`() {
        val ui = preset(props = mapOf(FujiPropertyCode.DynamicRange to 0)).toUiModel()
        assertEquals("DR Auto", ui.effects["Dynamic Range"])
    }

    @Test
    fun `toUiModel maps D Range Priority values and uses DRP pill`() {
        val ui = preset(
            props = mapOf(
                FujiPropertyCode.DynamicRange to 400,
                FujiPropertyCode.DRangePriority to 32768,
            ),
        ).toUiModel()

        assertEquals("Auto", ui.effects["D Range Priority"])
        assertEquals("DR400%", ui.effects["Dynamic Range"])
        assertTrue(ui.pills.contains("DRP AUTO"))
        assertFalse(ui.pills.contains("DR400%"))
    }

    @Test
    fun `toPreset omits Dynamic Range when D Range Priority is active`() {
        val preset = RecipeUiModel(
            slot = "C1",
            name = "Priority",
            sim = "Provia / Standard",
            pills = emptyList(),
            effects = mapOf(
                "D Range Priority" to "Strong",
                "Dynamic Range" to "DR400%",
                "Grain Effect" to "Off",
                "Color Chrome" to "Off",
                "Color Chrome FX Blue" to "Off",
                "Smooth Skin" to "Off",
            ),
        ).toPreset(CameraSlot.C1)

        assertEquals(2, preset.properties[FujiPropertyCode.DRangePriority])
        assertFalse(preset.properties.containsKey(FujiPropertyCode.DynamicRange))
    }

    @Test
    fun `toPreset writes Dynamic Range when D Range Priority is Off`() {
        val preset = RecipeUiModel(
            slot = "C1",
            name = "Manual DR",
            sim = "Provia / Standard",
            pills = emptyList(),
            effects = mapOf(
                "D Range Priority" to "Off",
                "Dynamic Range" to "DR200%",
                "Grain Effect" to "Off",
                "Color Chrome" to "Off",
                "Color Chrome FX Blue" to "Off",
                "Smooth Skin" to "Off",
            ),
        ).toPreset(CameraSlot.C1)

        assertEquals(0, preset.properties[FujiPropertyCode.DRangePriority])
        assertEquals(200, preset.properties[FujiPropertyCode.DynamicRange])
    }

    @Test
    fun `toPreset accepts FXW Dynamic Range labels without percent`() {
        val preset = RecipeUiModel(
            slot = "C1",
            name = "FXW DR",
            sim = "Provia / Standard",
            pills = emptyList(),
            effects = mapOf(
                "D Range Priority" to "Off",
                "Dynamic Range" to "DR400",
                "Grain Effect" to "Off",
                "Color Chrome" to "Off",
                "Color Chrome FX Blue" to "Off",
                "Smooth Skin" to "Off",
            ),
        ).toPreset(CameraSlot.C1)

        assertEquals(400, preset.properties[FujiPropertyCode.DynamicRange])
    }

    // ── grain effect ──────────────────────────────────────────────────────────

    @Test
    fun `toUiModel maps grain code 3 to Strong Small`() {
        val ui = preset(props = mapOf(FujiPropertyCode.GrainEffect to 3)).toUiModel()
        assertEquals("Strong Small", ui.effects["Grain Effect"])
    }

    @Test
    fun `toUiModel maps grain Off and omits pill`() {
        val ui = preset(props = mapOf(FujiPropertyCode.GrainEffect to 0)).toUiModel()
        assertEquals("Off", ui.effects["Grain Effect"])
        assertTrue(ui.pills.none { it.startsWith("GRAIN") })
    }

    @Test
    fun `toUiModel maps grain code 1 to Off`() {
        // Reference: both 1 and 6 decode to Off.
        val ui = preset(props = mapOf(FujiPropertyCode.GrainEffect to 1)).toUiModel()
        assertEquals("Off", ui.effects["Grain Effect"])
        assertTrue(ui.pills.none { it.startsWith("GRAIN") })
    }

    @Test
    fun `toUiModel adds GRAIN pill for non-Off grain`() {
        val ui = preset(props = mapOf(FujiPropertyCode.GrainEffect to 2)).toUiModel()
        assertEquals("Weak Small", ui.effects["Grain Effect"])
        assertTrue(ui.pills.any { it.startsWith("GRAIN") })
    }

    // ── ×10-scaled tone display ─────────────────────────────────────────────────

    @Test
    fun `toUiModel scales highlight wire value by ten`() {
        // PTP wire = dial × 10; raw 20 → dial +2.
        val ui = preset(props = mapOf(FujiPropertyCode.HighlightTone to 20)).toUiModel()
        assertEquals("+2", ui.tone["Highlight Tone"])
    }

    @Test
    fun `toUiModel renders half-step tone`() {
        val ui = preset(props = mapOf(FujiPropertyCode.ShadowTone to -15)).toUiModel()
        // Unicode minus − not hyphen
        assertEquals("−1.5", ui.tone["Shadow Tone"])
    }

    @Test
    fun `toUiModel scales color wire value by ten`() {
        val ui = preset(props = mapOf(FujiPropertyCode.Color to 20)).toUiModel()
        assertEquals("+2", ui.tone["Color"])
    }

    @Test
    fun `toUiModel shows zero as plain 0`() {
        val ui = preset(props = mapOf(FujiPropertyCode.HighlightTone to 0)).toUiModel()
        assertEquals("0", ui.tone["Highlight Tone"])
    }

    // ── Off / Weak / Strong (1=Off, 2=Weak, 3=Strong) ───────────────────────────

    @Test
    fun `toUiModel maps color chrome 1 to Off and 3 to Strong`() {
        val off = preset(props = mapOf(FujiPropertyCode.ColorChrome to 1)).toUiModel()
        assertEquals("Off", off.effects["Color Chrome"])
        val strong = preset(props = mapOf(FujiPropertyCode.ColorChrome to 3)).toUiModel()
        assertEquals("Strong", strong.effects["Color Chrome"])
    }

    // ── white balance display ─────────────────────────────────────────────────

    @Test
    fun `toUiModel maps WB code 4 to Daylight`() {
        val ui = preset(props = mapOf(FujiPropertyCode.WhiteBalance to 4)).toUiModel()
        assertEquals("Daylight", ui.wb["White Balance"])
    }

    @Test
    fun `toUiModel shows color temperature for Kelvin mode`() {
        val ui = preset(
            props = mapOf(
                FujiPropertyCode.WhiteBalance to 0x8007,
                FujiPropertyCode.ColorTemperature to 7500,
            )
        ).toUiModel()
        assertEquals("7500K", ui.wb["White Balance"])
    }

    // ── High ISO NR non-linear lookup ──────────────────────────────────────────

    @Test
    fun `toUiModel maps NR 8192 to neutral`() {
        val ui = preset(props = mapOf(FujiPropertyCode.HighIsoNr to 8192)).toUiModel()
        assertEquals("0", ui.tone["High ISO NR"])
    }

    @Test
    fun `toUiModel maps NR raw 0 to dial +2`() {
        val ui = preset(props = mapOf(FujiPropertyCode.HighIsoNr to 0)).toUiModel()
        assertEquals("+2", ui.tone["High ISO NR"])
    }

    @Test
    fun `toUiModel maps NR raw 32768 to dial -4`() {
        val ui = preset(props = mapOf(FujiPropertyCode.HighIsoNr to 32768)).toUiModel()
        assertEquals("−4", ui.tone["High ISO NR"])
    }

    // ── round-trip (raw → UI → raw) preserves wire values ───────────────────────

    @Test
    fun `round-trip preserves scaled tone grain ows and NR wire values`() {
        val original = mapOf(
            FujiPropertyCode.FilmSimulation to 1,
            FujiPropertyCode.DynamicRange to 200,
            FujiPropertyCode.GrainEffect to 3,      // Strong Small
            FujiPropertyCode.ColorChrome to 2,      // Weak
            FujiPropertyCode.SmoothSkin to 3,       // Strong
            FujiPropertyCode.HighlightTone to 15,   // +1.5
            FujiPropertyCode.Color to 40,           // +4
            FujiPropertyCode.Sharpness to -20,      // −2
            FujiPropertyCode.Clarity to 30,         // +3
            FujiPropertyCode.HighIsoNr to 4096,     // +1
        )
        val rebuilt = preset(props = original).toUiModel().toPreset(CameraSlot.C1).properties

        assertEquals(200, rebuilt[FujiPropertyCode.DynamicRange])
        assertEquals(3, rebuilt[FujiPropertyCode.GrainEffect])
        assertEquals(2, rebuilt[FujiPropertyCode.ColorChrome])
        assertEquals(3, rebuilt[FujiPropertyCode.SmoothSkin])
        assertEquals(15, rebuilt[FujiPropertyCode.HighlightTone])
        assertEquals(40, rebuilt[FujiPropertyCode.Color])
        assertEquals(-20, rebuilt[FujiPropertyCode.Sharpness])
        assertEquals(30, rebuilt[FujiPropertyCode.Clarity])
        assertEquals(4096, rebuilt[FujiPropertyCode.HighIsoNr])
    }

    @Test
    fun `toPreset encodes grain Off as 6`() {
        val ui = preset(props = mapOf(FujiPropertyCode.GrainEffect to 6)).toUiModel()
        assertEquals(6, ui.toPreset(CameraSlot.C1).properties[FujiPropertyCode.GrainEffect])
    }
}
