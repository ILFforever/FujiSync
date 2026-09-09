package com.ilfforever.fujisync.data.usb

import android.hardware.usb.UsbDevice
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.RecipePreset
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicInteger

/**
 * Keeps an eye on whether the camera is still there, and picks up changes made on the body itself.
 *
 * Those are two different questions with two different costs, so they run at two different rates.
 * Liveness is one round trip. Re-reading all seven slots is closer to a hundred and seventy, and
 * only tells us something new when someone has been editing recipes on the camera — so it happens
 * far less often than the check that the cable is still connected.
 */
class CameraHeartbeat(
    private val sessionManager: CameraSessionManager,
) {
    private val _alive = MutableStateFlow(false)
    val alive: StateFlow<Boolean> = _alive.asStateFlow()

    private val _slots = MutableStateFlow<List<RecipePreset>>(emptyList())
    val slots: StateFlow<List<RecipePreset>> = _slots.asStateFlow()

    private val consecutiveFailures = AtomicInteger(0)

    suspend fun monitor(device: UsbDevice) {
        _alive.value = true
        consecutiveFailures.set(0)
        var pulse = 0

        while (currentCoroutineContext().isActive) {
            delay(PULSE_INTERVAL_MS)

            val refreshing = pulse % SLOT_REFRESH_EVERY_PULSES == 0
            pulse++

            val ok = if (refreshing) refreshSlots(device) else pingCamera(device)

            if (ok) {
                consecutiveFailures.set(0)
                _alive.value = true
            } else if (consecutiveFailures.incrementAndGet() >= FAILURES_BEFORE_DEAD) {
                _alive.value = false
                // Nothing is going to answer on this connection again; holding the interface claim
                // would only stop a reconnect from getting it.
                sessionManager.release()
                return
            }
        }
    }

    fun reset() {
        consecutiveFailures.set(0)
        _alive.value = false
        _slots.value = emptyList()
    }

    private suspend fun pingCamera(device: UsbDevice): Boolean {
        if (!sessionManager.hasPermission(device)) return false
        return sessionManager.withRawSession(device) { it.ping() }.getOrDefault(false)
    }

    /** Any slot failing to read counts as a failed pulse — a partial board is worse than none. */
    private suspend fun refreshSlots(device: UsbDevice): Boolean {
        if (!sessionManager.hasPermission(device)) return false

        val presets = sessionManager.withRawSession(device) { connection ->
            val camera = FujiRecipeCamera(connection)
            CameraSlot.entries.map { camera.readPreset(it) }
        }.getOrNull() ?: return false

        _slots.value = presets
        return true
    }

    companion object {
        const val PULSE_INTERVAL_MS = 3_000L
        const val FAILURES_BEFORE_DEAD = 2

        /** Every tenth pulse — a full board re-read roughly every 30 seconds. */
        const val SLOT_REFRESH_EVERY_PULSES = 10
    }
}
