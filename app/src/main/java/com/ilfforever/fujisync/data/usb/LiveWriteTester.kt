package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.mapper.FujiValueMapper
import com.ilfforever.fujisync.data.ptp.MONO_SIM_CODES
import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.ptpResponseName
import com.ilfforever.fujisync.data.ptp.uint16Le
import kotlinx.coroutines.delay

// Some live properties only accept a value when another property is in the right state. Setting
// colour temperature is refused unless white balance is in colour-temperature mode; the monochrome
// toning controls are refused unless a monochrome film simulation is active. The camera reports
// both cases as InvalidDevicePropValue, which looks the same as simply guessing a bad value.
//
// So a write test does three things: put the prerequisite in place, try several candidate values
// rather than one, and then put everything back exactly as it was found.

data class Prerequisite(
    val label: String,
    val code: Int,
    val candidates: List<Int>,
    val signed: Boolean = false,
)

private val COLOUR_TEMP_WB = Prerequisite(
    label = "White Balance → colour temperature (0x8007)",
    code = 0x5005,
    candidates = listOf(0x8007),
)

private val MONO_FILM_SIM = Prerequisite(
    label = "Film Simulation → monochrome",
    code = 0xD001,
    candidates = MONO_SIM_CODES.toList(),
)

// D Range Priority takes over the tone curve. While it is anything but Off the camera owns Dynamic
// Range and both tone dials, and refuses writes to them with InvalidDevicePropValue — the same
// response as an out-of-range value, which is what makes it easy to misread as "unsupported".
//
// Observed on an X-H2 with priority on Auto: Dynamic Range, Highlight Tone and Shadow Tone all
// rejected values that are legal for the body. Forcing priority Off first is what separates "locked
// right now" from "not writable at all".
private val DR_PRIORITY_OFF = Prerequisite(
    label = "D Range Priority → Off",
    code = 0xD02E,
    candidates = listOf(0),
)

/**
 * Properties the bench reads but never writes. Lens Modulation Optimiser corrects for the optics of
 * the attached lens; what it does to a stored recipe is not understood, so it is left alone rather
 * than poked to see what happens.
 */
val WRITE_EXCLUDED: Map<Int, String> = mapOf(
    0xD34D to "not written — Lens Modulation Optimiser, purpose not understood",
)

/** Live property code → the property that must be set first for it to accept a value. */
val PREREQUISITES: Map<Int, Prerequisite> = mapOf(
    0xD017 to COLOUR_TEMP_WB,   // Color Temperature
    0xD104 to MONO_FILM_SIM,    // Mono WC
    0xD031 to MONO_FILM_SIM,    // Mono MG
    0xD007 to DR_PRIORITY_OFF,  // Dynamic Range
    0xD320 to DR_PRIORITY_OFF,  // Highlight Tone
    0xD321 to DR_PRIORITY_OFF,  // Shadow Tone
)

data class LiveWriteTest(
    val prop: LiveProp,
    val original: Int,
    val attempted: Int?,
    val writeResponse: Int?,
    val readBack: Int?,
    val restoredValue: Int?,
    val candidatesTried: Int,
    val restoreNote: String? = null,
    val prerequisiteApplied: String?,
    val prerequisiteFailed: String?,
) {
    val verified: Boolean get() = attempted != null && readBack == attempted
    val restored: Boolean get() = restoredValue == original

    /** True when the camera was left as found, counting a reverted prerequisite as a restore. */
    val effectivelyRestored: Boolean get() = restored || restoreNote?.contains("inactive") == true

    val verdict: String
        get() = when {
            prerequisiteFailed != null -> "BLOCKED — could not set $prerequisiteFailed"
            verified -> "VERIFIED — changed $original -> $attempted" +
                (prerequisiteApplied?.let { " (needed $it)" } ?: "")
            candidatesTried == 0 -> "NO CANDIDATE — no legal value known to try"
            writeResponse != PtpConstants.RESPONSE_OK ->
                "REJECTED — tried $candidatesTried value(s), last was ${ptpResponseName(writeResponse ?: 0)}"
            else -> "ACCEPTED BUT NOT APPLIED — tried $candidatesTried value(s), " +
                "camera reported OK but the value never changed. Likely read-only on this body."
        }
}

private suspend fun write(connection: OpenPtpConnection, code: Int, value: Int, settleMs: Long): Int? {
    val tx = runCatching {
        connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(code),
            payload = uint16Le(value),
        )
    }.getOrNull()
    delay(settleMs)
    return tx?.response?.code
}

// Legal values per property, taken from the app's own canonical tables in FujiValueMapper rather
// than guessed. Neighbouring integers are usually invalid: Grain Effect is an enum, High ISO NR is
// a non-linear lookup, and the tone dials move in steps of 10 on the wire.
private val OFF_WEAK_STRONG = listOf(1, 2, 3)

/** Wire values move in steps of 10 for the ×10-scaled dials, so walk outward in tens. */
private fun scaledSteps(original: Int): List<Int> {
    val base = if (original == FujiValueMapper.DEFAULT_SENTINEL) 0 else original
    return listOf(10, -10, 20, -20, 30, -30).map { base + it }.filter { it in -180..180 }
}

