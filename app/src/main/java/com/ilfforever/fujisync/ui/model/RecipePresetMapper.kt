package com.ilfforever.fujisync.ui.model

import com.ilfforever.fujisync.data.mapper.FujiValueMapper
import com.ilfforever.fujisync.data.ptp.MONO_SIM_CODES
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.FujiFilmSimulation
import com.ilfforever.fujisync.domain.model.FujiPropertyCode
import com.ilfforever.fujisync.ui.library.normalizedDRangePriorityLabel
import com.ilfforever.fujisync.ui.library.normalizedDynamicRangeLabel
import com.ilfforever.fujisync.domain.model.RecipePreset

fun RecipePreset.toUiModel(): RecipeUiModel {
    val props = properties

    // ── Film Simulation ───────────────────────────────────────────
    val simCode = props[FujiPropertyCode.FilmSimulation] ?: 1
    val sim = FujiFilmSimulation.entries.firstOrNull { it.protocolValue == simCode }?.label
        ?: "Provia / Standard"
    val mono = simCode in MONO_SIM_CODES

    // ── Dynamic Range ─────────────────────────────────────────────
    val dr = when (props[FujiPropertyCode.DynamicRange]) {
        100  -> "DR100%"
        200  -> "DR200%"
        400  -> "DR400%"
        else -> "DR Auto"
    }
    val dRangePriority = when (props[FujiPropertyCode.DRangePriority]) {
        1     -> "Weak"
        2     -> "Strong"
        32768 -> "Auto"
        else  -> "Off"
    }

    // ── Grain Effect ──────────────────────────────────────────────
    val grain = FujiValueMapper.grainRawToLabel(props[FujiPropertyCode.GrainEffect])

    // ── Off / Weak / Strong ───────────────────────────────────────
    val cc     = FujiValueMapper.owsRawToLabel(props[FujiPropertyCode.ColorChrome])
    val ccBlue = FujiValueMapper.owsRawToLabel(props[FujiPropertyCode.ColorChromeFxBlue])
    val skin   = FujiValueMapper.owsRawToLabel(props[FujiPropertyCode.SmoothSkin])

    // ── White Balance ─────────────────────────────────────────────
    val wbCode     = props[FujiPropertyCode.WhiteBalance] ?: 2
    val colorTempK = props[FujiPropertyCode.ColorTemperature] ?: 5600
    val wbDisplay  = when (wbCode) {
        2      -> "Auto"
        0x8020 -> "Auto White Priority"
        0x8021 -> "Ambience Priority"
        4      -> "Daylight"
        6      -> "Incandescent"
        8      -> "Underwater"
        0x8001 -> "Fluorescent 1"
        0x8002 -> "Fluorescent 2"
        0x8003 -> "Fluorescent 3"
        0x8006 -> "Shade"
        0x8007 -> "${colorTempK}K"
        else   -> "Auto"
    }

    // ── Signed ±n display (direct dial, no scale) — WB shifts ──────
    fun signed(v: Int?) = when {
        v == null || v == 0 -> "0"
        v > 0               -> "+$v"
        else                -> "−${-v}" // − not hyphen
    }

    // ── ×10-scaled dials — HL/SH/Color/Sharpness/Clarity ──────────
    // PTP wire unit is dial × 10 (int16); display as ±0.5-step float.
    fun signedScaled(raw: Int?): String {
        val dial = FujiValueMapper.scaledRawToDial(raw) ?: return "0"
        if (dial == 0f) return "0"
        val prefix = if (dial > 0) "+" else "−"
        val abs = kotlin.math.abs(dial)
        return if (abs == kotlin.math.floor(abs)) "$prefix${abs.toInt()}" else "$prefix$abs"
    }

    val hl      = signedScaled(props[FujiPropertyCode.HighlightTone])
    val sh      = signedScaled(props[FujiPropertyCode.ShadowTone])
    val color   = signedScaled(props[FujiPropertyCode.Color])
    val sharp   = signedScaled(props[FujiPropertyCode.Sharpness])
    val clarity = signedScaled(props[FujiPropertyCode.Clarity])
    val wbR     = signed(props[FujiPropertyCode.WbShiftRed])
    val wbB     = signed(props[FujiPropertyCode.WbShiftBlue])

    // High ISO NR — non-linear PTP lookup (8192 = Normal). Unknown raw falls back to 0.
    val nrDial = FujiValueMapper.nrRawToDial(props[FujiPropertyCode.HighIsoNr] ?: 8192) ?: 0
    val nr = signed(nrDial)

    // ── Structured maps ───────────────────────────────────────────
    val effects = buildMap {
        put("D Range Priority",     dRangePriority)
        put("Dynamic Range",        dr)
        put("Grain Effect",         grain)
        put("Color Chrome",         if (mono) "—" else cc)
        put("Color Chrome FX Blue", if (mono) "—" else ccBlue)
        put("Smooth Skin",          skin)
    }

    val tone = buildMap {
        put("Highlight Tone", hl)
        put("Shadow Tone",    sh)
        if (!mono) put("Color", color)
        put("Sharpness",   sharp)
        put("High ISO NR", nr)
        put("Clarity",     clarity)
    }

    val wb = buildMap {
        put("White Balance", wbDisplay)
        if (!mono) {
            put("WB Shift R", wbR)
            put("WB Shift B", wbB)
        }
    }

    // ── Pills ─────────────────────────────────────────────────────
    val pills = buildList {
        if (dRangePriority == "Off") {
            add(dr)
        } else {
            add("DRP ${dRangePriority.uppercase()}")
        }
        if (grain != "Off") {
            val tag = grain
                .replace("Weak Small",   "WK/S")
                .replace("Weak Large",   "WK/L")
                .replace("Strong Small", "ST/S")
                .replace("Strong Large", "ST/L")
            add("GRAIN $tag")
        }
        if (!mono) {
            if (cc     != "Off") add("CC ${if (cc     == "Strong") "STRONG" else "WEAK"}")
            if (ccBlue != "Off") add("FX BLUE ${if (ccBlue == "Strong") "STR" else "WK"}")
        }
        if (hl != "0") add("HL $hl")
        if (!mono && sh != "0") add("SH $sh")
        if (!mono && color != "0") add("COLOR $color")
        if (clarity != "0") add("CLARITY $clarity")
    }

    return RecipeUiModel(
        slot  = slot.label,
        name  = name.ifBlank { slot.label },
        sim   = sim,
        pills = pills,
        effects = effects,
        tone    = tone,
        wb      = wb,
    )
}

