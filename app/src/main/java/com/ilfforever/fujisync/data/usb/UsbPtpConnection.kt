package com.ilfforever.fujisync.data.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.ilfforever.fujisync.data.ptp.PtpConstants

/**
 * Finds the interface to talk PTP on and claims it. The conversation itself is
 * [OpenPtpConnection]; getting a session open on it is `PtpSessionStartup.kt`.
 */
class UsbPtpConnection(
    private val usbManager: UsbManager,
) {
    fun open(device: UsbDevice): OpenPtpConnection? {
        val ptpInterface = device.findPtpInterface() ?: return null
        val bulkOut = ptpInterface.findEndpoint(UsbConstants.USB_ENDPOINT_XFER_BULK, UsbConstants.USB_DIR_OUT)
        val bulkIn = ptpInterface.findEndpoint(UsbConstants.USB_ENDPOINT_XFER_BULK, UsbConstants.USB_DIR_IN)
        val interruptIn = ptpInterface.findEndpoint(UsbConstants.USB_ENDPOINT_XFER_INT, UsbConstants.USB_DIR_IN)
        if (bulkOut == null || bulkIn == null) return null

        val connection = usbManager.openDevice(device) ?: return null
        if (!connection.claimInterface(ptpInterface, true)) {
            connection.close()
            return null
        }

        // A few bodies put the PTP endpoints on a non-default alternate setting. Claiming succeeds
        // either way, so skipping this leaves a connection that looks fine and transfers nothing.
        if (ptpInterface.alternateSetting != 0 && !connection.setInterface(ptpInterface)) {
            runCatching { connection.releaseInterface(ptpInterface) }
            connection.close()
            return null
        }

        return OpenPtpConnection(connection, ptpInterface, bulkOut, bulkIn, interruptIn)
    }

    /**
     * Only an interface carrying both bulk directions can be the PTP one, so candidates are
     * filtered on that first. The Still Image class wins outright; otherwise a lone candidate is
     * accepted, on the reasoning that a body exposing exactly one bulk pair means it.
     *
     * Deliberately no fall back to interface 0 — on a camera in card-reader mode that is mass
     * storage, which has a bulk pair too and would swallow PTP traffic without complaining.
     */
    private fun UsbDevice.findPtpInterface(): UsbInterface? {
        val candidates = (0 until interfaceCount)
            .map { getInterface(it) }
            .filter { it.hasBulkPair() }

        return candidates.firstOrNull { it.interfaceClass == PtpConstants.PTP_INTERFACE_CLASS }
            ?: candidates.singleOrNull()
    }

    private fun UsbInterface.hasBulkPair(): Boolean =
        findEndpoint(UsbConstants.USB_ENDPOINT_XFER_BULK, UsbConstants.USB_DIR_IN) != null &&
            findEndpoint(UsbConstants.USB_ENDPOINT_XFER_BULK, UsbConstants.USB_DIR_OUT) != null
}

private fun UsbInterface.findEndpoint(type: Int, direction: Int): UsbEndpoint? {
    for (index in 0 until endpointCount) {
        val endpoint = getEndpoint(index)
        if (endpoint.type == type && endpoint.direction == direction) return endpoint
    }
    return null
}
