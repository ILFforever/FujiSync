package com.paeki.fujirecipes.data.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.paeki.fujirecipes.data.ptp.PtpConstants

data class FujiUsbDevice(
    val device: UsbDevice,
    val deviceName: String,
    val productName: String?,
    val vendorId: Int,
    val productId: Int,
    val mode: CameraUsbMode,
)

class UsbCameraScanner(
    private val usbManager: UsbManager,
) {
    fun findFujiDevices(): List<FujiUsbDevice> =
        usbManager.deviceList.values
            .filter { it.vendorId == PtpConstants.FUJI_VENDOR_ID }
            .map { device ->
                FujiUsbDevice(
                    device = device,
                    deviceName = device.deviceName,
                    productName = device.productName,
                    vendorId = device.vendorId,
                    productId = device.productId,
                    mode = device.detectMode(),
                )
            }

    private fun UsbDevice.detectMode(): CameraUsbMode {
        var sawPtp = false
        var sawMassStorage = false

        for (index in 0 until interfaceCount) {
            when (getInterface(index).interfaceClass) {
                PtpConstants.PTP_INTERFACE_CLASS -> sawPtp = true
                PtpConstants.MASS_STORAGE_INTERFACE_CLASS -> sawMassStorage = true
            }
        }

        return when {
            sawMassStorage -> CameraUsbMode.CardReader
            sawPtp -> CameraUsbMode.Ptp
            else -> CameraUsbMode.Other
        }
    }
}
