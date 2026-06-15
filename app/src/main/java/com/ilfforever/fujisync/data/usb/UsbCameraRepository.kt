package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.domain.repository.CameraRepository

class UsbCameraRepository(
    private val scanner: UsbCameraScanner,
) : CameraRepository {
    override fun scanUsb(): List<FujiUsbDevice> = scanner.findFujiDevices()

    override fun currentMode(): CameraUsbMode =
        scanUsb().firstOrNull()?.mode ?: CameraUsbMode.NotPlugged
}
