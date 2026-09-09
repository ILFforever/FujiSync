package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.ptpResponseName

/**
 * Getting a session open on a camera that has only just appeared, and getting it back when the
 * pipe breaks.
 *
 * Kept out of [OpenPtpConnection] because none of this is the wire protocol — it is policy about
 * how patient to be with a body that is still waking up, and it changes for different reasons than
 * the transport does.
 *
 * Everything here blocks rather than suspends. [OpenPtpConnection] is blocking throughout and its
 * callers already run on `Dispatchers.IO` behind `CameraHeartbeat.usbMutex`; making just these
 * functions suspend would force `FujiPtpProbe.probe` and everything above it to follow, and buy
 * nothing.
 */
sealed interface SessionStartResult {
    /** The camera accepted a session, first ask or after a few polls. */
    data object Opened : SessionStartResult

    /** The camera was holding a session from an earlier run; it was cleared and a fresh one opened. */
    data object Recovered : SessionStartResult

    data class Failed(val reason: String) : SessionStartResult

    val isOpen: Boolean get() = this is Opened || this is Recovered
}

/**
 * Opens a PTP session, waiting for the camera to be ready rather than taking the first answer.
 *
 * A body that has just enumerated will refuse for a moment; one that reports SessionAlreadyOpen is
 * holding state from a run that never closed cleanly, and no amount of asking again will help —
 * that one needs the session closed and the interface reset before it will hand out a new one.
 */
fun OpenPtpConnection.openSessionWhenReady(
    sessionId: Int = 1,
    attempts: Int = PtpConstants.OPEN_SESSION_ATTEMPTS,
): SessionStartResult {
    var lastCode: Int? = null
    var staleCleared = false

    repeat(attempts) { attempt ->
        if (attempt > 0) sleepQuietly(PtpConstants.OPEN_SESSION_POLL_MS)

        if (!isUsable && !resetPipe()) {
            return SessionStartResult.Failed("The USB pipe failed and a device reset did not clear it.")
        }

        val code = runCatching { openSessionResponse(sessionId).response.code }.getOrNull()
        if (code == null) return@repeat // Transport failure; the next pass resets and tries again.
        lastCode = code

        when (code) {
            PtpConstants.RESPONSE_OK ->
                return if (staleCleared) SessionStartResult.Recovered else SessionStartResult.Opened

            PtpConstants.RESPONSE_SESSION_ALREADY_OPEN -> if (!staleCleared) {
                runCatching { closeSession() }
                if (resetPipe()) sleepQuietly(PtpConstants.POST_RESET_DELAY_MS)
                staleCleared = true
            }

            // DeviceBusy is the camera saying "not yet" — exactly what the polling is for.
            else -> Unit
        }
    }

    val detail = lastCode?.let { " Last response: ${ptpResponseName(it)}." } ?: ""
    return SessionStartResult.Failed("The camera did not open a PTP session.$detail")
}

/**
 * GetDeviceInfo, retried with backoff.
 *
 * This is the first real payload we ask for and the one bodies most often fumble — some answer
 * while still settling after OpenSession and reject it or return nothing. A failure here is not
 * evidence about the camera's capabilities, so it is worth several tries before it is believed.
 */
fun OpenPtpConnection.readDeviceInfoPayload(
    attempts: Int = PtpConstants.DEVICE_INFO_ATTEMPTS,
): ByteArray? {
    repeat(attempts) { attempt ->
        if (attempt > 0) {
            if (!reviveSession()) return null
            sleepQuietly(PtpConstants.DEVICE_INFO_RETRY_BASE_DELAY_MS * attempt)
        }

        val payload = runCatching { executeCommand(PtpConstants.GET_DEVICE_INFO) }
            .getOrNull()
            ?.takeIf { it.isOk }
            ?.data
            ?.payload

        if (payload != null && payload.isNotEmpty()) return payload
    }
    return null
}

/**
 * Resets the interface so a poisoned connection can carry traffic again. Does not put a session
 * back — a reset drops the camera's session too, so callers that need one must reopen it.
 */
private fun OpenPtpConnection.resetPipe(): Boolean {
    if (state == OpenPtpConnection.State.Closed) return false
    if (!resetDevice()) return false

    clearPoison()
    return true
}

/** [resetPipe] plus a fresh session, for callers that were mid-conversation when the pipe broke. */
private fun OpenPtpConnection.reviveSession(): Boolean {
    if (isUsable) return true
    if (!resetPipe()) return false

    sleepQuietly(PtpConstants.POST_RESET_DELAY_MS)
    return openSessionWhenReady().isOpen
}

private fun sleepQuietly(millis: Long) {
    if (millis <= 0) return
    try {
        Thread.sleep(millis)
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}
