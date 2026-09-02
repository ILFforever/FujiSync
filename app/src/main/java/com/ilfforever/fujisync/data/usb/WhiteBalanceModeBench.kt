package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.ptpResponseName
import com.ilfforever.fujisync.data.ptp.uint16Le
import kotlinx.coroutines.delay

// White Balance mode sweep.
//
// The protocol docs list nine WB modes, every one confirmed on hardware. Static analysis of X RAW
// STUDIO turned up a label table carrying fourteen: the nine documented ones plus Custom 1-3 and two
// Auto-priority modes. Those five are Fuji's own numbers, but no camera has been seen to accept
// them, so they sit in the reverse-engineering notes rather than the protocol documentation.
//
// This bench writes every mode in turn to the live White Balance property, reads it back, and puts
// the original mode back afterwards. A mode that reads back as written is confirmed on this body and
// can graduate into the documented table.
//
// One result needs care. Custom 1-3 select a white balance the photographer has already measured and
// stored in the camera. On a body with an empty custom slot the camera may well refuse the mode even
// though it supports it, so a rejection there is ambiguous in a way a rejection of, say, Auto
// Ambience Priority is not. The verdict text says so rather than reporting a clean "unsupported".

/** Live White Balance. Its stored counterpart is `0xD199`; both carry the same mode encoding. */
private const val LIVE_WHITE_BALANCE = 0x5005

data class WbMode(
    val value: Int,
    val label: String,
    /** Already documented as hardware-confirmed, so it doubles as a control for this run. */
    val documented: Boolean,
    /** Selecting this mode recalls a white balance the user must have measured beforehand. */
    val needsStoredMeasurement: Boolean = false,
) {
    val hex: String get() = "0x%04X".format(value)
}

/** Ordered as the camera menu presents them, so a run is easy to follow on the body itself. */
val WB_MODES: List<WbMode> = listOf(
    WbMode(0x0002, "Auto", documented = true),
    WbMode(0x8020, "Auto White Priority", documented = false),
    WbMode(0x8021, "Auto Ambience Priority", documented = false),
    WbMode(0x8008, "Custom 1", documented = false, needsStoredMeasurement = true),
    WbMode(0x8009, "Custom 2", documented = false, needsStoredMeasurement = true),
    WbMode(0x800A, "Custom 3", documented = false, needsStoredMeasurement = true),
    WbMode(0x8007, "Color Temperature", documented = true),
    WbMode(0x0004, "Daylight", documented = true),
    WbMode(0x8006, "Shade", documented = true),
    WbMode(0x8001, "Fluorescent 1", documented = true),
    WbMode(0x8002, "Fluorescent 2", documented = true),
    WbMode(0x8003, "Fluorescent 3", documented = true),
    WbMode(0x0006, "Incandescent", documented = true),
    WbMode(0x0008, "Underwater", documented = true),
)

data class WbModeResult(
    val mode: WbMode,
    val writeResponse: Int?,
    val readBack: Int?,
) {
    val accepted: Boolean get() = readBack == mode.value

    /** A documented mode that fails means the run itself is suspect, not that the mode is gone. */
    val isControlFailure: Boolean get() = mode.documented && !accepted

    val verdict: String
        get() = when {
            accepted -> "ACCEPTED — read back ${mode.hex}"

            writeResponse == PtpConstants.RESPONSE_OK ->
                "NOT APPLIED — camera returned OK but read back " +
                    (readBack?.let { "0x%04X".format(it) } ?: "nothing")

            writeResponse == RESPONSE_INVALID_VALUE && mode.needsStoredMeasurement ->
                "REJECTED — but this mode recalls a stored measurement. Measure a custom white " +
                    "balance into this slot on the camera and re-run before concluding anything."

            writeResponse == RESPONSE_INVALID_VALUE -> "REJECTED — value not legal on this body"
            writeResponse == RESPONSE_PROP_NOT_SUPPORTED -> "PROPERTY ABSENT — 0x5005 not supported"
            writeResponse == null -> "NO RESPONSE — transport error"
            else -> "FAILED — ${ptpResponseName(writeResponse)}"
        }
}

data class WbModeSweep(
    val original: Int?,
    val restored: Int?,
    val results: List<WbModeResult>,
    val durationMs: Long,
) {
    val cameraRestored: Boolean get() = original != null && restored == original

    val confirmed: List<WbModeResult> get() = results.filter { it.accepted && !it.mode.documented }
    val controlFailures: List<WbModeResult> get() = results.filter { it.isControlFailure }

    /**
     * A run only means something if the modes already confirmed on hardware still pass. If they do
     * not, the fault is in the run — a wrong dial position, a busy camera — not in the new modes.
     */
    val trustworthy: Boolean get() = controlFailures.isEmpty()

    val summary: String
        get() = when {
            original == null -> "White Balance could not be read; nothing was written."
            !trustworthy ->
                "${controlFailures.size} already-documented mode(s) failed, so this run is not " +
                    "trustworthy. Check the camera is idle and in USB RAW CONV., then re-run."
            confirmed.isEmpty() ->
                "No new modes accepted. The ${results.count { it.mode.documented && it.accepted }} " +
                    "documented modes behaved as expected."
            else ->
                "${confirmed.size} previously-underived mode(s) accepted: " +
                    confirmed.joinToString { "${it.mode.label} ${it.mode.hex}" }
        }
}

private const val RESPONSE_INVALID_VALUE = 0x201C
private const val RESPONSE_PROP_NOT_SUPPORTED = 0x200A

private suspend fun writeMode(connection: OpenPtpConnection, value: Int, settleMs: Long): Int? {
    val tx = runCatching {
        connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(LIVE_WHITE_BALANCE),
            payload = uint16Le(value),
        )
    }.getOrNull()
    delay(settleMs)
    return tx?.response?.code
}

/**
 * Writes every White Balance mode in turn and reads each one back, then restores the mode the camera
 * started in. Modes already documented act as controls: if they fail, the run is reported as
 * untrustworthy rather than as evidence about the undocumented ones.
 */
suspend fun runWbModeSweep(
    connection: OpenPtpConnection,
    settleMs: Long = 150L,
    onProgress: (WbModeResult) -> Unit = {},
): WbModeSweep {
    val started = System.currentTimeMillis()
    val original = readValue(connection, LIVE_WHITE_BALANCE, signed = false).first

    if (original == null) {
        return WbModeSweep(
            original = null,
            restored = null,
            results = emptyList(),
            durationMs = System.currentTimeMillis() - started,
        )
    }

    val results = mutableListOf<WbModeResult>()
    for (mode in WB_MODES) {
        val response = writeMode(connection, mode.value, settleMs)
        val readBack = readValue(connection, LIVE_WHITE_BALANCE, signed = false).first
        val result = WbModeResult(mode, response, readBack)
        results.add(result)
        onProgress(result)
    }

    writeMode(connection, original, settleMs)
    val restored = readValue(connection, LIVE_WHITE_BALANCE, signed = false).first

    return WbModeSweep(
        original = original,
        restored = restored,
        results = results,
        durationMs = System.currentTimeMillis() - started,
    )
}
