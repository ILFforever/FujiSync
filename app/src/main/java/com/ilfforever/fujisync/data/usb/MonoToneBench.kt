package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.ptpResponseName
import com.ilfforever.fujisync.data.ptp.uint16Le
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.FujiFilmSimulation
import kotlinx.coroutines.delay

// Monochrome toning sweep — 0xD193 and 0xD194.
//
// **Nothing about these two codes is hardware-confirmed**, including the parts the protocol notes
// state plainly. `RecipePresetMapper` maps neither direction, so the app has never sent or read
// either value — there has never been an opportunity to confirm anything about them. The rows in
// `properties.md` are inference that has hardened into documentation by sitting there.
//
// What is inferred rather than measured:
//
// 1. **The labels.** "Mono WC" and "Mono MG" look like the two axes of one control, matched to two
//    codes. But 0xD193's XRFC field name is `lBlackImageTone`, and Fuji's capability table marks it
//    supported on exactly four configurations — X-T3, X-T30 and GFX100 firmware 1-2 — while
//    `MonochromaticColor` (0xD194) is true on twenty-four including every X-Trans V body. Two axes
//    of one control would not split 4/24. That fits 0xD193 being the single-axis black-and-white
//    toning those 2018-2019 bodies had, superseded later by the two-axis control.
//
// 2. **The scale.** Both are grouped with the dial x 10 properties. The properties in that group
//    that were actually measured are Colour, Highlight, Shadow, Sharpness and Clarity.
//
// The bench therefore assumes neither. It reports by property code, not by axis name, and probes
// both candidate scales.
//
// **Read the result on the camera, not just here.** Read-back only proves the camera stored a
// number. Which axis actually moved is visible in the camera's own Monochromatic Color screen for
// the slot being written, and that is the only thing that settles what these codes drive.

// Named by code, not by axis: which axis each one drives is exactly what is unknown.
private const val PROP_D193 = 0xD193
private const val PROP_D194 = 0xD194

private const val RESPONSE_INVALID_VALUE = 0x201C
private const val RESPONSE_PROP_NOT_SUPPORTED = 0x200A

/**
 * Values to try, as wire values, covering both candidate scales.
 *
 * `properties.md` groups these with the dial x 10 properties, which would put the camera's ±9 dial
 * at ±90 on the wire. That grouping cannot have been measured: the app has never sent either value,
 * so nothing has ever round-tripped them. The properties in that list that *were* confirmed are
 * Colour, Highlight, Shadow, Sharpness and Clarity.
 *
 * So the spread walks both hypotheses. If the wire value is the raw dial, everything past ±9 is
 * refused or clamped. If it is dial x 10, the multiples of ten are accepted and 100 is one notch too
 * far. The pattern that comes back decides it.
 */
val MONO_TONE_PROBES: List<Int> = listOf(0, 1, 2, 5, 9, 10, 20, 50, 90, 100, -9, -90)

data class MonoToneProbe(
    val propertyCode: Int,
    val propertyLabel: String,
    val written: Int,
    val writeResponse: Int?,
    val readBack: Int?,
) {
    val hex: String get() = "0x%04X".format(propertyCode)
    val accepted: Boolean get() = readBack == written

    val verdict: String
        get() = when {
            accepted -> "stored as written"
            writeResponse == PtpConstants.RESPONSE_OK && readBack != null ->
                "camera said OK but stored $readBack — clamped or rescaled"

            writeResponse == PtpConstants.RESPONSE_OK -> "camera said OK but nothing read back"
            writeResponse == RESPONSE_INVALID_VALUE -> "rejected — value not legal here"
            writeResponse == RESPONSE_PROP_NOT_SUPPORTED -> "property absent on this body"
            writeResponse == null -> "no response — transport error"
            else -> "failed — ${ptpResponseName(writeResponse)}"
        }
}

