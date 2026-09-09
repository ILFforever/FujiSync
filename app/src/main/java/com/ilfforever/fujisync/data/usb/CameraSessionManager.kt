package com.ilfforever.fujisync.data.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.ilfforever.fujisync.data.capability.CameraCapability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the app's one PTP conversation with the camera.
 *
 * The connection is opened once and held, not reopened per operation. Claiming the interface,
 * opening a session and waiting for the body to settle costs far more than the commands that
 * follow it, and older cameras are the least tolerant of having that done to them over and over.
 *
 * Every path that touches the USB device takes [usbMutex] — including [withExclusiveUsb], for
 * callers that open a handle of their own. `claimInterface(force = true)` would otherwise take the
 * interface away from the held connection without either side noticing.
 */
@Singleton
class CameraSessionManager @Inject constructor(
    private val usbManager: UsbManager,
    private val connectionFactory: UsbPtpConnection,
) {
    val usbMutex = Mutex()

    private var held: Held? = null

    private class Held(val deviceName: String, val connection: OpenPtpConnection)

    /** Runs [block] against the held session, opening one if there isn't a usable one already. */
    suspend fun <T> withSession(
        device: UsbDevice,
        writeDelayMs: Long = 0L,
        /** What the attached body accepts; defaults to gating nothing. */
        capability: CameraCapability = CameraCapability.Unknown,
        block: suspend (camera: FujiRecipeCamera, connection: OpenPtpConnection) -> T,
    ): Result<T> = withRawSession(device) { connection ->
        block(FujiRecipeCamera(connection, writeDelayMs, capability), connection)
    }

    /** As [withSession], without the [FujiRecipeCamera] wrapper. */
    suspend fun <T> withRawSession(
        device: UsbDevice,
        block: suspend (connection: OpenPtpConnection) -> T,
    ): Result<T> = usbMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = acquire(device)
                try {
                    block(connection)
                } finally {
                    // A transport failure poisons the connection. Dropping it here means the next
                    // caller pays for a reconnect instead of inheriting a pipe that cannot answer.
                    if (!connection.isUsable) releaseLocked()
                }
            }
        }
    }

    /**
     * Runs [block] with the camera to itself, closing the held connection first.
     *
     * For the dev benches, which open their own handle rather than going through the session. Two
     * live claims on one interface is not a thing the USB stack will refuse — it just quietly
     * moves the claim, and the older handle then talks to nothing.
     */
    suspend fun <T> withExclusiveUsb(block: suspend () -> T): T = usbMutex.withLock {
        releaseLocked()
        block()
    }

    /** Closes the held connection. For when the camera goes away or the user disconnects. */
    suspend fun release() = usbMutex.withLock { releaseLocked() }

    /**
     * Closes without waiting for the mutex, for teardown paths that cannot suspend. An operation
     * in flight will see its connection go out from under it and fail — which is what tearing down
     * while busy means, and better than leaking the interface claim past the app's own lifetime.
     */
    fun releaseImmediately() {
        held?.connection?.close()
        held = null
    }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    private fun acquire(device: UsbDevice): OpenPtpConnection {
        held?.let { current ->
            // Same physical device and a pipe that still works, or it is not worth keeping.
            if (current.deviceName == device.deviceName && current.connection.isUsable) {
                return current.connection
            }
            releaseLocked()
        }

        val connection = connectionFactory.open(device)
            ?: error("Unable to open the camera's PTP USB interface.")

        val start = connection.openSessionWhenReady()
        if (!start.isOpen) {
            connection.close()
            error((start as SessionStartResult.Failed).reason)
        }

        held = Held(device.deviceName, connection)
        return connection
    }

    private fun releaseLocked() {
        held?.connection?.close()
        held = null
    }
}
