package com.ilfforever.fujisync.data.capability

import com.ilfforever.fujisync.data.ptp.MONO_SIM_CODES
import com.ilfforever.fujisync.domain.model.FujiPropertyCode
import com.ilfforever.fujisync.domain.model.RecipePreset

/**
 * Decides what will actually be sent to a camera for a given recipe, and what will not.
 *
 * Kept pure and separate from the USB layer for two reasons: it is the logic most worth unit
 * testing without hardware, and the sync sheet needs to show the user the same answer *before* the
 * write happens. The write path and the preview therefore cannot disagree.
 *
 * Three kinds of exclusion, in order of certainty:
 *  - **Interlocks** — settings the camera itself refuses in a particular state. Confirmed on
 *    hardware, not guesses: colour-only settings under a monochrome simulation, Colour Temperature
 *    while White Balance is in another mode, and the three properties Dynamic Range Priority takes
 *    over while it is active.
 *  - **Blocked** — the camera did not list the property in its own DeviceInfo. Never attempted.
 *  - **Warned** — only Fuji's capability table objects. Still attempted, because that table
 *    describes X RAW STUDIO's feature set rather than camera firmware; the camera gets the last
 *    word and reports its own rejection.
 */
object RecipeWritePlanner {

    /** Settings a monochrome film simulation makes meaningless; the camera rejects them outright. */
    private val COLOR_ONLY = setOf(
        FujiPropertyCode.ColorChrome,
        FujiPropertyCode.ColorChromeFxBlue,
        FujiPropertyCode.Color,
        FujiPropertyCode.WbShiftRed,
        FujiPropertyCode.WbShiftBlue,
    )

    /** The mirror of [COLOR_ONLY]: monochrome toning under a colour simulation is rejected (0x201C). */
    private val MONO_ONLY = setOf(
        FujiPropertyCode.MonoWc,
        FujiPropertyCode.MonoMg,
    )

    /**
     * While Dynamic Range Priority is Weak/Strong/Auto the camera owns all three of these and
     * answers `InvalidDevicePropValue` to any write — including values that are otherwise legal for
     * the body. Confirmed on an X-H2 by a controlled pair of runs.
     */
    private val OWNED_BY_DR_PRIORITY = setOf(
        FujiPropertyCode.DynamicRange,
        FujiPropertyCode.HighlightTone,
        FujiPropertyCode.ShadowTone,
    )

    /**
     * Properties where snapping to the nearest legal value preserves the photographer's intent: a
     * body without half-steps gets the nearest whole step, a body with a fixed Kelvin list gets the
     * nearest listed temperature. Never applied to enumerations like Film Simulation, where the
     * "nearest" value would be a different look entirely.
     */
    private val CLAMPABLE = setOf(
        FujiPropertyCode.HighlightTone,
        FujiPropertyCode.ShadowTone,
        FujiPropertyCode.ColorTemperature,
    )

    private const val WB_COLOR_TEMPERATURE = 0x8007

    fun plan(preset: RecipePreset, capability: CameraCapability): RecipeWritePlan {
        val properties = preset.properties
        val filmSim = properties[FujiPropertyCode.FilmSimulation]
        val isMono = filmSim != null && filmSim in MONO_SIM_CODES

        // Film Simulation is the one property that stops the write rather than warning about it.
        // Everything else can be dropped and still leave a recognisable recipe; the simulation is
        // the recipe. Sending the other fifteen properties and letting the camera refuse the
        // simulation would leave the slot holding this recipe's tone curve on top of the previous
        // recipe's look — a corrupted preset that reports as a success.
        val block = filmSim?.let { blockingFilmSimulation(it, capability) }
        val isColorTemp = properties[FujiPropertyCode.WhiteBalance] == WB_COLOR_TEMPERATURE
        val drPriorityActive = (properties[FujiPropertyCode.DRangePriority] ?: 0) != 0

        // Film Simulation is written first: it moves the camera into the state that decides which
        // ranges the other properties will accept.
        val ordered = properties.entries.sortedBy { (property, _) ->
            if (property == FujiPropertyCode.FilmSimulation) 0 else 1
        }

        val writes = mutableListOf<PlannedWrite>()
        val skipped = mutableListOf<SkippedWrite>()

        for ((property, requested) in ordered) {
            val reason = interlockReason(property, isMono, isColorTemp, drPriorityActive)
                ?: if (capability.isBlocked(property)) WriteSkipReason.NotOnThisBody else null

            if (reason != null) {
                skipped += SkippedWrite(property, requested, reason)
                continue
            }

            writes += planOne(property, requested, capability.of(property))
        }

        return RecipeWritePlan(writes, skipped, block)
    }