// ── Reverse mapper: RecipeUiModel → RecipePreset (for writing to camera) ────

fun RecipeUiModel.toPreset(targetSlot: CameraSlot): RecipePreset {
    val props = mutableMapOf<FujiPropertyCode, Int>()

    // Film Simulation — must be first in the write order
    val simValue = FujiFilmSimulation.entries.firstOrNull { it.label == sim }?.protocolValue ?: 1
    props[FujiPropertyCode.FilmSimulation] = simValue

    val dRangePriority = (effects["D Range Priority"] ?: "Off").normalizedDRangePriorityLabel()
    props[FujiPropertyCode.DRangePriority] = when (dRangePriority) {
        "Weak"   -> 1
        "Strong" -> 2
        "Auto"   -> 32768
        else     -> 0
    }

    // Dynamic Range is camera-controlled when D Range Priority is active.
    if (dRangePriority == "Off") {
        props[FujiPropertyCode.DynamicRange] = when ((effects["Dynamic Range"] ?: "DR Auto").normalizedDynamicRangeLabel()) {
            "DR100%" -> 100
            "DR200%" -> 200
            "DR400%" -> 400
            else     -> 0
        }
    }

    // Grain Effect (1/6=Off, 2=WkS, 3=StS, 4=WkL, 5=StL)
    props[FujiPropertyCode.GrainEffect] = FujiValueMapper.grainLabelToRaw(effects["Grain Effect"])

    // Off / Weak / Strong (1=Off, 2=Weak, 3=Strong)
    props[FujiPropertyCode.ColorChrome]       = FujiValueMapper.owsLabelToRaw(effects["Color Chrome"])
    props[FujiPropertyCode.ColorChromeFxBlue] = FujiValueMapper.owsLabelToRaw(effects["Color Chrome FX Blue"])
    props[FujiPropertyCode.SmoothSkin]        = FujiValueMapper.owsLabelToRaw(effects["Smooth Skin"])

    // White Balance
    val wbDisplay = wb["White Balance"] ?: "Auto"
    val wbCode = when {
        wbDisplay == "Auto"                -> 2
        wbDisplay == "Auto White Priority" -> 0x8020
        wbDisplay == "Ambience Priority"   -> 0x8021
        wbDisplay == "Daylight"            -> 4
        wbDisplay == "Incandescent"        -> 6
        wbDisplay == "Underwater"          -> 8
        wbDisplay == "Fluorescent 1"       -> 0x8001
        wbDisplay == "Fluorescent 2"       -> 0x8002
        wbDisplay == "Fluorescent 3"       -> 0x8003
        wbDisplay == "Shade"               -> 0x8006
        wbDisplay.endsWith("K")            -> 0x8007
        else                               -> 2
    }
    props[FujiPropertyCode.WhiteBalance] = wbCode
    if (wbCode == 0x8007) {
        props[FujiPropertyCode.ColorTemperature] = wbDisplay.dropLast(1).toIntOrNull() ?: 5600
    }

    // Signed ±n display → Int (direct dial). The forward mapper uses Unicode minus − not hyphen.
    fun parseSigned(v: String?): Int? =
        v?.replace('−', '-')?.replace("+", "")?.trim()?.toIntOrNull()

    // ×10-scaled dials (HL/SH/Color/Sharpness/Clarity): wire unit is dial × 10.
    fun parseScaled(v: String?): Int? =
        v?.replace('−', '-')?.replace("+", "")?.trim()?.toFloatOrNull()
            ?.let { FujiValueMapper.scaledDialToRaw(it) }

    parseSigned(wb["WB Shift R"])?.let       { props[FujiPropertyCode.WbShiftRed]    = it }
    parseSigned(wb["WB Shift B"])?.let       { props[FujiPropertyCode.WbShiftBlue]   = it }
    parseScaled(tone["Highlight Tone"])?.let { props[FujiPropertyCode.HighlightTone] = it }
    parseScaled(tone["Shadow Tone"])?.let    { props[FujiPropertyCode.ShadowTone]    = it }
    parseScaled(tone["Color"])?.let          { props[FujiPropertyCode.Color]         = it }
    parseScaled(tone["Sharpness"])?.let      { props[FujiPropertyCode.Sharpness]     = it }
    parseScaled(tone["Clarity"])?.let        { props[FujiPropertyCode.Clarity]       = it }

    // High ISO NR: dial → non-linear PTP raw (dial 0 → 8192).
    parseSigned(tone["High ISO NR"])?.let {
        props[FujiPropertyCode.HighIsoNr] = FujiValueMapper.nrDialToRaw(it)
    }

    return RecipePreset(slot = targetSlot, name = name, properties = props)
}
