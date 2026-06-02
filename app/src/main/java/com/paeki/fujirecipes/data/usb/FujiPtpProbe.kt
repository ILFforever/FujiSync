package com.paeki.fujirecipes.data.usb

import android.hardware.usb.UsbDevice
import android.util.Log
import com.paeki.fujirecipes.data.ptp.PtpConstants
import com.paeki.fujirecipes.data.ptp.PtpDeviceInfo
import com.paeki.fujirecipes.data.ptp.hexDump
import com.paeki.fujirecipes.data.ptp.parseDeviceInfo
import com.paeki.fujirecipes.data.ptp.parsePtpString
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed interface FujiPtpProbeResult {
    data class Ready(
        val deviceInfo: PtpDeviceInfo,
        val usbDeviceSerial: String?,
        val usbConnectionSerial: String?,
        val propertyDumps: List<PtpPropertyDump>,
        val candidateDumps: List<PtpPropertyDump>,
        val batteryPercent: Int?,
    ) : FujiPtpProbeResult {
        val bestSerial: String
            get() = usbConnectionSerial.orCleanBlank()
                ?: usbDeviceSerial.orCleanBlank()
                ?: deviceInfo.serialNumber.ifBlank { "" }
    }

    data class NotReady(val reason: String) : FujiPtpProbeResult
}

data class PtpPropertyDump(
    val code: Int,
    val responseCode: Int?,
    val payloadBytes: Int,
    val rawHex: String,
    val decoded: String,
    val error: String? = null,
) {
    val codeHex: String
        get() = "0x${code.toString(16).uppercase().padStart(4, '0')}"

    val responseHex: String
        get() = responseCode?.let { "0x${it.toString(16).uppercase().padStart(4, '0')}" } ?: "-"
}

