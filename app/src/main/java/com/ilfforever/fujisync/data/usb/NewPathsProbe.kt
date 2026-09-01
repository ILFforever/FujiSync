package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.decodePtpString
import com.ilfforever.fujisync.data.ptp.hexDump
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Read-only diagnostic reads for Fuji vendor properties found by decompiling FUJIFILM X RAW
// STUDIO (Fuji's own official tethering app), not yet confirmed against a live camera. See
// https://github.com/ILFforever/fujifilm-ptp-recipes/blob/main/docs/research/xraw-studio-sdk.md
// for how these were found. Every read here is a plain PTP GetDevicePropValue (0x1015) — same
// mechanism as every other documented property — this just points it at codes nobody has
// checked against a real camera yet. No writes: we don't know these are even writable, and one
// of them (0xD235) returns a structured list, not a scalar.

data class PropProbeResult(
    val label: String,
    val propCode: Int,
    val internalCmd: Int,
    val ok: Boolean,
    val rawHex: String,
    val decoded: String,
    val note: String,
)

data class PropListEntry(
    val propCode: Int,
    val valueLength: Int,
    val valueHex: String,
)

data class NewPathsProbeResult(
    val cameraModel: String?,
    val scalarResults: List<PropProbeResult>,
    val propList: List<PropListEntry>,
    val propListRaw: PropProbeResult,
    val durationMs: Long,
)

private data class ScalarProbeSpec(
    val label: String,
    val propCode: Int,
    val internalCmd: Int,
    val note: String,
    val decode: (ByteArray) -> String,
)

private fun decodeAsString(payload: ByteArray): String =
    decodePtpString(payload) ?: "<undecodable as PTP string>"

// Byte 0-3 unpacking confirmed from X RAW STUDIO's decompiled CheckBatteryInfo (body/grip/grip2/
// body2, one raw byte each). Bytes 4-5 of the 6-byte response were not traced — shown as raw hex.
private fun decodeBatteryInfo(payload: ByteArray): String {
    if (payload.size < 4) return "<too short: ${payload.size} bytes>"
    val body = payload[0].toInt() and 0xFF
    val grip = payload[1].toInt() and 0xFF
    val grip2 = payload[2].toInt() and 0xFF
    val body2 = payload[3].toInt() and 0xFF
    val tail = if (payload.size > 4) " tail=${hexDump(payload.copyOfRange(4, payload.size))}" else ""
    return "body=$body grip=$grip grip2=$grip2 body2=$body2$tail"
}

private fun decodeBatteryRatio(payload: ByteArray): String {
    val s = decodePtpString(payload) ?: return "<undecodable as PTP string>"
    val parts = s.split(",")
    return if (parts.size == 4) {
        "body=${parts[0]}% grip=${parts[1]}% grip2=${parts[2]}% body2=${parts[3]}%"
    } else {
        s
    }
}

private val SCALAR_PROBES = listOf(
    ScalarProbeSpec(
        label = "Tether RAW Condition Code",
        propCode = 0xD186,
        internalCmd = 0x1386,
        note = "XSDK_GetTetherRAWConditionCode — gates whether USB-RAW mode / individual recipe properties are usable for this body.",
        decode = ::decodeAsString,
    ),
    ScalarProbeSpec(
        label = "Tether RAW Compatibility Code",
        propCode = 0xD187,
        internalCmd = 0x1387,
        note = "XSDK_GetTetherRawCompatibilityCode — broader body/model compatibility class.",
        decode = ::decodeAsString,
    ),
    ScalarProbeSpec(
        label = "IOP Code",
        propCode = 0xD184,
        internalCmd = 0x1385,
        note = "XSDK_GetIOPCode — stored per-shot in RAWSettings/ConversionProfile.PropertyGroup.IOPCode, not a pure session gate like the two above. \"IOP\" meaning unconfirmed.",
        decode = ::decodeAsString,
    ),
    ScalarProbeSpec(
        label = "Battery Info (raw)",
        propCode = 0xD36A,
        internalCmd = 0x1046,
        note = "Read inside XSDK_CheckBatteryInfo — raw per-battery level bytes.",
        decode = ::decodeBatteryInfo,
    ),
    ScalarProbeSpec(
        label = "Battery Ratio (%)",
        propCode = 0xD36B,
        internalCmd = 0x1046,
        note = "Read inside XSDK_CheckBatteryInfo — comma-separated percentage string.",
        decode = ::decodeBatteryRatio,
    ),
)