data class MonoToneSweep(
    val slot: CameraSlot,
    /** The film simulation the slot held before the run, restored afterwards. */
    val originalFilmSim: Int?,
    val originalD193: Int?,
    val originalD194: Int?,
    val monoSimApplied: Boolean,
    val probes: List<MonoToneProbe>,
    val restored: Boolean,
    val durationMs: Long,
) {
    fun probesFor(code: Int): List<MonoToneProbe> = probes.filter { it.propertyCode == code }

    private fun acceptedFor(code: Int) = probesFor(code).count { it.accepted }

    val d193Accepted: Int get() = acceptedFor(PROP_D193)
    val d194Accepted: Int get() = acceptedFor(PROP_D194)

    val summary: String
        get() = when {
            !monoSimApplied ->
                "Could not put the slot into a monochrome film simulation, so nothing was written. " +
                    "Both properties are rejected under a colour simulation, and a run without that " +
                    "step would only prove the interlock works."

            probes.isEmpty() -> "Nothing was written."

            d193Accepted == 0 && d194Accepted == 0 ->
                "Neither property accepted any value. Both may be absent on this body, or the slot " +
                    "was not in the state the camera expects."

            d193Accepted == 0 ->
                "0xD194 accepted $d194Accepted value(s); 0xD193 accepted none. That fits " +
                    "0xD193 being the older single-axis toning this body does not have. Check the " +
                    "camera's Monochromatic Color screen to see what 0xD194 moved."

            d194Accepted == 0 ->
                "0xD193 accepted $d193Accepted value(s); 0xD194 accepted none."

            else ->
                "Both properties accepted values — 0xD193 $d193Accepted, 0xD194 " +
                    "$d194Accepted. Check the camera's Monochromatic Color screen for slot " +
                    "${slot.label} to see which axis each one moved."
        }
}

/**
 * What the camera is currently holding for a slot. Read-only.
 *
 * This is the better of the two experiments available here, and the one to try first. Writing a
 * value and looking for movement infers the mapping backwards; setting both axes on the camera to
 * known, distinct values and reading the codes gives it directly — and answers the scale at the
 * same time, because the number on the camera and the number on the wire can be compared.
 */
data class MonoToneReading(
    val slot: CameraSlot,
    val filmSim: Int?,
    val d193: Int?,
    val d194: Int?,
) {
    private val values = listOfNotNull(d193, d194).filter { it != 0 }

    /** Whether the wire values look like the raw camera dial or the documented dial x 10. */
    val scaleHint: String
        get() = when {
            values.isEmpty() ->
                "Both read 0. Set Monochromatic Color on the camera for this slot first — use two " +
                    "different values, ideally with opposite signs, so neither code is ambiguous."

            values.any { kotlin.math.abs(it) > 9 } ->
                "At least one value is beyond ±9, so these are the documented dial × 10. Divide by " +
                    "ten to compare with the camera."

            else ->
                "Both values are within ±9, which matches the camera dial directly — so these are " +
                    "NOT dial × 10, and FujiValueMapper.SCALED_X10 is wrong for these two."
        }

    /** Reads as the camera dial would if the x 10 scaling holds. */
    fun asDial(raw: Int?): String = when {
        raw == null -> "—"
        kotlin.math.abs(raw) > 9 -> "$raw  (dial ${raw / 10})"
        else -> "$raw"
    }
}

/** Reads both toning properties for a slot without writing anything. */
suspend fun readMonoToneState(
    connection: OpenPtpConnection,
    slot: CameraSlot,
    settleMs: Long = 150L,
): MonoToneReading {
    if (!selectSlot(connection, slot)) return MonoToneReading(slot, null, null, null)
    delay(settleMs)

    return MonoToneReading(
        slot = slot,
        filmSim = readValue(connection, 0xD192, signed = false).first,
        d193 = readValue(connection, PROP_D193, signed = true).first,
        d194 = readValue(connection, PROP_D194, signed = true).first,
    )
}

/** What a hold left on the camera, so the caller can put it back afterwards. */
data class MonoToneHold(
    val slot: CameraSlot,
    val probe: MonoToneProbe,
    val previousFilmSim: Int?,
    val previousD193: Int?,
    val previousD194: Int?,
    val monoSimApplied: Boolean,
) {
    val summary: String
        get() = when {
            !monoSimApplied -> "Could not set a monochrome film simulation, so nothing was written."
            probe.accepted ->
                "${probe.hex} = ${probe.written} is on ${slot.label} now, with Acros applied. Open " +
                    "${slot.label} on the camera and look at Monochromatic Color."

            else -> "Not applied — ${probe.verdict}"
        }
}

/**
 * Writes one value and **leaves it there**, along with the monochrome film simulation it needs.
 *
 * The sweep restores everything, which is right for a measurement but useless for the question that
 * actually matters here: which axis moved. That can only be read off the camera, and only while the
 * value is still set. Restore afterwards with [restoreMonoTone].
 */
