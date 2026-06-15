package com.ilfforever.fujisync.domain.repository

import com.ilfforever.fujisync.data.usb.CameraUsbMode
import com.ilfforever.fujisync.data.usb.FujiUsbDevice

interface CameraRepository {
    fun scanUsb(): List<FujiUsbDevice>
    fun currentMode(): CameraUsbMode
}
