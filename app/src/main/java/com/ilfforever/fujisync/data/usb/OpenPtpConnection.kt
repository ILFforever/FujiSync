package com.ilfforever.fujisync.data.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import com.ilfforever.fujisync.data.ptp.PtpContainer
import com.ilfforever.fujisync.data.ptp.PtpContainerReader
import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.PtpTransaction
import com.ilfforever.fujisync.data.ptp.PtpTransportException
import com.ilfforever.fujisync.data.ptp.buildCommandPacket
import com.ilfforever.fujisync.data.ptp.buildDataOutPacket
import java.util.concurrent.atomic.AtomicInteger

/**
 * One claimed PTP interface.
 *
 * Blocking by design — every call sits on `bulkTransfer`, and callers already run it on
 * `Dispatchers.IO` behind `CameraHeartbeat.usbMutex`. Session startup and recovery policy lives in
 * `PtpSessionStartup.kt`; this type is only the wire.
 */
class OpenPtpConnection(
    val connection: UsbDeviceConnection,
    val ptpInterface: UsbInterface,
    val bulkOut: UsbEndpoint,
    val bulkIn: UsbEndpoint,
    val interruptIn: UsbEndpoint?,
) : AutoCloseable {
    enum class State { Usable, Poisoned, Closed }

    private val nextTransactionId = AtomicInteger(1)
    private val reader = PtpContainerReader()

    @Volatile
    var state: State = State.Usable
        private set

    private var poisonCause: PtpTransportException? = null
    private var sessionOpen = false

    val isUsable: Boolean get() = state == State.Usable

    fun openSession(sessionId: Int = 1): Boolean {
        val code = executeCommand(PtpConstants.OPEN_SESSION, params = listOf(sessionId)).response.code
        // SessionAlreadyOpen means the camera is still holding a session from a previous run. It is
        // usable, but it is not ours; PtpSessionStartup treats it as something to clear.
        sessionOpen = code == PtpConstants.RESPONSE_OK || code == PtpConstants.RESPONSE_SESSION_ALREADY_OPEN
        return sessionOpen
    }

    /** The raw OpenSession response, for callers that need to tell "already open" from "opened". */
    fun openSessionResponse(sessionId: Int = 1): PtpTransaction =
        executeCommand(PtpConstants.OPEN_SESSION, params = listOf(sessionId))
            .also { sessionOpen = it.response.code == PtpConstants.RESPONSE_OK }

    fun closeSession(): Boolean {
        val ok = executeCommand(PtpConstants.CLOSE_SESSION, timeoutMs = PtpConstants.CLOSE_SESSION_TIMEOUT_MS).isOk
        sessionOpen = false
        return ok
    }

    fun ping(): Boolean =
        try {
            val transaction = executeCommand(
                code = PtpConstants.GET_DEVICE_INFO,
                timeoutMs = PtpConstants.HEARTBEAT_TIMEOUT_MS,
            )
            transaction.isOk && transaction.data != null
        } catch (_: Exception) {
            false
        }

    fun executeCommand(
        code: Int,
        params: List<Int> = emptyList(),
        timeoutMs: Int = PtpConstants.STANDARD_TIMEOUT_MS,
    ): PtpTransaction = runTransaction(code, params, null, timeoutMs)

    fun executeCommandWithData(
        code: Int,
        params: List<Int>,
        payload: ByteArray,
        timeoutMs: Int = PtpConstants.STANDARD_TIMEOUT_MS,
    ): PtpTransaction = runTransaction(code, params, payload, timeoutMs)

    /**
     * Sends one operation and returns every container the camera replies with, for diagnostics.
     * Unlike [executeCommand] this makes no assumption about how many containers arrive or in what
     * order, and it deliberately does not check transaction IDs or poison the connection — its
     * whole job is to show what a camera does with an operation we suspect we are calling wrongly,
     * and a bench that kills the session on the first odd reply cannot show you the second one.
     */
    fun rawExchange(
        code: Int,
        params: List<Int> = emptyList(),
        timeoutMs: Int = PtpConstants.STANDARD_TIMEOUT_MS,
        maxContainers: Int = 4,
    ): List<PtpContainer> {
        val transactionId = nextTransactionId.getAndIncrement()
        send(buildCommandPacket(code, transactionId, params), timeoutMs)

        val received = mutableListOf<PtpContainer>()
        repeat(maxContainers) {
            val container = try {
                receiveContainer(timeoutMs)
            } catch (_: Exception) {
                return received
            }
            received.add(container)
            if (container.type == PtpConstants.CONTAINER_RESPONSE) return received
        }
        return received
    }

    /**
     * Still Image class device reset. Clears the camera's transaction state so a desynced pipe can
     * be used again; the connection stays poisoned until a caller explicitly revives it, because a
     * reset alone does not put a PTP session back.
     */
    fun resetDevice(): Boolean {
        if (state == State.Closed) return false
        if (ptpInterface.interfaceClass != PtpConstants.PTP_INTERFACE_CLASS) return false

        val result = try {
            connection.controlTransfer(
                PtpConstants.STILL_IMAGE_RESET_REQUEST_TYPE,
                PtpConstants.STILL_IMAGE_RESET_REQUEST,
                0,
                ptpInterface.id,
                null,
                0,
                PtpConstants.STILL_IMAGE_RESET_TIMEOUT_MS,
            )
        } catch (_: Exception) {
            -1
        }

        if (result == 0) {
            reader.reset()
            sessionOpen = false
        }
        return result == 0
    }

    /** Marks a reset connection usable again. Only [PtpSessionStartup] should need this. */
    internal fun clearPoison() {
        if (state != State.Poisoned) return
        state = State.Usable
        poisonCause = null
        reader.reset()
    }

    override fun close() {
        if (state == State.Closed) return
        // Only if we still believe a session is open — CameraSessionManager closes explicitly, and
        // sending CloseSession twice earns a SessionNotOpen for no reason.
        if (state == State.Usable && sessionOpen) runCatching { closeSession() }

        state = State.Closed
        sessionOpen = false
        reader.reset()
        runCatching { connection.releaseInterface(ptpInterface) }
        connection.close()
    }

    private fun runTransaction(
        code: Int,
        params: List<Int>,
        outData: ByteArray?,
        timeoutMs: Int,
    ): PtpTransaction {
        requireUsable()
        return poisonOnTransportFailure {
            val transactionId = nextTransactionId.getAndIncrement()
            send(buildCommandPacket(code, transactionId, params), timeoutMs)
            if (outData != null) send(buildDataOutPacket(code, transactionId, outData), timeoutMs)

            var data: PtpContainer? = null
            var response: PtpContainer? = null

            while (response == null) {
                val container = receiveContainer(timeoutMs)

                // A container for someone else's transaction means we would be reading the reply to
                // a command that already gave up. Answering with it is worse than failing.
                if (container.transactionId != transactionId) {
                    throw desync("Expected transaction $transactionId, got ${container.transactionId}.")
                }

                when (container.type) {
                    PtpConstants.CONTAINER_DATA -> {
                        if (outData != null || data != null) {
                            throw desync("Unexpected data container for operation 0x%04X.".format(code))
                        }
                        data = container
                    }

                    PtpConstants.CONTAINER_RESPONSE -> response = container

                    else -> throw desync("Unexpected container type ${container.type} on bulk IN.")
                }
            }

            PtpTransaction(data, response)
        }
    }

    private fun send(bytes: ByteArray, timeoutMs: Int) {
        var offset = 0
        while (offset < bytes.size) {
            val size = minOf(PtpConstants.BULK_CHUNK_SIZE, bytes.size - offset)
            val written = try {
                connection.bulkTransfer(bulkOut, bytes, offset, size, timeoutMs)
            } catch (e: Exception) {
                throw PtpTransportException(
                    PtpTransportException.Kind.BulkIo,
                    "USB bulk OUT failed after $offset of ${bytes.size} bytes.",
                    e,
                )
            }
            if (written <= 0) {
                throw PtpTransportException(
                    PtpTransportException.Kind.BulkIo,
                    "USB bulk OUT failed after $offset of ${bytes.size} bytes.",
                )
            }
            offset += written
        }
    }

    /**
     * Always asks for a full buffer and lets [PtpContainerReader] decide where containers end.
     * Requesting only the bytes still outstanding looks tidier and is wrong: a device sending a
     * full max-size packet into a smaller request overflows, and Android reports that as failure.
     */
    private fun receiveContainer(timeoutMs: Int): PtpContainer {
        reader.next()?.let { return it }

        val chunk = ByteArray(PtpConstants.BULK_CHUNK_SIZE)
        while (true) {
            val read = try {
                connection.bulkTransfer(bulkIn, chunk, chunk.size, timeoutMs)
            } catch (e: Exception) {
                throw receiveFailure(e)
            }
            if (read <= 0) throw receiveFailure(null)

            reader.append(chunk, read)
            reader.next()?.let { return it }
        }
    }

    private fun receiveFailure(cause: Throwable?): PtpTransportException =
        if (reader.buffered > 0) {
            PtpTransportException(
                PtpTransportException.Kind.Desync,
                "USB bulk IN ended with an incomplete PTP container (${reader.buffered} bytes held).",
                cause,
            )
        } else {
            PtpTransportException(
                PtpTransportException.Kind.BulkIo,
                "USB bulk IN timed out or failed.",
                cause,
            )
        }

    private fun desync(message: String) =
        PtpTransportException(PtpTransportException.Kind.Desync, message)

    private inline fun <T> poisonOnTransportFailure(block: () -> T): T =
        try {
            block()
        } catch (e: PtpTransportException) {
            poison(e)
            throw e
        }

    private fun poison(error: PtpTransportException) {
        if (state == State.Closed) return
        state = State.Poisoned
        if (poisonCause == null) poisonCause = error
        sessionOpen = false
        reader.reset()
    }

    private fun requireUsable() {
        when (state) {
            State.Usable -> Unit
            State.Closed -> throw PtpTransportException(
                PtpTransportException.Kind.BulkIo,
                "The PTP connection is closed.",
            )

            State.Poisoned -> throw PtpTransportException(
                poisonCause?.kind ?: PtpTransportException.Kind.Desync,
                "The PTP connection is poisoned and cannot run another command.",
                poisonCause,
            )
        }
    }
}