private fun probeOne(connection: OpenPtpConnection, spec: ScalarProbeSpec): PropProbeResult {
    val tx = runCatching {
        connection.executeCommand(PtpConstants.GET_DEVICE_PROP_VALUE, params = listOf(spec.propCode))
    }.getOrElse {
        return PropProbeResult(spec.label, spec.propCode, spec.internalCmd, false, "<exception>", it.message ?: "read failed", spec.note)
    }
    val payload = tx.data?.payload ?: ByteArray(0)
    val ok = tx.isOk && payload.isNotEmpty()
    val decoded = if (ok) runCatching { spec.decode(payload) }.getOrElse { "<decode error: ${it.message}>" } else "<no data, response=0x${tx.response.code.toString(16)}>"
    return PropProbeResult(spec.label, spec.propCode, spec.internalCmd, ok, hexDump(payload, maxBytes = 64), decoded, spec.note)
}

// 0xD235: uint16 recordCount, then per record uint32 valueLength + uint16 propCode + valueLength
// raw bytes. Found by decompiling XGFXAPI.dll's CCameraCommandCameraPropList response parser.
private fun parsePropList(payload: ByteArray): List<PropListEntry> {
    if (payload.size < 2) return emptyList()
    val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
    val count = buffer.short.toInt() and 0xFFFF
    val entries = mutableListOf<PropListEntry>()
    repeat(count) {
        if (buffer.remaining() < 6) return entries
        val len = buffer.int
        if (len < 0 || buffer.remaining() < 2 + len) return entries
        val propCode = buffer.short.toInt() and 0xFFFF
        val value = ByteArray(len)
        buffer.get(value)
        entries.add(PropListEntry(propCode, len, hexDump(value, maxBytes = 24)))
    }
    return entries
}

suspend fun runNewPathsProbe(connection: OpenPtpConnection, cameraModel: String?): NewPathsProbeResult {
    val started = System.currentTimeMillis()

    val scalarResults = SCALAR_PROBES.map { probeOne(connection, it) }

    val listTx = runCatching {
        connection.executeCommand(
            PtpConstants.GET_DEVICE_PROP_VALUE,
            params = listOf(0xD235),
            timeoutMs = PtpConstants.STANDARD_TIMEOUT_MS * 3,
        )
    }
    val listPayload = listTx.getOrNull()?.data?.payload ?: ByteArray(0)
    val listResponseCode = listTx.getOrNull()?.response?.code
    val listOk = listTx.getOrNull()?.isOk == true && listPayload.isNotEmpty()
    val propList = if (listOk) runCatching { parsePropList(listPayload) }.getOrElse { emptyList() } else emptyList()

    val listStatus = when {
        listTx.isFailure -> "<exception: ${listTx.exceptionOrNull()?.message}>"
        listOk -> "${propList.size} record(s) — see table below"
        listResponseCode != null -> "<PTP response 0x%04X, %d data bytes>".format(listResponseCode, listPayload.size)
        else -> "<no response>"
    }

    val propListRaw = PropProbeResult(
        label = "Live Property List",
        propCode = 0xD235,
        internalCmd = 0x1513,
        ok = listOk,
        rawHex = hexDump(listPayload, maxBytes = 64),
        decoded = listStatus,
        note = "XSDK_GetPropertyValue special-cases this code via CCameraCommandCameraPropList — a live, camera-reported dump of every vendor property it currently supports plus its value.",
    )

    return NewPathsProbeResult(
        cameraModel = cameraModel,
        scalarResults = scalarResults,
        propList = propList,
        propListRaw = propListRaw,
        durationMs = System.currentTimeMillis() - started,
    )
}