suspend fun holdMonoToneValue(
    connection: OpenPtpConnection,
    slot: CameraSlot,
    propertyCode: Int,
    value: Int,
    settleMs: Long = 150L,
): MonoToneHold {
    val label = if (propertyCode == PROP_D193) "BlackImageTone" else "MonochromaticColor"

    if (!selectSlot(connection, slot)) {
        return MonoToneHold(
            slot = slot,
            probe = MonoToneProbe(propertyCode, label, value, null, null),
            previousFilmSim = null,
            previousD193 = null,
            previousD194 = null,
            monoSimApplied = false,
        )
    }
    delay(settleMs)

    val previousFilmSim = readValue(connection, 0xD192, signed = false).first
    val previousD193 = readValue(connection, PROP_D193, signed = true).first
    val previousD194 = readValue(connection, PROP_D194, signed = true).first

    val monoResponse = write(connection, 0xD192, FujiFilmSimulation.Acros.protocolValue, settleMs)
    val monoSimApplied = monoResponse == PtpConstants.RESPONSE_OK

    val response = if (monoSimApplied) write(connection, propertyCode, value, settleMs) else null
    val readBack = if (monoSimApplied) readValue(connection, propertyCode, signed = true).first else null

    return MonoToneHold(
        slot = slot,
        probe = MonoToneProbe(propertyCode, label, value, response, readBack),
        previousFilmSim = previousFilmSim,
        previousD193 = previousD193,
        previousD194 = previousD194,
        monoSimApplied = monoSimApplied,
    )
}

/** Puts a slot back the way [holdMonoToneValue] found it. */
suspend fun restoreMonoTone(
    connection: OpenPtpConnection,
    hold: MonoToneHold,
    settleMs: Long = 150L,
): Boolean {
    if (!selectSlot(connection, hold.slot)) return false
    delay(settleMs)

    hold.previousD193?.let { write(connection, PROP_D193, it, settleMs) }
    hold.previousD194?.let { write(connection, PROP_D194, it, settleMs) }
    hold.previousFilmSim?.let { write(connection, 0xD192, it, settleMs) }

    val sim = readValue(connection, 0xD192, signed = false).first
    return hold.previousFilmSim != null && sim == hold.previousFilmSim
}

private fun selectSlot(connection: OpenPtpConnection, slot: CameraSlot): Boolean = runCatching {
    connection.executeCommandWithData(
        code = PtpConstants.SET_DEVICE_PROP_VALUE,
        params = listOf(PtpConstants.FUJI_SLOT_SELECTOR),
        payload = uint16Le(slot.protocolValue),
    ).isOk
}.getOrDefault(false)

private suspend fun write(
    connection: OpenPtpConnection,
    code: Int,
    value: Int,
    settleMs: Long,
): Int? {
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

/**
 * Sweeps both monochrome toning properties on one slot and puts everything back.
 *
 * The slot is moved to Acros first: both properties are refused under a colour film simulation —
 * confirmed on an X-H2, response `0x201C` — so a run without that step would measure the interlock
 * rather than the properties. The original simulation and both original tone values are restored at
 * the end regardless of what happened in between.
 */
suspend fun runMonoToneSweep(
    connection: OpenPtpConnection,
    slot: CameraSlot,
    settleMs: Long = 150L,
    onProgress: (MonoToneProbe) -> Unit = {},
): MonoToneSweep {
    val started = System.currentTimeMillis()

    if (!selectSlot(connection, slot)) {
        return MonoToneSweep(slot, null, null, null, false, emptyList(), false, 0)
    }
    delay(settleMs)

    val originalFilmSim = readValue(connection, 0xD192, signed = false).first
    val originalD193 = readValue(connection, PROP_D193, signed = true).first
    val originalD194 = readValue(connection, PROP_D194, signed = true).first

    // Monochrome first, or every write below is refused for the wrong reason.
    val monoResponse = write(connection, 0xD192, FujiFilmSimulation.Acros.protocolValue, settleMs)
    val monoSimApplied = monoResponse == PtpConstants.RESPONSE_OK

    val probes = mutableListOf<MonoToneProbe>()
    if (monoSimApplied) {
        for ((code, label) in listOf(PROP_D193 to "BlackImageTone", PROP_D194 to "MonochromaticColor")) {
            for (value in MONO_TONE_PROBES) {
                val response = write(connection, code, value, settleMs)
                val readBack = readValue(connection, code, signed = true).first
                val probe = MonoToneProbe(code, label, value, response, readBack)
                probes.add(probe)
                onProgress(probe)
            }
            // Leave each property where it started before moving to the next one.
            val original = if (code == PROP_D193) originalD193 else originalD194
            original?.let { write(connection, code, it, settleMs) }
        }
    }

    originalFilmSim?.let { write(connection, 0xD192, it, settleMs) }
    val restoredSim = readValue(connection, 0xD192, signed = false).first
    val restored = originalFilmSim != null && restoredSim == originalFilmSim

    return MonoToneSweep(
        slot = slot,
        originalFilmSim = originalFilmSim,
        originalD193 = originalD193,
        originalD194 = originalD194,
        monoSimApplied = monoSimApplied,
        probes = probes,
        restored = restored,
        durationMs = System.currentTimeMillis() - started,
    )
}
