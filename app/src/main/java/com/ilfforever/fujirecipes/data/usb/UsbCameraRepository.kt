package com.ilfforever.fujirecipes.data.usb

import com.ilfforever.fujirecipes.domain.repository.CameraRepository

class UsbCameraRepository(
    private val scanner: UsbCameraScanner,
) : CameraRepository {
    override fun scanUsb(): List<FujiUsbDevice> = scanner.findFujiDevices()

    override fun currentMode(): CameraUsbMode =
        scanUsb().firstOrNull()?.mode ?: CameraUsbMode.NotPlugged
}