class FujiPtpProbe(
    private val connectionFactory: UsbPtpConnection,
) {
    fun probe(device: UsbDevice): FujiPtpProbeResult {
        val usbDeviceSerial = readUsbDeviceSerial(device)
        val openConnection = connectionFactory.open(device)
            ?: return FujiPtpProbeResult.NotReady("Unable to open the camera's PTP USB interface.")

        openConnection.use { connection ->
            val usbConnectionSerial = readUsbConnectionSerial(connection)
            val opened = connection.openSession()
            if (!opened) {
                return FujiPtpProbeResult.NotReady("Camera rejected OpenSession.")
            }

            val deviceInfoTransaction = connection.executeCommand(PtpConstants.GET_DEVICE_INFO)
            val payload = deviceInfoTransaction.data?.payload
                ?: return FujiPtpProbeResult.NotReady("Camera did not return DeviceInfo.")

            if (!deviceInfoTransaction.isOk) {
                return FujiPtpProbeResult.NotReady("Camera returned an error for DeviceInfo.")
            }

            val deviceInfo = parseDeviceInfo(payload)
            Log.d(
                TAG,
                "PTP DeviceInfo: manufacturer=${deviceInfo.manufacturer.ifBlank { "<blank>" }}, " +
                    "model=${deviceInfo.model.ifBlank { "<blank>" }}, " +
                    "version=${deviceInfo.deviceVersion.ifBlank { "<blank>" }}, " +
                    "serial=${deviceInfo.serialNumber.ifBlank { "<blank>" }}, " +
                    "usbDeviceSerial=${usbDeviceSerial.orCleanBlank() ?: "<blank>"}, " +
                    "usbConnectionSerial=${usbConnectionSerial.orCleanBlank() ?: "<blank>"}, " +
                    "props=${deviceInfo.supportedDeviceProperties.size}",
            )
            val propertyDumps = dumpDeviceProperties(connection, deviceInfo.supportedDeviceProperties)
            val batteryPercent = parseBatteryPercent(propertyDumps)
            val candidateDumps = dumpDeviceProperties(
                connection = connection,
                propertyCodes = BATTERY_CANDIDATE_PROPERTIES.filterNot {
                    it in deviceInfo.supportedDeviceProperties
                },
                label = "Candidate",
            )
            connection.closeSession()

            return if (deviceInfo.supportsFujiRecipeSlots) {
                FujiPtpProbeResult.Ready(
                    deviceInfo = deviceInfo,
                    usbDeviceSerial = usbDeviceSerial,
                    usbConnectionSerial = usbConnectionSerial,
                    propertyDumps = propertyDumps,
                    candidateDumps = candidateDumps,
                    batteryPercent = batteryPercent,
                )
            } else {
                FujiPtpProbeResult.NotReady(
                    "Fuji recipe slots not available. On the camera go to MENU → SET UP → USB Setting and select \"USB RAW Conv. / Backup Restore\", then reconnect."
                )
            }
        }
    }

    private fun readUsbDeviceSerial(device: UsbDevice): String? =
        runCatching { device.serialNumber }.getOrNull().orCleanBlank()

    private fun readUsbConnectionSerial(connection: OpenPtpConnection): String? =
        runCatching { connection.connection.serial }.getOrNull().orCleanBlank()

    private fun dumpDeviceProperties(
        connection: OpenPtpConnection,
        propertyCodes: List<Int>,
        label: String = "Property",
    ): List<PtpPropertyDump> =
        propertyCodes.map { propertyCode ->
            val dump = runCatching {
                val transaction = connection.executeCommand(
                    code = PtpConstants.GET_DEVICE_PROP_VALUE,
                    params = listOf(propertyCode),
                    timeoutMs = PROPERTY_DUMP_TIMEOUT_MS,
                )
                val payload = transaction.data?.payload ?: ByteArray(0)
                PtpPropertyDump(
                    code = propertyCode,
                    responseCode = transaction.response.code,
                    payloadBytes = payload.size,
                    rawHex = hexDump(payload, maxBytes = 128),
                    decoded = decodeDebugValue(payload),
                )
            }.getOrElse { error ->
                PtpPropertyDump(
                    code = propertyCode,
                    responseCode = null,
                    payloadBytes = 0,
                    rawHex = "-",
                    decoded = "-",
                    error = error.message ?: error::class.java.simpleName,
                )
            }

            Log.d(
                TAG,
                "$label ${dump.codeHex}: response=${dump.responseHex}, " +
                    "bytes=${dump.payloadBytes}, decoded=${dump.decoded}, raw=${dump.rawHex}" +
                    (dump.error?.let { ", error=$it" } ?: ""),
            )
            dump
        }

    private fun decodeDebugValue(payload: ByteArray): String {
        if (payload.isEmpty()) return "<empty>"

        val ptpString = decodePtpStringIfLikely(payload)
        if (ptpString != null) return "ptpString=\"$ptpString\""

        val ascii = decodeAsciiIfUseful(payload)
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return when (payload.size) {
            Short.SIZE_BYTES -> {
                val signed = buffer.short.toInt()
                "u16=${signed and 0xFFFF}, i16=$signed" + ascii.orEmpty()
            }
            Int.SIZE_BYTES -> {
                val signed = buffer.int
                "u32=${signed.toLong() and 0xFFFFFFFFL}, i32=$signed" + ascii.orEmpty()
            }
            else -> ascii?.trimStart() ?: "${payload.size} raw bytes"
        }
    }

    private fun decodePtpStringIfLikely(payload: ByteArray): String? {
        val count = payload.first().toInt() and 0xFF
        if (count == 0 || payload.size != 1 + (count * Short.SIZE_BYTES)) return null
        return runCatching { parsePtpString(payload) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun decodeAsciiIfUseful(payload: ByteArray): String? {
        if (payload.size < 4) return null

        val chars = payload.map { byte ->
            val value = byte.toInt() and 0xFF
            when (value) {
                in 32..126 -> value.toChar()
                0 -> '.'
                else -> '.'
            }
        }.joinToString(separator = "")

        val printable = payload.count { byte ->
            val value = byte.toInt() and 0xFF
            value in 32..126
        }
        return if (printable >= payload.size / 2) " ascii=\"$chars\"" else null
    }

    private fun parseBatteryPercent(propertyDumps: List<PtpPropertyDump>): Int? =
        propertyDumps
            .firstOrNull { it.code == FUJI_BATTERY_STATUS }
            ?.decoded
            ?.substringAfter("ptpString=\"", missingDelimiterValue = "")
            ?.substringBefore('"')
            ?.substringBefore(',')
            ?.toIntOrNull()
            ?.takeIf { it in 0..100 }

    private companion object {
        const val TAG = "FujiPtpProbe"
        const val PROPERTY_DUMP_TIMEOUT_MS = 2_000
        const val FUJI_BATTERY_STATUS = 0xD36B

        val BATTERY_CANDIDATE_PROPERTIES = listOf(
            0x5001, // Standard PTP BatteryLevel.
            0xD10B,
            0xD10C,
            0xD218,
            0xD219,
            0xD242,
            0xD243,
        )
    }
}

private fun String?.orCleanBlank(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }
