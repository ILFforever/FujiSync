package com.paeki.fujirecipes.data.mapper

import com.paeki.fujirecipes.domain.model.FujiPropertyCode

/**
 * Canonical translation between raw PTP wire values and human dial positions.
 *
 * This mirrors the reverse-engineered reference app's `FujiValueMapper.displayValue`
 * (decompiled at `reverse-engineering/.../data/mapper/FujiValueMapper.java`) and adds the
 * inverse (dial → raw) direction the reference keeps in its UI state.
 *
 * **Canonical internal representation:** `RecipePreset.properties` always holds *raw PTP wire
 * values* (the exact bytes the camera reads/writes). Display and editing happen through this
 * mapper. Encoding rules (verified against the reference + PROTOCOL.md §5):
 *
 * - HighlightTone, ShadowTone, Color, Sharpness, Clarity, MonoWc, MonoMg → wire = dial × 10.
 *   (e.g. Color dial +2 → wire 20; Highlight −1.5 → wire −15.) These are int16LE.
 * - HighIsoNr → non-linear lookup ([nrDecode]); 8192 = dial 0 (Normal), 0 = dial +2.
 * - GrainEffect → 1 or 6 = Off, 2 = Weak Small, 3 = Strong Small, 4 = Weak Large, 5 = Strong Large.
 * - ColorChrome / ColorChromeFxBlue / SmoothSkin → 1 = Off, 2 = Weak, 3 = Strong.
 * - DynamicRange → raw is the literal percentage (100/200/400); 0 = Auto.
 * - WbShiftRed / WbShiftBlue → raw is the dial value directly (no scale).
 *
 * The Short.MIN_VALUE sentinel (-32768) means "default / unset" for signed properties.
 */
object FujiValueMapper {

    /** Signed properties whose PTP wire value is the dial position × 10. */
    val SCALED_X10: Set<FujiPropertyCode> = setOf(
        FujiPropertyCode.HighlightTone,
        FujiPropertyCode.ShadowTone,
        FujiPropertyCode.Color,
        FujiPropertyCode.Sharpness,
        FujiPropertyCode.Clarity,
        FujiPropertyCode.MonoWc,
        FujiPropertyCode.MonoMg,
    )

    /** Camera "default / unset" sentinel for signed int16 properties. */
    const val DEFAULT_SENTINEL: Int = Short.MIN_VALUE.toInt() // -32768

    // ── High ISO NR (non-linear) ──────────────────────────────────────────────
    private val nrDecode: Map<Int, Int> = mapOf(
        32768 to -4,
        28672 to -3,
        16384 to -2,
        12288 to -1,
        8192 to 0,
        4096 to 1,
        0 to 2,
        24576 to 3,
        20480 to 4,
    )
    private val nrEncode: Map<Int, Int> = nrDecode.entries.associate { (raw, dial) -> dial to raw }

    /** PTP raw → NR dial, or null if the raw value isn't a known NR code. */
    fun nrRawToDial(raw: Int): Int? = nrDecode[raw]

    /** NR dial → PTP raw. Falls back to 8192 (Normal) for out-of-range dials. */
    fun nrDialToRaw(dial: Int): Int = nrEncode[dial] ?: 8192

    // ── Grain Effect (combined strength + size) ─────────────────────────────────
    fun grainRawToLabel(raw: Int?): String = when (raw) {
        2 -> "Weak Small"
        3 -> "Strong Small"
        4 -> "Weak Large"
        5 -> "Strong Large"
        else -> "Off" // 1, 6, null, or unknown
    }

    /** Off encodes to 6 (the camera default per PROTOCOL §5). */
    fun grainLabelToRaw(label: String?): Int = when (label) {
        "Weak Small" -> 2
        "Strong Small" -> 3
        "Weak Large" -> 4
        "Strong Large" -> 5
        else -> 6
    }

    // ── Off / Weak / Strong (Color Chrome, FX Blue, Smooth Skin) ────────────────
    fun owsRawToLabel(raw: Int?): String = when (raw) {
        2 -> "Weak"
        3 -> "Strong"
        else -> "Off" // 1, null, or unknown
    }

    fun owsLabelToRaw(label: String?): Int = when (label) {
        "Weak" -> 2
        "Strong" -> 3
        else -> 1
    }

    // ── ×10-scaled dials ────────────────────────────────────────────────────────
    /** PTP raw → dial position (e.g. wire 20 → 2.0). Returns null for the default sentinel. */
    fun scaledRawToDial(raw: Int?): Float? = when (raw) {
        null, DEFAULT_SENTINEL -> null
        else -> raw / 10f
    }

    /** Dial position → PTP raw (e.g. 2.0 → 20). */
    fun scaledDialToRaw(dial: Float): Int = Math.round(dial * 10f)
}
