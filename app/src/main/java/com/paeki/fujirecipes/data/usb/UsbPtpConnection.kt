package com.paeki.fujirecipes.data.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.paeki.fujirecipes.data.ptp.PtpContainer
import com.paeki.fujirecipes.data.ptp.PtpConstants
import com.paeki.fujirecipes.data.ptp.PtpProtocolException
import com.paeki.fujirecipes.data.ptp.PtpTransaction
import com.paeki.fujirecipes.data.ptp.buildCommandPacket
import com.paeki.fujirecipes.data.ptp.buildDataOutPacket
import java.util.concurrent.atomic.AtomicInteger

class UsbPtpConnection(
    private val usbManager: UsbManager,
) {
    fun open(device: UsbDevice): OpenPtpConnection? {
        val ptpInterface = device.findPtpInterface() ?: return null
        val connection = usbManager.openDevice(device) ?: return null

        if (!connection.claimInterface(ptpInterface, true)) {
            connection.close()
            return null
        }

        val bulkOut = ptpInterface.findEndpoint(UsbConstants.USB_ENDPOINT_XFER_BULK, UsbConstants.USB_DIR_OUT)
        val bulkIn = ptpInterface.findEndpoint(UsbConstants.USB_ENDPOINT_XFER_BULK, UsbConstants.USB_DIR_IN)
        val interruptIn = ptpInterface.findEndpoint(UsbConstants.USB_ENDPOINT_XFER_INT, UsbConstants.USB_DIR_IN)

        if (bulkOut == null || bulkIn == null) {
            connection.releaseInterface(ptpInterface)
            connection.close()
            return null
        }

        return OpenPtpConnection(connection, ptpInterface, bulkOut, bulkIn, interruptIn)
    }

    private fun UsbDevice.findPtpInterface(): UsbInterface? {
        for (index in 0 until interfaceCount) {
            val candidate = getInterface(index)
            if (candidate.interfaceClass == PtpConstants.PTP_INTERFACE_CLASS) {
                return candidate
            }
        }
        return if (interfaceCount > 0) getInterface(0) else null
    }

    private fun UsbInterface.findEndpoint(type: Int, direction: Int): UsbEndpoint? {
        for (index in 0 until endpointCount) {
            val endpoint = getEndpoint(index)
            if (endpoint.type == type && endpoint.direction == direction) {
                return endpoint
            }
        }
        return null
    }
}

data class OpenPtpConnection(
    val connection: UsbDeviceConnection,
    val ptpInterface: UsbInterface,
    val bulkOut: UsbEndpoint,
    val bulkIn: UsbEndpoint,
    val interruptIn: UsbEndpoint?,
) : AutoCloseable {
    private val nextTransactionId = AtomicInteger(1)

    fun openSession(sessionId: Int = 1): Boolean {
        val transaction = executeCommand(
            code = PtpConstants.OPEN_SESSION,
            params = listOf(sessionId),
        )
        return transaction.response.code == PtpConstants.RESPONSE_OK ||
            transaction.response.code == PtpConstants.RESPONSE_SESSION_ALREADY_OPEN
    }

    fun closeSession(): Boolean =
        executeCommand(PtpConstants.CLOSE_SESSION).isOk

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
    ): PtpTransaction {
        val transactionId = nextTransactionId.getAndIncrement()
        send(buildCommandPacket(code, transactionId, params), timeoutMs)

        val first = receiveContainer(timeoutMs)
        val response = if (first.type == PtpConstants.CONTAINER_DATA) {
            receiveContainer(timeoutMs)
        } else {
            first
        }

        return PtpTransaction(
            data = first.takeIf { it.type == PtpConstants.CONTAINER_DATA },
            response = response,
        )
    }

    fun executeCommandWithData(
        code: Int,
        params: List<Int>,
        payload: ByteArray,
        timeoutMs: Int = PtpConstants.STANDARD_TIMEOUT_MS,
    ): PtpTransaction {
        val transactionId = nextTransactionId.getAndIncrement()
        send(buildCommandPacket(code, transactionId, params), timeoutMs)
        send(buildDataOutPacket(code, transactionId, payload), timeoutMs)
        return PtpTransaction(
            data = null,
            response = receiveContainer(timeoutMs),
        )
    }

    override fun close() {
        connection.releaseInterface(ptpInterface)
        connection.close()
    }

    private fun send(bytes: ByteArray, timeoutMs: Int) {
        var offset = 0
        while (offset < bytes.size) {
            val size = minOf(PtpConstants.BULK_CHUNK_SIZE, bytes.size - offset)
            val written = connection.bulkTransfer(bulkOut, bytes, offset, size, timeoutMs)
            if (written <= 0) {
                throw PtpProtocolException("USB bulk OUT failed.")
            }
            offset += written
        }
    }

    private fun receiveContainer(timeoutMs: Int): PtpContainer {
        val first = ByteArray(PtpConstants.BULK_CHUNK_SIZE)
        val firstRead = connection.bulkTransfer(bulkIn, first, first.size, timeoutMs)
        if (firstRead < PtpContainer.HEADER_BYTES) {
            throw PtpProtocolException("USB bulk IN returned no valid PTP header.")
        }

        val expectedLength = readUInt32Le(first)
        if (expectedLength !in PtpContainer.HEADER_BYTES..PtpConstants.MAX_SMALL_CONTAINER_BYTES) {
            throw PtpProtocolException("Invalid PTP container length: $expectedLength.")
        }

        if (firstRead >= expectedLength) {
            return PtpContainer.parse(first.copyOf(expectedLength))
        }

        val full = ByteArray(expectedLength)
        first.copyInto(full, endIndex = firstRead)
        var offset = firstRead

        while (offset < expectedLength) {
            val chunkSize = minOf(PtpConstants.BULK_CHUNK_SIZE, expectedLength - offset)
            val read = connection.bulkTransfer(bulkIn, full, offset, chunkSize, timeoutMs)
            if (read <= 0) {
                throw PtpProtocolException("USB bulk IN ended before full PTP container arrived.")
            }
            offset += read
        }

        return PtpContainer.parse(full)
    }

    private fun readUInt32Le(bytes: ByteArray): Int =
        (bytes[0].toInt() and 0xFF) or
            ((bytes[1].toInt() and 0xFF) shl 8) or
            ((bytes[2].toInt() and 0xFF) shl 16) or
            ((bytes[3].toInt() and 0xFF) shl 24)
}
