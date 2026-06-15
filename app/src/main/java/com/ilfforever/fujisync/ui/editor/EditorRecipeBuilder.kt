package com.ilfforever.fujisync.ui.editor

import com.ilfforever.fujisync.domain.model.FujiFilmSimulation
import com.ilfforever.fujisync.ui.library.normalizedDRangePriorityLabel
import com.ilfforever.fujisync.ui.library.normalizedDynamicRangeLabel
import com.ilfforever.fujisync.ui.model.RecipeUiModel

internal data class FilmSimFamily(
    val label: String,
    val sims: List<String>,
)

internal val filmSimFamilies = listOf(
    FilmSimFamily(
        label = "Standard",
        sims = listOf("Provia / Standard", "Velvia / Vivid", "Astia / Soft"),
    ),
    FilmSimFamily(
        label = "Portrait",
        sims = listOf("Pro Neg Hi", "Pro Neg Std", "Nostalgic Neg", "Reala Ace"),
    ),
    FilmSimFamily(
        label = "Classic",
        sims = listOf("Classic Chrome", "Classic Neg", "Sepia"),
    ),
    FilmSimFamily(
        label = "Mono",
        sims = listOf("Monochrome", "Monochrome + Y", "Monochrome + R", "Monochrome + G", "Acros", "Acros + Y", "Acros + R", "Acros + G"),
    ),
    FilmSimFamily(
        label = "Cinema",
        sims = listOf("Eterna", "Eterna Bleach Bypass"),
    ),
)

internal val whiteBalanceOptions = listOf(
    "Auto",
    "Auto White Priority",
    "Ambience Priority",
    "Daylight",
    "Incandescent",
    "Underwater",
    "Fluorescent 1",
    "Fluorescent 2",
    "Fluorescent 3",
    "Shade",
    "Color Temperature",
)

internal fun defaultRecipe() = RecipeUiModel(
    slot = "",
    name = "New Recipe",
    sim = FujiFilmSimulation.Provia.label,
    pills = listOf("DR AUTO"),
    effects = mapOf(
        "Dynamic Range" to "DR Auto",
        "D Range Priority" to "Off",
        "Grain Effect" to "Off",
        "Color Chrome" to "Off",
        "Color Chrome FX Blue" to "Off",
        "Smooth Skin" to "Off",
    ),
    tone = mapOf(
        "Highlight Tone" to "0",
        "Shadow Tone" to "0",
        "Color" to "0",
        "Sharpness" to "0",
        "High ISO NR" to "0",
        "Clarity" to "0",
    ),
    wb = mapOf("White Balance" to "Auto", "WB Shift R" to "0", "WB Shift B" to "0"),
)

internal fun buildRecipe(
    base: RecipeUiModel,
    name: String,
    description: String,
    sim: String,
    dRangePriority: String,
    dynamicRange: String,
    grain: String,
    colorChrome: String,
    colorChromeBlue: String,
    smoothSkin: String,
    whiteBalance: String,
    wbRed: Int,
    wbBlue: Int,
    highlight: Float,
    shadow: Float,
    color: Int,
    sharpness: Int,
    highIsoNr: Int,
    clarity: Int,
    monoWc: Int,
    monoMg: Int,
    referenceImageUris: List<String>,
    isMono: Boolean,
    isoMin: Int?,
    isoMax: Int?,
    exposureCompMin: Float?,
    exposureCompMax: Float?,
    sensorGens: List<Int>,
): RecipeUiModel {
    val effects = linkedMapOf(
        "D Range Priority" to dRangePriority.normalizedDRangePriorityLabel(),
        "Dynamic Range" to dynamicRange.normalizedDynamicRangeLabel(),
        "Grain Effect" to grain,
        "Color Chrome" to if (isMono) "—" else colorChrome,
        "Color Chrome FX Blue" to if (isMono) "—" else colorChromeBlue,
        "Smooth Skin" to smoothSkin,
    )
    val tone = linkedMapOf(
        "Highlight Tone" to highlight.signedDisplay(),
        "Shadow Tone" to shadow.signedDisplay(),
    )
    if (isMono) {
        tone["Mono WC"] = monoWc.signedDisplay()
        tone["Mono MG"] = monoMg.signedDisplay()
    } else {
        tone["Color"] = color.signedDisplay()
    }
    tone["Sharpness"] = sharpness.signedDisplay()
    tone["High ISO NR"] = highIsoNr.signedDisplay()
    tone["Clarity"] = clarity.signedDisplay()

    val wb = linkedMapOf("White Balance" to whiteBalance)
    if (!isMono) {
        wb["WB Shift R"] = wbRed.signedDisplay()
        wb["WB Shift B"] = wbBlue.signedDisplay()
    }

    return base.copy(
        name = name,
        sim = sim,
        description = description,
        effects = effects,
        tone = tone,
        wb = wb,
        referenceImageUris = referenceImageUris,
        pills = buildPills(dRangePriority.normalizedDRangePriorityLabel(), dynamicRange.normalizedDynamicRangeLabel(), grain, colorChrome, colorChromeBlue, highlight, shadow, color, clarity, isMono),
        isoMin = isoMin,
        isoMax = isoMax,
        exposureCompMin = exposureCompMin,
        exposureCompMax = exposureCompMax,
        sensorGens = sensorGens,
    )
}