private fun knownValuesFor(prop: LiveProp, original: Int): List<Int> = when (prop.liveCode) {
    // Enum: 1 = Off, 2..5 = weak/strong x small/large. The camera reads back 6 or 7 for a
    // factory-default slot but rejects writes of those, so they are deliberately absent here.
    0xD023 -> listOf("Off", "Weak Small", "Strong Small", "Weak Large", "Strong Large")
        .map { FujiValueMapper.grainLabelToRaw(it) }

    // Non-linear lookup; the raws are not contiguous.
    0xD01C -> (-4..4).map { FujiValueMapper.nrDialToRaw(it) }

    // Highlight and Shadow Tone run -2..+4 on the dial, so the wire range is -20..40 and nothing
    // outside it is legal. Walking outward in tens from the current value overshoots that on one
    // side and burns attempts on values the camera was always going to refuse.
    0xD320, 0xD321 -> listOf(0, 10, 20, -10, -20, 30, 40)

    0xD029, 0xD030, 0xD189 -> OFF_WEAK_STRONG          // Color Chrome, FX Blue, Smooth Skin
    0xD00A -> listOf(1, 2)                              // Color Space
    0xD007 -> listOf(100, 200, 400, 0)                  // Dynamic Range, literal percent, 0 = Auto
    0xD017 -> listOf(2500, 3000, 4000, 5000, 5600, 6500, 7500, 10000)  // Kelvin, editor range
    0xD00B, 0xD00C -> listOf(-2, -1, 0, 1, 2)           // WB shifts, raw is the dial directly

    // ×10-scaled dials: Mono WC/MG, Color, Sharpness, Clarity.
    0xD104, 0xD031, 0xD008, 0x5015, 0xD032 -> scaledSteps(original)

    else -> emptyList()
}

/**
 * Values worth trying, best first: the app's known-legal table for this property, then values the
 * camera is already holding in slots C1-C7, and only then neighbours as a last resort.
 */
private fun candidateValues(prop: LiveProp, original: Int, fromSlots: List<Int>): List<Int> {
    val out = LinkedHashSet<Int>()
    out.addAll(knownValuesFor(prop, original))
    out.addAll(fromSlots)
    listOf(1, -1, 2, -2).forEach { out.add(original + it) }
    return out.filter { it != original && it in -32768..65535 }
}

suspend fun runLiveWriteTest(
    connection: OpenPtpConnection,
    reading: LivePropReading,
    settleMs: Long = 120L,
): LiveWriteTest {
    val original = reading.value
        ?: error("${reading.prop.label} could not be read, so a write cannot be verified.")
    val code = reading.prop.liveCode
    val signed = reading.prop.signed

    // 1. Put any prerequisite in place, remembering what it was.
    val prereq = PREREQUISITES[code]
    var prereqOriginal: Int? = null
    var prereqApplied: String? = null
    if (prereq != null) {
        prereqOriginal = readValue(connection, prereq.code, prereq.signed).first
        val ok = prereq.candidates.any { write(connection, prereq.code, it, settleMs) == PtpConstants.RESPONSE_OK }
        if (!ok) {
            return LiveWriteTest(
                prop = reading.prop,
                original = original,
                attempted = null,
                writeResponse = null,
                readBack = null,
                restoredValue = readValue(connection, code, signed).first,
                candidatesTried = 0,
                prerequisiteApplied = null,
                prerequisiteFailed = prereq.label,
            )
        }
        prereqApplied = prereq.label
    }

    // 2. Try candidates until one visibly changes the camera.
    val candidates = candidateValues(reading.prop, original, legalValuesFromSlots(connection, reading.prop))
    var lastResponse: Int? = null
    var accepted: Int? = null
    var readBack: Int? = null
    var tried = 0

    for (candidate in candidates.take(MAX_CANDIDATES)) {
        tried++
        lastResponse = write(connection, code, candidate, settleMs)
        if (lastResponse != PtpConstants.RESPONSE_OK) continue
        val now = readValue(connection, code, signed).first
        if (now == candidate) {
            accepted = candidate
            readBack = now
            break
        }
    }

    // 3. Put everything back, in the only order that can work: the target first, while its
    // prerequisite is still in place, then the prerequisite. Reverting the prerequisite first
    // would make the target unwritable — colour temperature is only settable while white balance
    // is in colour-temperature mode.
    write(connection, code, original, settleMs)
    val restored = readValue(connection, code, signed).first
    if (prereq != null && prereqOriginal != null) {
        write(connection, prereq.code, prereqOriginal, settleMs)
    }

    // A property gated behind a prerequisite reads a placeholder when that prerequisite is
    // inactive: colour temperature reads 0 when white balance is not in colour-temperature mode,
    // and 0 is not a writable Kelvin. Reverting the prerequisite is the real restore in that case,
    // so it is not a failure.
    val restoreNote = when {
        restored == original -> null
        prereq != null -> "left at $restored, but the prerequisite was reverted so it is inactive"
        else -> "left at $restored, expected $original"
    }

    return LiveWriteTest(
        prop = reading.prop,
        original = original,
        attempted = accepted,
        writeResponse = if (accepted != null) PtpConstants.RESPONSE_OK else lastResponse,
        readBack = readBack,
        restoredValue = restored,
        candidatesTried = tried,
        restoreNote = restoreNote,
        prerequisiteApplied = prereqApplied,
        prerequisiteFailed = null,
    )
}

private const val MAX_CANDIDATES = 10
