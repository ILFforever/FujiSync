package com.paeki.fujirecipes.domain.repository

import com.paeki.fujirecipes.data.usb.CameraUsbMode
import com.paeki.fujirecipes.data.usb.FujiUsbDevice

interface CameraRepository {
    fun scanUsb(): List<FujiUsbDevice>
    fun currentMode(): CameraUsbMode
}