internal fun buildPills(
    dRangePriority: String,
    dynamicRange: String,
    grain: String,
    colorChrome: String,
    colorChromeBlue: String,
    highlight: Float,
    shadow: Float,
    color: Int,
    clarity: Int,
    isMono: Boolean,
): List<String> = buildList {
    if (dRangePriority == "Off") {
        add(dynamicRange.uppercase())
    } else {
        add("DRP ${dRangePriority.uppercase()}")
    }
    if (grain != "Off") {
        add("GRAIN " + grain
            .replace("Weak Small", "WK/S")
            .replace("Weak Large", "WK/L")
            .replace("Strong Small", "ST/S")
            .replace("Strong Large", "ST/L")
            .uppercase())
    }
    if (!isMono) {
        if (colorChrome != "Off") add("CC ${if (colorChrome == "Strong") "STRONG" else "WEAK"}")
        if (colorChromeBlue != "Off") add("FX BLUE ${if (colorChromeBlue == "Strong") "STR" else "WK"}")
        if (shadow != 0f) add("SH ${shadow.signedDisplay()}")
        if (color != 0) add("COLOR ${color.signedDisplay()}")
    }
    if (highlight != 0f) add("HL ${highlight.signedDisplay()}")
    if (clarity != 0) add("CLARITY ${clarity.signedDisplay()}")
}

internal fun String?.signedIntOrZero(): Int =
    this?.replace("−", "-")?.replace("+", "")?.trim()?.toIntOrNull() ?: 0

internal fun String?.signedFloatOrZero(): Float =
    this?.replace("−", "-")?.replace("+", "")?.trim()?.toFloatOrNull() ?: 0f

internal fun String?.editorWhiteBalanceMode(): String {
    val value = this?.trim().orEmpty()
    return when {
        value.endsWith("K", ignoreCase = true) -> "Color Temperature"
        value.equals("Auto (White Priority)", ignoreCase = true) -> "Auto White Priority"
        value.equals("Auto (Ambience)", ignoreCase = true) -> "Ambience Priority"
        value.equals("Tungsten", ignoreCase = true) -> "Incandescent"
        value in whiteBalanceOptions -> value
        else -> "Auto"
    }
}

internal fun filmSimFamilyFor(sim: String): FilmSimFamily =
    filmSimFamilies.firstOrNull { family -> sim in family.sims }
        ?: filmSimFamilies.first()

internal fun Int.signedDisplay(): String = when {
    this > 0 -> "+$this"
    this < 0 -> "−${-this}"
    else -> "0"
}

internal fun Float.signedDisplay(): String {
    if (this == 0f) return "0"
    val prefix = if (this > 0f) "+" else "−"
    val abs = kotlin.math.abs(this)
    return if (abs == kotlin.math.floor(abs)) "$prefix${abs.toInt()}" else "$prefix$abs"
}
