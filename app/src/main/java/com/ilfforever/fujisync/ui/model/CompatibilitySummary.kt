package com.ilfforever.fujisync.ui.model

import com.ilfforever.fujisync.data.capability.CameraCapability
import com.ilfforever.fujisync.data.capability.CapabilitySource
import com.ilfforever.fujisync.data.capability.RecipeWritePlan
import com.ilfforever.fujisync.data.capability.RecipeWritePlanner
import com.ilfforever.fujisync.data.capability.WriteSkipReason
import com.ilfforever.fujisync.data.mapper.FujiValueMapper
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.FujiFilmSimulation
import com.ilfforever.fujisync.domain.model.FujiPropertyCode

/**
 * What the user should be told before a recipe is written to the camera in front of them.
 *
 * Built from the same [RecipeWritePlanner] the write path uses, so the preview cannot promise
 * something different from what actually happens.
 */
data class CompatibilitySummary(
    /**
     * Set when this body cannot take the recipe's film simulation. Nothing will be written at all —
     * this is the one refusal rather than a warning.
     */
    val blocked: CompatibilityNote?,
    /**
     * Every setting that will not reach the camera exactly as it was set, tagged with the recipe
     * section it belongs to.
     *
     * Only changed settings appear. What survives is covered by a single sentence in the sheet
     * rather than by listing a dozen untouched rows — the reassurance costs one line instead of a
     * screenful.
     */
    val changes: List<CompatibilityChange>,
    val recipeName: String,
    val cameraName: String,
) {
    val isEmpty: Boolean get() = blocked == null && changes.isEmpty()

    /** How many recipe settings will not reach the camera exactly as the user set them. */
    val affectedCount: Int get() = changes.size

    /** Nothing can be written at all. */
    val isBlocked: Boolean get() = blocked != null

    val dropped: List<CompatibilityChange> get() = changes.filter { it.kind == ChangeKind.Dropped }
    val adjusted: List<CompatibilityChange> get() = changes.filter { it.kind == ChangeKind.Changed }

    /** The sections that actually contain something, in the order the recipe reads. */
    val sections: List<Pair<String, List<CompatibilityChange>>>
        get() = SECTION_ORDER
            .map { section -> section to changes.filter { it.section == section } }
            .filter { (_, items) -> items.isNotEmpty() }

    companion object {
        val Empty = CompatibilitySummary(null, emptyList(), "", "this camera")
    }
}

/** How a setting is affected. Drives the row's treatment, not just its wording. */
enum class ChangeKind {
    /** The body has not got this setting, so it is not sent. */
    Dropped,

    /** Snapped to the nearest value this body holds. */
    Changed,

    /** Believed out of range but sent anyway, so the camera has the last word. */
    Warned,

    /** Refused by the camera's current state rather than by a missing feature. */
    Interlock,
}

data class CompatibilityChange(
    val section: String,
    val label: String,
    /** The value as the dial reads it. For [ChangeKind.Changed] this is the "+1.5 → +1" pair. */
    val value: String,
    val kind: ChangeKind,
    val reason: String,
)

data class CompatibilityNote(
    val setting: String,
    val detail: String,
)

private const val SECTION_SIMULATION = "FILM SIMULATION"
private const val SECTION_EFFECTS = "EFFECTS"
private const val SECTION_TONE = "TONE"
private const val SECTION_WHITE_BALANCE = "WHITE BALANCE"

private val SECTION_ORDER = listOf(
    SECTION_SIMULATION,
    SECTION_EFFECTS,
    SECTION_TONE,
    SECTION_WHITE_BALANCE,
)

/** Which part of the recipe a property is read under, matching how the detail page groups them. */
private val FujiPropertyCode.section: String
    get() = when (this) {
        FujiPropertyCode.FilmSimulation -> SECTION_SIMULATION

        FujiPropertyCode.DynamicRange,
        FujiPropertyCode.DRangePriority,
        FujiPropertyCode.GrainEffect,
        FujiPropertyCode.ColorChrome,
        FujiPropertyCode.ColorChromeFxBlue,
        FujiPropertyCode.SmoothSkin,
        -> SECTION_EFFECTS

        FujiPropertyCode.WhiteBalance,
        FujiPropertyCode.ColorTemperature,
        FujiPropertyCode.WbShiftRed,
        FujiPropertyCode.WbShiftBlue,
        -> SECTION_WHITE_BALANCE

        else -> SECTION_TONE
    }

fun compatibilitySummary(
    recipe: RecipeUiModel,
    capability: CameraCapability,
    cameraName: String = "this camera",
): CompatibilitySummary {
    if (capability.isEmpty) return CompatibilitySummary.Empty
    return summarize(
        plan = RecipeWritePlanner.plan(recipe.toPreset(CameraSlot.C1), capability),
        recipeName = recipe.name,
        cameraName = cameraName.ifBlank { "this camera" },
    )
}

