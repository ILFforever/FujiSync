package com.paeki.fujirecipes.data.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
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

class CameraHeartbeat(
    private val usbManager: UsbManager,
    private val connectionFactory: UsbPtpConnection,
) {
    private val _alive = MutableStateFlow(false)
    val alive: StateFlow<Boolean> = _alive.asStateFlow()

    // All USB-opening operations (reads, writes, heartbeat) must hold this before
    // calling connectionFactory.open(). Prevents the heartbeat from force-claiming
    // the USB interface while a read or write has it open.
    val usbMutex = Mutex()

    private var consecutiveFailures = 0

    suspend fun monitor(device: UsbDevice) {
        _alive.value = true
        consecutiveFailures = 0

        while (currentCoroutineContext().isActive) {
            delay(PULSE_INTERVAL_MS)
            val ok = usbMutex.withLock {
                withContext(Dispatchers.IO) { ping(device) }
            }
            if (ok) {
                consecutiveFailures = 0
                _alive.value = true
            } else {
                consecutiveFailures++
                if (consecutiveFailures >= FAILURES_BEFORE_DEAD) {
                    _alive.value = false
                    return
                }
            }
        }
    }

    fun reset() {
        consecutiveFailures = 0
        _alive.value = false
    }

    private fun ping(device: UsbDevice): Boolean {
        if (!usbManager.hasPermission(device)) return false

        return connectionFactory.open(device)?.use { connection ->
            if (!connection.openSession()) return false
            try {
                connection.ping()
            } finally {
                runCatching { connection.closeSession() }
            }
        } ?: false
    }

    companion object {
        const val PULSE_INTERVAL_MS = 3_000L
        const val FAILURES_BEFORE_DEAD = 2
    }
}
