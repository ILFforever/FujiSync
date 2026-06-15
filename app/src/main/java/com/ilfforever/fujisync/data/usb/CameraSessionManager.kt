package com.ilfforever.fujisync.data.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point of entry for all USB PTP operations that require opening a session.
 * Acquires [CameraHeartbeat.usbMutex], opens a connection, runs [block], and guarantees cleanup.
 */
@Singleton
class CameraSessionManager @Inject constructor(
    private val usbManager: UsbManager,
    private val connectionFactory: UsbPtpConnection,
    private val heartbeat: CameraHeartbeat,
) {
    /**
     * Executes [block] within an open PTP session, holding the USB mutex.
     * Returns the result of [block] or a failure if the connection/session could not be established.
     */
    suspend fun <T> withSession(
        device: UsbDevice,
        writeDelayMs: Long = 0L,
        block: suspend (camera: FujiRecipeCamera, connection: OpenPtpConnection) -> T,
    ): Result<T> = heartbeat.usbMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = connectionFactory.open(device)
                    ?: error("Unable to open the camera's PTP USB interface.")
                connection.use {
                    check(connection.openSession()) { "OpenSession rejected." }
                    try {
                        block(FujiRecipeCamera(connection, writeDelayMs), connection)
                    } finally {
                        runCatching { connection.closeSession() }
                    }
                }
            }
        }
    }

    /**
     * Opens a raw connection (no [FujiRecipeCamera] wrapper) for read-only operations
     * that need direct connection access.
     */
    suspend fun <T> withRawSession(
        device: UsbDevice,
        block: suspend (connection: OpenPtpConnection) -> T,
    ): Result<T> = heartbeat.usbMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = connectionFactory.open(device)
                    ?: error("Unable to open the camera's PTP USB interface.")
                connection.use {
                    check(connection.openSession()) { "OpenSession rejected." }
                    try {
                        block(connection)
                    } finally {
                        runCatching { connection.closeSession() }
                    }
                }
            }
        }
    }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)
}