private fun summarize(
    plan: RecipeWritePlan,
    recipeName: String,
    cameraName: String,
): CompatibilitySummary {
    val blocked = plan.block?.let { block ->
        CompatibilityNote(
            setting = displayValue(FujiPropertyCode.FilmSimulation, block.filmSimulationValue),
            detail = "not on the $cameraName",
        )
    }

    val changes = buildList {
        plan.skipped.forEach { skip ->
            val dropped = skip.reason == WriteSkipReason.NotOnThisBody
            add(
                CompatibilityChange(
                    section = skip.property.section,
                    label = skip.property.displayName,
                    value = displayValue(skip.property, skip.value),
                    kind = if (dropped) ChangeKind.Dropped else ChangeKind.Interlock,
                    reason = if (dropped) "not on the $cameraName" else skip.reason.shortReason,
                ),
            )
        }

        plan.writes.forEach { write ->
            write.adjustment?.let { adjustment ->
                add(
                    CompatibilityChange(
                        section = write.property.section,
                        label = write.property.displayName,
                        value = displayValue(write.property, adjustment.from) + "  →  " +
                            displayValue(write.property, adjustment.to),
                        kind = ChangeKind.Changed,
                        reason = adjustmentReason(write.property),
                    ),
                )
            }
            write.warning?.let { warning ->
                add(
                    CompatibilityChange(
                        section = write.property.section,
                        label = write.property.displayName,
                        value = displayValue(write.property, warning.value),
                        kind = ChangeKind.Warned,
                        reason = when (warning.source) {
                            CapabilitySource.Camera -> "the camera reports this as out of range"
                            else -> "not confirmed for this body; sent for the camera to decide"
                        },
                    ),
                )
            }
        }
    }

    return CompatibilitySummary(blocked, changes, recipeName, cameraName)
}

private fun adjustmentReason(property: FujiPropertyCode): String = when (property) {
    FujiPropertyCode.ColorTemperature -> "nearest temperature this body holds"
    FujiPropertyCode.HighlightTone, FujiPropertyCode.ShadowTone -> "this body moves in whole steps"
    else -> "nearest value this body holds"
}

/** Short enough to sit under a row. */
private val WriteSkipReason.shortReason: String
    get() = when (this) {
        WriteSkipReason.MonochromeSimulation -> "not used by a monochrome simulation"
        WriteSkipReason.ColorSimulation -> "only used by a monochrome simulation"
        WriteSkipReason.WhiteBalanceNotKelvin -> "White Balance is not in Kelvin mode"
        WriteSkipReason.DynamicRangePriorityActive -> "D Range Priority controls this"
        WriteSkipReason.NotOnThisBody -> "not on this camera"
    }

/** Wire value rendered the way the dial reads on the camera, not as a raw PTP number. */
fun displayValue(property: FujiPropertyCode, raw: Int): String = when (property) {
    FujiPropertyCode.FilmSimulation ->
        FujiFilmSimulation.entries.firstOrNull { it.protocolValue == raw }?.label ?: "value $raw"

    FujiPropertyCode.WhiteBalance -> whiteBalanceLabel(raw)
    FujiPropertyCode.ColorTemperature -> "${raw}K"
    FujiPropertyCode.DynamicRange -> if (raw == 0) "DR Auto" else "DR$raw%"
    FujiPropertyCode.DRangePriority -> when (raw) {
        1 -> "Weak"
        2 -> "Strong"
        32768 -> "Auto"
        else -> "Off"
    }

    FujiPropertyCode.GrainEffect -> FujiValueMapper.grainRawToLabel(raw)
    FujiPropertyCode.ColorChrome,
    FujiPropertyCode.ColorChromeFxBlue,
    FujiPropertyCode.SmoothSkin,
    -> FujiValueMapper.owsRawToLabel(raw)

    FujiPropertyCode.HighIsoNr -> signed(FujiValueMapper.nrRawToDial(raw) ?: 0)

    FujiPropertyCode.WbShiftRed, FujiPropertyCode.WbShiftBlue -> signed(raw)

    else -> if (property in FujiValueMapper.SCALED_X10) scaled(raw) else raw.toString()
}

private fun whiteBalanceLabel(raw: Int): String =
    WhiteBalanceCodes.label(raw) ?: "value $raw"

private fun signed(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}

private fun scaled(raw: Int): String {
    val dial = FujiValueMapper.scaledRawToDial(raw) ?: return "0"
    if (dial == 0f) return "0"
    val prefix = if (dial > 0) "+" else "−"
    val abs = kotlin.math.abs(dial)
    return if (abs == kotlin.math.floor(abs)) "$prefix${abs.toInt()}" else "$prefix$abs"
}

/**
 * The film simulation this body will refuse, or null when the recipe can be written.
 *
 * Used to stop the sync flow at its entry point rather than letting someone pick a slot, tap write
 * and be refused — the recipe cannot go to this camera at all, so the button says so.
 */
fun blockingFilmSimulation(recipe: RecipeUiModel, capability: CameraCapability): String? {
    if (capability.isEmpty) return null
    val block = RecipeWritePlanner.plan(recipe.toPreset(CameraSlot.C1), capability).block ?: return null
    return displayValue(FujiPropertyCode.FilmSimulation, block.filmSimulationValue)
}
