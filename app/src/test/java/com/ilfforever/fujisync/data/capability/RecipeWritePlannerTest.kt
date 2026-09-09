package com.ilfforever.fujisync.data.capability

import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.FujiFilmSimulation
import com.ilfforever.fujisync.domain.model.FujiPropertyCode
import com.ilfforever.fujisync.domain.model.RecipePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeWritePlannerTest {

    private fun preset(vararg values: Pair<FujiPropertyCode, Int>) = RecipePreset(
        slot = CameraSlot.C1,
        name = "TEST",
        properties = mapOf(*values),
    )

    private fun capability(vararg entries: Pair<FujiPropertyCode, PropertyCapability>) =
        CameraCapability.Unknown.copy(properties = mapOf(*entries))

    private fun present(property: FujiPropertyCode, values: ValueSet = ValueSet.Unknown) =
        property to PropertyCapability(
            property = property,
            support = Support.Present,
            supportSource = CapabilitySource.Camera,
            values = values,
            valuesSource = if (values == ValueSet.Unknown) CapabilitySource.Unknown else CapabilitySource.FujiTable,
        )

    private fun absentPerCamera(property: FujiPropertyCode) = property to PropertyCapability(
        property = property,
        support = Support.Absent,
        supportSource = CapabilitySource.Camera,
        values = ValueSet.Unknown,
        valuesSource = CapabilitySource.Unknown,
    )

    private fun absentPerTable(property: FujiPropertyCode) = property to PropertyCapability(
        property = property,
        support = Support.Absent,
        supportSource = CapabilitySource.FujiTable,
        values = ValueSet.Unknown,
        valuesSource = CapabilitySource.Unknown,
    )

    private fun RecipeWritePlan.valueFor(property: FujiPropertyCode): Int? =
        writes.firstOrNull { it.property == property }?.value

    private fun RecipeWritePlan.skipReason(property: FujiPropertyCode): WriteSkipReason? =
        skipped.firstOrNull { it.property == property }?.reason

    // ── Nothing known: the planner must not gate at all ────────────────────────

    @Test
    fun `unknown capability writes every property unchanged`() {
        val recipe = preset(
            FujiPropertyCode.FilmSimulation to FujiFilmSimulation.RealaAce.protocolValue,
            FujiPropertyCode.Clarity to 20,
            FujiPropertyCode.HighlightTone to 15,
        )

        val plan = RecipeWritePlanner.plan(recipe, CameraCapability.Unknown)

        assertEquals(3, plan.writes.size)
        assertTrue(plan.skipped.isEmpty())
        assertTrue(plan.isClean)
        assertEquals(15, plan.valueFor(FujiPropertyCode.HighlightTone))
    }

    @Test
    fun `film simulation is always written first`() {
        val recipe = preset(
            FujiPropertyCode.Clarity to 10,
            FujiPropertyCode.Sharpness to 0,
            FujiPropertyCode.FilmSimulation to 2,
        )

        val plan = RecipeWritePlanner.plan(recipe, CameraCapability.Unknown)

        assertEquals(FujiPropertyCode.FilmSimulation, plan.writes.first().property)
    }

    // ── Only the camera's own word blocks a write ──────────────────────────────

    @Test
    fun `property the camera does not list is skipped as unsupported`() {
        val recipe = preset(FujiPropertyCode.Clarity to 20)

        val plan = RecipeWritePlanner.plan(recipe, capability(absentPerCamera(FujiPropertyCode.Clarity)))

        assertTrue(plan.writes.isEmpty())
        assertEquals(WriteSkipReason.NotOnThisBody, plan.skipReason(FujiPropertyCode.Clarity))
        assertEquals(1, plan.unsupported.size)
    }

    @Test
    fun `property only Fuji's table doubts is still attempted`() {
        val recipe = preset(FujiPropertyCode.Clarity to 20)

        val plan = RecipeWritePlanner.plan(recipe, capability(absentPerTable(FujiPropertyCode.Clarity)))

        assertEquals(20, plan.valueFor(FujiPropertyCode.Clarity))
        assertTrue(plan.skipped.isEmpty())
    }

    // ── Value ranges ───────────────────────────────────────────────────────────

    @Test
    fun `film simulation above this body's ceiling is sent anyway with a warning`() {
        // An X-T4 (Std4) tops out at Eterna Bleach Bypass; Reala Ace is out of range there. The
        // camera must be the one to reject it, so the write still goes out.
        val recipe = preset(FujiPropertyCode.FilmSimulation to FujiFilmSimulation.RealaAce.protocolValue)
        val body = capability(
            present(FujiPropertyCode.FilmSimulation, ValueSet.Enumerated((1..18).toList())),
        )

        val plan = RecipeWritePlanner.plan(recipe, body)

        assertEquals(20, plan.valueFor(FujiPropertyCode.FilmSimulation))
        assertEquals(1, plan.warnings.size)
        assertFalse(plan.isClean)
    }

    @Test
    fun `film simulation is never substituted for a neighbouring value`() {
        val recipe = preset(FujiPropertyCode.FilmSimulation to 20)
        val body = capability(
            present(FujiPropertyCode.FilmSimulation, ValueSet.Enumerated((1..18).toList())),
        )

        val plan = RecipeWritePlanner.plan(recipe, body)

        assertTrue(plan.adjustments.isEmpty())
    }

    @Test
    fun `half-step highlight tone snaps to the nearest whole step on an older body`() {
        // Std1 bodies have whole steps only: -20,-10,0,10,20,30,40.
        val recipe = preset(FujiPropertyCode.HighlightTone to 15)
        val body = capability(
            present(
                FujiPropertyCode.HighlightTone,
                ValueSet.Enumerated(listOf(-20, -10, 0, 10, 20, 30, 40)),
            ),
        )

        val plan = RecipeWritePlanner.plan(recipe, body)

        val adjustment = plan.adjustments.singleOrNull()
        assertNotNull(adjustment)
        assertEquals(15, adjustment!!.from)
        assertEquals(10, adjustment.to)
        assertEquals(10, plan.valueFor(FujiPropertyCode.HighlightTone))
    }

    @Test
    fun `half-step highlight tone is untouched on a body that has half steps`() {
        val recipe = preset(FujiPropertyCode.HighlightTone to 15)
        val body = capability(
            present(
                FujiPropertyCode.HighlightTone,
                ValueSet.Enumerated(listOf(-20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30, 35, 40)),
            ),
        )

        val plan = RecipeWritePlanner.plan(recipe, body)

        assertTrue(plan.adjustments.isEmpty())
        assertEquals(15, plan.valueFor(FujiPropertyCode.HighlightTone))
    }

    @Test
    fun `colour temperature clamps into a continuous kelvin range`() {
        val recipe = preset(
            FujiPropertyCode.WhiteBalance to 0x8007,
            FujiPropertyCode.ColorTemperature to 12000,
        )
        val body = capability(
            present(FujiPropertyCode.ColorTemperature, ValueSet.Range(2500, 10000, 10)),
        )

        val plan = RecipeWritePlanner.plan(recipe, body)

        assertEquals(10000, plan.valueFor(FujiPropertyCode.ColorTemperature))
    }

    // ── Interlocks confirmed on hardware ───────────────────────────────────────

    @Test
    fun `monochrome simulation suppresses colour-only properties`() {
        val recipe = preset(
            FujiPropertyCode.FilmSimulation to FujiFilmSimulation.Acros.protocolValue,
            FujiPropertyCode.Color to 20,
            FujiPropertyCode.ColorChrome to 2,
            FujiPropertyCode.WbShiftRed to 1,
            FujiPropertyCode.Clarity to 10,
        )

        val plan = RecipeWritePlanner.plan(recipe, CameraCapability.Unknown)

        assertEquals(WriteSkipReason.MonochromeSimulation, plan.skipReason(FujiPropertyCode.Color))
        assertEquals(WriteSkipReason.MonochromeSimulation, plan.skipReason(FujiPropertyCode.ColorChrome))
        assertEquals(WriteSkipReason.MonochromeSimulation, plan.skipReason(FujiPropertyCode.WbShiftRed))
        assertEquals(10, plan.valueFor(FujiPropertyCode.Clarity))
    }

    @Test
    fun `colour simulation suppresses monochrome toning`() {
        // The mirror of the rule above. The camera answers 0x201C to these, confirmed on an X-H2.
        val recipe = preset(
            FujiPropertyCode.FilmSimulation to FujiFilmSimulation.Velvia.protocolValue,
            FujiPropertyCode.MonoWc to 20,
            FujiPropertyCode.MonoMg to -10,
        )

        val plan = RecipeWritePlanner.plan(recipe, CameraCapability.Unknown)

        assertEquals(WriteSkipReason.ColorSimulation, plan.skipReason(FujiPropertyCode.MonoWc))
        assertEquals(WriteSkipReason.ColorSimulation, plan.skipReason(FujiPropertyCode.MonoMg))
    }

    @Test
    fun `mono toning is written under a monochrome simulation`() {
        val recipe = preset(
            FujiPropertyCode.FilmSimulation to FujiFilmSimulation.Monochrome.protocolValue,
            FujiPropertyCode.MonoWc to 20,
        )

        val plan = RecipeWritePlanner.plan(recipe, CameraCapability.Unknown)

        assertEquals(20, plan.valueFor(FujiPropertyCode.MonoWc))
    }

    @Test
    fun `colour temperature is only written in kelvin white balance mode`() {
        val notKelvin = preset(
            FujiPropertyCode.WhiteBalance to 2,
            FujiPropertyCode.ColorTemperature to 5600,
        )
        val kelvin = preset(
            FujiPropertyCode.WhiteBalance to 0x8007,
            FujiPropertyCode.ColorTemperature to 5600,
        )

        assertEquals(
            WriteSkipReason.WhiteBalanceNotKelvin,
            RecipeWritePlanner.plan(notKelvin, CameraCapability.Unknown)
                .skipReason(FujiPropertyCode.ColorTemperature),
        )
        assertEquals(
            5600,
            RecipeWritePlanner.plan(kelvin, CameraCapability.Unknown)
                .valueFor(FujiPropertyCode.ColorTemperature),
        )
    }

    @Test
    fun `active D Range Priority suppresses all three properties it owns`() {
        // Confirmed on X-H2 fw 5.20: while priority is on, Dynamic Range *and* both tone dials are
        // rejected with 0x201C, including values that are legal for the body.
        val recipe = preset(
            FujiPropertyCode.DRangePriority to 2,
            FujiPropertyCode.DynamicRange to 400,
            FujiPropertyCode.HighlightTone to 10,
            FujiPropertyCode.ShadowTone to -10,
            FujiPropertyCode.Clarity to 0,
        )

        val plan = RecipeWritePlanner.plan(recipe, CameraCapability.Unknown)

        assertEquals(
            WriteSkipReason.DynamicRangePriorityActive,
            plan.skipReason(FujiPropertyCode.DynamicRange),
        )
        assertEquals(
            WriteSkipReason.DynamicRangePriorityActive,
            plan.skipReason(FujiPropertyCode.HighlightTone),
        )
        assertEquals(
            WriteSkipReason.DynamicRangePriorityActive,
            plan.skipReason(FujiPropertyCode.ShadowTone),
        )
        assertEquals(2, plan.valueFor(FujiPropertyCode.DRangePriority))
        assertEquals(0, plan.valueFor(FujiPropertyCode.Clarity))
    }

    @Test
    fun `inactive D Range Priority leaves dynamic range and tone dials alone`() {
        val recipe = preset(
            FujiPropertyCode.DRangePriority to 0,
            FujiPropertyCode.DynamicRange to 400,
            FujiPropertyCode.HighlightTone to 10,
        )

        val plan = RecipeWritePlanner.plan(recipe, CameraCapability.Unknown)

        assertTrue(plan.skipped.isEmpty())
        assertEquals(400, plan.valueFor(FujiPropertyCode.DynamicRange))
        assertNull(plan.skipReason(FujiPropertyCode.HighlightTone))
    }

    // ── Film simulation is the one hard block ──────────────────────────────────

    @Test
    fun `a film simulation this body lacks blocks the whole write`() {
        // An X-T4 (Std4) tops out at Eterna Bleach Bypass. Sending the other fifteen properties and
        // letting the camera refuse the simulation would leave the slot holding this recipe's tone
        // curve on top of the previous recipe's look.
        val recipe = preset(
            FujiPropertyCode.FilmSimulation to FujiFilmSimulation.RealaAce.protocolValue,
            FujiPropertyCode.Clarity to 20,
            FujiPropertyCode.Sharpness to 10,
        )
        val body = capability(
            present(FujiPropertyCode.FilmSimulation, ValueSet.Enumerated((1..18).toList())),
        )

        val plan = RecipeWritePlanner.plan(recipe, body)

        assertTrue(plan.isBlocked)
        assertEquals(20, plan.block!!.filmSimulationValue)
        assertFalse(plan.isClean)
    }

    @Test
    fun `a supported film simulation does not block`() {
        val recipe = preset(FujiPropertyCode.FilmSimulation to FujiFilmSimulation.Eterna.protocolValue)
        val body = capability(
            present(FujiPropertyCode.FilmSimulation, ValueSet.Enumerated((1..18).toList())),
        )

        assertFalse(RecipeWritePlanner.plan(recipe, body).isBlocked)
    }

    @Test
    fun `nothing is blocked when nothing is known about the body`() {
        val recipe = preset(FujiPropertyCode.FilmSimulation to FujiFilmSimulation.RealaAce.protocolValue)

        assertFalse(RecipeWritePlanner.plan(recipe, CameraCapability.Unknown).isBlocked)
    }

    @Test
    fun `a camera holding the simulation in a slot overrides a stale table`() {
        // The table says this body tops out at Nostalgic Neg, but C3 is holding Reala Ace — so the
        // body plainly takes it and the table is behind the firmware. The camera wins.
        val body = capability(
            present(FujiPropertyCode.FilmSimulation, ValueSet.Enumerated((1..19).toList())),
        )
        val recipe = preset(FujiPropertyCode.FilmSimulation to 20)

        assertTrue(RecipeWritePlanner.plan(recipe, body).isBlocked)

        val observed = body.withObservedFilmSimulations(listOf(1, 17, 20))

        assertFalse(RecipeWritePlanner.plan(recipe, observed).isBlocked)
        assertEquals(
            CapabilitySource.Camera,
            observed.of(FujiPropertyCode.FilmSimulation).valuesSource,
        )
    }

    @Test
    fun `observing slots can only widen the range, never narrow it`() {
        val body = capability(
            present(FujiPropertyCode.FilmSimulation, ValueSet.Enumerated((1..20).toList())),
        )

        // Every slot holds Provia. That says nothing about the other nineteen simulations, so the
        // ceiling must not drop to 1.
        val observed = body.withObservedFilmSimulations(listOf(1, 1, 1, 1, 1, 1, 1))

        assertFalse(
            RecipeWritePlanner.plan(preset(FujiPropertyCode.FilmSimulation to 20), observed).isBlocked,
        )
    }

    @Test
    fun `observing nothing leaves capability untouched`() {
        val body = capability(
            present(FujiPropertyCode.FilmSimulation, ValueSet.Enumerated((1..19).toList())),
        )

        assertEquals(body, body.withObservedFilmSimulations(emptyList()))
    }
}