    /**
     * Whether this body should refuse the recipe's film simulation outright.
     *
     * Only decided against a value set that actually exists: when nothing is known about the body,
     * nothing is blocked. The set can come from the camera's own descriptor, from values observed
     * sitting in its slots, or — failing both — from Fuji's table, which is the weakest source but
     * also the best-evidenced part of it: the simulation numbering is a strictly nested chain whose
     * every addition matches the camera that simulation launched on.
     */
    private fun blockingFilmSimulation(value: Int, capability: CameraCapability): WriteBlock? {
        val support = capability.of(FujiPropertyCode.FilmSimulation)
        return when {
            support.isBlocked -> WriteBlock(value, support.supportSource)
            support.values != ValueSet.Unknown && !support.values.allows(value) ->
                WriteBlock(value, support.valuesSource)

            else -> null
        }
    }

    private fun interlockReason(
        property: FujiPropertyCode,
        isMono: Boolean,
        isColorTemp: Boolean,
        drPriorityActive: Boolean,
    ): WriteSkipReason? = when {
        isMono && property in COLOR_ONLY -> WriteSkipReason.MonochromeSimulation
        !isMono && property in MONO_ONLY -> WriteSkipReason.ColorSimulation
        !isColorTemp && property == FujiPropertyCode.ColorTemperature -> WriteSkipReason.WhiteBalanceNotKelvin
        drPriorityActive && property in OWNED_BY_DR_PRIORITY -> WriteSkipReason.DynamicRangePriorityActive
        else -> null
    }

    private fun planOne(
        property: FujiPropertyCode,
        requested: Int,
        capability: PropertyCapability,
    ): PlannedWrite {
        if (!capability.rejects(requested)) {
            return PlannedWrite(property, requested, requested, adjustment = null, warning = null)
        }

        val clamped = (capability.values as? ValueSet.Range)
            ?.takeIf { property in CLAMPABLE }
            ?.clamp(requested)
            ?: nearestEnumerated(property, requested, capability.values)

        return when {
            clamped != null && clamped != requested -> PlannedWrite(
                property = property,
                requestedValue = requested,
                value = clamped,
                adjustment = Adjustment(requested, clamped, capability.valuesSource),
                warning = null,
            )

            // Nothing sensible to substitute. Send it as asked and let the camera answer: Fuji's
            // table is evidence about the body, not a statement of what its firmware accepts.
            else -> PlannedWrite(
                property = property,
                requestedValue = requested,
                value = requested,
                adjustment = null,
                warning = CapabilityWarning(property, requested, capability.valuesSource),
            )
        }
    }

    private fun nearestEnumerated(
        property: FujiPropertyCode,
        requested: Int,
        values: ValueSet,
    ): Int? {
        if (property !in CLAMPABLE) return null
        val enumerated = (values as? ValueSet.Enumerated)?.values?.takeIf { it.isNotEmpty() } ?: return null
        return enumerated.minByOrNull { kotlin.math.abs(it - requested) }
    }
}

data class RecipeWritePlan(
    val writes: List<PlannedWrite>,
    val skipped: List<SkippedWrite>,
    /**
     * Set when the recipe must not be written to this body at all. A blocked plan's [writes] are
     * never executed — a partial recipe is worse than no recipe, because it looks like a success.
     */
    val block: WriteBlock? = null,
) {
    val isBlocked: Boolean get() = block != null

    val adjustments: List<Adjustment> get() = writes.mapNotNull { it.adjustment }
    val warnings: List<CapabilityWarning> get() = writes.mapNotNull { it.warning }

    /** Settings the camera said it does not have. The strongest signal for the sync sheet. */
    val unsupported: List<SkippedWrite>
        get() = skipped.filter { it.reason == WriteSkipReason.NotOnThisBody }

    /** True when everything in the recipe reaches the camera exactly as the user set it. */
    val isClean: Boolean
        get() = !isBlocked && unsupported.isEmpty() && adjustments.isEmpty() && warnings.isEmpty()
}

/** The film simulation this body will not take, and how confident the app is about that. */
data class WriteBlock(
    val filmSimulationValue: Int,
    val source: CapabilitySource,
)

data class PlannedWrite(
    val property: FujiPropertyCode,
    val requestedValue: Int,
    /** What will be sent. Differs from [requestedValue] only when [adjustment] is set. */
    val value: Int,
    val adjustment: Adjustment?,
    val warning: CapabilityWarning?,
)

/** A value snapped to the nearest one this body accepts, rather than being sent and rejected. */
data class Adjustment(
    val from: Int,
    val to: Int,
    val source: CapabilitySource,
)

/** A value believed to be out of range, sent anyway so the camera can have the final say. */
data class CapabilityWarning(
    val property: FujiPropertyCode,
    val value: Int,
    val source: CapabilitySource,
)

data class SkippedWrite(
    val property: FujiPropertyCode,
    val value: Int,
    val reason: WriteSkipReason,
)

enum class WriteSkipReason {
    /** Colour-only setting under a monochrome film simulation. */
    MonochromeSimulation,

    /** Monochrome toning under a colour film simulation. */
    ColorSimulation,

    /** Colour Temperature is only writable while White Balance is in Kelvin mode. */
    WhiteBalanceNotKelvin,

    /** Dynamic Range Priority is active and owns this property. */
    DynamicRangePriorityActive,

    /** The camera did not list this property in its own DeviceInfo. */
    NotOnThisBody,
}
