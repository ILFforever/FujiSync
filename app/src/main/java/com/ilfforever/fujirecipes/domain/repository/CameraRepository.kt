package com.ilfforever.fujirecipes.domain.repository

import com.ilfforever.fujirecipes.data.usb.CameraUsbMode
import com.ilfforever.fujirecipes.data.usb.FujiUsbDevice

interface CameraRepository {
    fun scanUsb(): List<FujiUsbDevice>
    fun currentMode(): CameraUsbMode
}
