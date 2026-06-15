package com.ilfforever.fujirecipes.data.ptp

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PtpDeviceInfo(
    val manufacturer: String,
    val model: String,
    val deviceVersion: String,
    val serialNumber: String,
    val supportedDeviceProperties: List<Int>,
) {
    val supportsFujiRecipeSlots: Boolean
        get() = PtpConstants.FUJI_SLOT_SELECTOR in supportedDeviceProperties
}

fun parseDeviceInfo(payload: ByteArray): PtpDeviceInfo {
    if (payload.size < MIN_DEVICE_INFO_BYTES) {
        throw PtpProtocolException("DeviceInfo payload too short: ${payload.size} bytes.")
    }

    val reader = PtpPayloadReader(payload)
    reader.skip(8)
    reader.skipPtpString()
    reader.skip(2)
    reader.skipUInt16Array()
    reader.skipUInt16Array()
    val supportedProperties = reader.readUInt16Array()
    reader.skipUInt16Array()
    reader.skipUInt16Array()
    val manufacturer = reader.readPtpString()
    val model = reader.readPtpString()
    val deviceVersion = reader.readPtpString()
    val serialNumber = reader.readPtpString()

    if (supportedProperties.isEmpty()) {
        throw PtpProtocolException("DeviceInfo declared no supported properties — payload may be malformed.")
    }

    return PtpDeviceInfo(
        manufacturer = manufacturer,
        model = model,
        deviceVersion = deviceVersion,
        serialNumber = serialNumber,
        supportedDeviceProperties = supportedProperties,
    )
}

private const val MIN_DEVICE_INFO_BYTES = 20

fun parsePtpString(payload: ByteArray): String {
    if (payload.isEmpty()) return ""

    val reader = PtpPayloadReader(payload)
    return reader.readPtpString()
}

private class PtpPayloadReader(payload: ByteArray) {
    private val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

    fun skip(bytes: Int) {
        if (bytes < 0 || buffer.remaining() < bytes) {
            throw PtpProtocolException("DeviceInfo overruns payload at offset ${buffer.position()} (+$bytes).")
        }
        buffer.position(buffer.position() + bytes)
    }

    fun readUInt16Array(): List<Int> {
        if (buffer.remaining() < Int.SIZE_BYTES) {
            throw PtpProtocolException("DeviceInfo array length missing at offset ${buffer.position()}.")
        }
        val count = buffer.int
        if (count < 0 || count.toLong() * Short.SIZE_BYTES > buffer.remaining()) {
            throw PtpProtocolException("DeviceInfo declared $count entries — payload only has ${buffer.remaining()} bytes left.")
        }
        val values = ArrayList<Int>(count)
        repeat(count) { values += buffer.short.toInt() and 0xFFFF }
        return values
    }

    fun skipUInt16Array() {
        if (buffer.remaining() < Int.SIZE_BYTES) {
            throw PtpProtocolException("DeviceInfo array length missing at offset ${buffer.position()}.")
        }
        val count = buffer.int
        val bytes = count.coerceAtLeast(0) * Short.SIZE_BYTES
        if (bytes > buffer.remaining()) {
            throw PtpProtocolException("DeviceInfo array overruns payload: $count entries, ${buffer.remaining()} bytes left.")
        }
        skip(bytes)
    }

    fun readPtpString(): String {
        if (!buffer.hasRemaining()) return ""

        val count = buffer.get().toInt() and 0xFF
        if (count == 0) return ""

        val byteCount = count * Short.SIZE_BYTES
        if (buffer.remaining() < byteCount) {
            throw PtpProtocolException("PTP string declares $count chars but only ${buffer.remaining()} bytes remain.")
        }

        val builder = StringBuilder(count)
        repeat(count) { index ->
            val codeUnit = buffer.short.toInt() and 0xFFFF
            if (index < count - 1 && codeUnit != 0) builder.append(codeUnit.toChar())
        }
        return builder.toString()
    }

    fun skipPtpString() {
        if (!buffer.hasRemaining()) return

        val count = buffer.get(buffer.position()).toInt() and 0xFF
        val bytes = 1 + (count * Short.SIZE_BYTES)
        if (buffer.remaining() < bytes) {
            throw PtpProtocolException("PTP string declares $count chars but only ${buffer.remaining()} bytes remain.")
        }
        skip(bytes)
    }
}
