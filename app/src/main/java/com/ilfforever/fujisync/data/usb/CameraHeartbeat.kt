package com.ilfforever.fujisync.data.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.RecipePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class CameraHeartbeat(
    private val usbManager: UsbManager,
    private val connectionFactory: UsbPtpConnection,
) {
    private val _alive = MutableStateFlow(false)
    val alive: StateFlow<Boolean> = _alive.asStateFlow()

    private val _slots = MutableStateFlow<List<RecipePreset>>(emptyList())
    val slots: StateFlow<List<RecipePreset>> = _slots.asStateFlow()

    // All USB-opening operations (reads, writes, heartbeat) must hold this before
    // calling connectionFactory.open(). Prevents the heartbeat from force-claiming
    // the USB interface while a read or write has it open.
    val usbMutex = Mutex()

    private val consecutiveFailures = AtomicInteger(0)

    suspend fun monitor(device: UsbDevice) {
        _alive.value = true
        consecutiveFailures.set(0)

        while (currentCoroutineContext().isActive) {
            delay(PULSE_INTERVAL_MS)
            val presets = usbMutex.withLock {
                withContext(Dispatchers.IO) { readAllSlots(device) }
            }
            if (presets != null) {
                consecutiveFailures.set(0)
                _alive.value = true
                _slots.value = presets
            } else {
                if (consecutiveFailures.incrementAndGet() >= FAILURES_BEFORE_DEAD) {
                    _alive.value = false
                    return
                }
            }
        }
    }

    fun reset() {
        consecutiveFailures.set(0)
        _alive.value = false
        _slots.value = emptyList()
    }

    private suspend fun readAllSlots(device: UsbDevice): List<RecipePreset>? {
        if (!usbManager.hasPermission(device)) return null

        return connectionFactory.open(device)?.use { connection ->
            if (!connection.openSession()) return null
            try {
                val cam = FujiRecipeCamera(connection)
                val results = mutableListOf<RecipePreset>()
                for (slot in CameraSlot.entries) {
                    results.add(runCatching { cam.readPreset(slot) }.getOrNull() ?: return null)
                }
                results
            } finally {
                runCatching { connection.closeSession() }
            }
        }
    }

    companion object {
        const val PULSE_INTERVAL_MS = 3_000L
        const val FAILURES_BEFORE_DEAD = 2
    }
}
