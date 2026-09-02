package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.PtpDeviceInfo
import com.ilfforever.fujisync.data.ptp.parseDeviceInfo
import com.ilfforever.fujisync.data.ptp.ptpResponseName
import com.ilfforever.fujisync.data.ptp.PtpPropDesc
import com.ilfforever.fujisync.data.ptp.parsePropDesc
import com.ilfforever.fujisync.data.ptp.uint16Le
import kotlinx.coroutines.delay

// The "push to live / C0" path, recovered by decompiling FUJIFILM X RAW STUDIO.
//
// XSDK_SetCustomSettingParameter has two independent gates: a slot number 1..7 writes the stored
// C1-C7 block (0xD18C name/selector + 0xD18E..0xD1A4), and a separate flag writes the same values
// to a parallel set of live/current-shooting property codes. Both blocks read the identical source
// struct at identical offsets, which is what pairs them. A slot number of 0 skips the stored block
// and writes only the live state — the P/A/S/M "C0" case.
//
// See docs/research/xraw-studio-call-chain.md in the fujifilm-ptp-recipes repo. The pairing is
// confirmed from Fuji's own code but has never been exercised against a real camera; that is what
// this bench is for.

data class LiveProp(
    val label: String,
    val slotCode: Int,
    val liveCode: Int,
    val structOffset: Int,
    /** INT16 on the wire. Known from the slot-side mapping; GetDevicePropDesc is not needed. */
    val signed: Boolean = false,
) {
    val liveHex: String get() = "0x%04X".format(liveCode)
    val slotHex: String get() = "0x%04X".format(slotCode)
}

val LIVE_PROPS: List<LiveProp> = listOf(
    LiveProp("Image Size?", 0xD18E, 0xD1A5, 0x00),
    LiveProp("Image Quality / File Type?", 0xD18F, 0xD018, 0x04),
    LiveProp("Dynamic Range", 0xD190, 0xD007, 0x08),
    LiveProp("Wide D-Range / DR Priority", 0xD191, 0xD02E, 0x0C),
    LiveProp("Film Simulation", 0xD192, 0xD001, 0x10),
    LiveProp("Mono WC", 0xD193, 0xD104, 0x14, signed = true),
    LiveProp("Mono MG", 0xD194, 0xD031, 0x18, signed = true),
    LiveProp("Grain Effect", 0xD195, 0xD023, 0x1C),
    LiveProp("Color Chrome", 0xD196, 0xD029, 0x20),
    LiveProp("Color Chrome FX Blue", 0xD197, 0xD030, 0x24),
    LiveProp("Smooth Skin", 0xD198, 0xD189, 0x28),
    LiveProp("White Balance", 0xD199, 0x5005, 0x2C),
    LiveProp("WB Shift Red", 0xD19A, 0xD00B, 0x30, signed = true),
    LiveProp("WB Shift Blue", 0xD19B, 0xD00C, 0x34, signed = true),
    LiveProp("Color Temperature", 0xD19C, 0xD017, 0x38),
    LiveProp("Highlight Tone", 0xD19D, 0xD320, 0x3C, signed = true),
    LiveProp("Shadow Tone", 0xD19E, 0xD321, 0x40, signed = true),
    LiveProp("Color", 0xD19F, 0xD008, 0x44, signed = true),
    LiveProp("Sharpness", 0xD1A0, 0x5015, 0x48, signed = true),
    LiveProp("Noise Reduction", 0xD1A1, 0xD01C, 0x4C),
    LiveProp("Clarity", 0xD1A2, 0xD032, 0x50, signed = true),
    LiveProp("Lens Modulation Opt.", 0xD1A3, 0xD34D, 0x54),
    LiveProp("Color Space", 0xD1A4, 0xD00A, 0x58),
)

// Written only as the fallback when the first field's write is rejected with SDK error 0x2C: the
// value is decomposed via a 27-entry 3x9 table into a column index (0xD03A) and a row index
// (0xD03B). Read here for reference; the bench never writes them.
val DECOMPOSE_CODES: List<Pair<String, Int>> = listOf(
    "Decompose column" to 0xD03A,
    "Decompose row" to 0xD03B,
)

// Control group: property codes this app already reads successfully on this camera every time it
// loads a recipe. If these fail the same way the live codes do, the fault is in how the request is
// made, not in whether the camera has the property. Without a control, a failing read is ambiguous.
val CONTROL_CODES: List<Pair<String, Int>> = listOf(
    "CONTROL slot selector" to 0xD18C,
    "CONTROL film simulation (slot)" to 0xD192,
    "CONTROL clarity (slot)" to 0xD1A2,
    "CONTROL battery level (PTP standard)" to 0x5001,
)

data class LivePropReading(
    val prop: LiveProp,
    /** The camera listed this code in its own DeviceInfo supported-properties array. */
    val advertised: Boolean,
    val value: Int?,
    val valueResponse: Int?,
    val desc: PtpPropDesc?,
    val descResponse: Int?,
    val note: String,
) {
    val readable: Boolean get() = value != null
    val writable: Boolean get() = desc?.writable ?: advertised
    val excludedReason: String? get() = WRITE_EXCLUDED[prop.liveCode]
    val testable: Boolean get() = readable && excludedReason == null

    /** Value to write during a test, and whether the camera vouched for it being legal. */
    fun testTarget(): Pair<Int, Boolean>? {
        desc?.alternateValue()?.let { return it to true }
        return value?.let { (it + 1) to false }
    }
}

data class LiveReadResult(
    val cameraModel: String?,
    val deviceModel: String,
    val supportsPropDesc: Boolean,
    val descNote: String,
    val advertisedCount: Int,
    val advertisedCodes: List<Int>,
    val readings: List<LivePropReading>,
    val decompose: List<LivePropReading>,
    val controls: List<LivePropReading>,
    val opProbes: List<OperationProbe>,
    val durationMs: Long,
) {
    val advertisedHere: Int get() = readings.count { it.advertised }
    val readableCount: Int get() = readings.count { it.readable }
}

internal fun readValue(connection: OpenPtpConnection, code: Int, signed: Boolean): Pair<Int?, Int?> {
    val tx = runCatching {
        connection.executeCommand(PtpConstants.GET_DEVICE_PROP_VALUE, params = listOf(code))
    }.getOrNull() ?: return null to null
    val payload = tx.data?.payload ?: ByteArray(0)
    if (!tx.isOk || payload.size < 2) return null to tx.response.code
    val raw = (payload[0].toInt() and 0xFF) or ((payload[1].toInt() and 0xFF) shl 8)
    return (if (signed) raw.toShort().toInt() else raw) to tx.response.code
}

/**
 * Reads this setting's stored value from each of C1..C7. Those are values the camera itself is
 * holding, so every one is legal for this body — which makes them safe write-test targets even
 * when GetDevicePropDesc is unavailable.
 */
internal fun legalValuesFromSlots(connection: OpenPtpConnection, prop: LiveProp): List<Int> {
    if (prop.slotCode == 0) return emptyList()
    val seen = LinkedHashSet<Int>()
    for (slot in 1..7) {
        val sel = runCatching {
            connection.executeCommandWithData(
                code = PtpConstants.SET_DEVICE_PROP_VALUE,
                params = listOf(PtpConstants.FUJI_SLOT_SELECTOR),
                payload = uint16Le(slot),
            )
        }.getOrNull()
        if (sel?.isOk != true) continue
        readValue(connection, prop.slotCode, prop.signed).first?.let { seen.add(it) }
    }
    return seen.toList()
}

private fun readOne(
    connection: OpenPtpConnection,
    prop: LiveProp,
    advertised: Boolean,
    tryDesc: Boolean,
): LivePropReading {
    // GetDevicePropDesc is optional and not every Fuji body implements it, so it is only an
    // enrichment. The value read is what decides whether a property is really there.
    var desc: PtpPropDesc? = null
    var descResponse: Int? = null
    if (tryDesc) {
        val tx = runCatching {
            connection.executeCommand(PtpConstants.GET_DEVICE_PROP_DESC, params = listOf(prop.liveCode))
        }.getOrNull()
        descResponse = tx?.response?.code
        val payload = tx?.data?.payload
        if (tx?.isOk == true && payload != null && payload.isNotEmpty()) desc = parsePropDesc(payload)
    }

    val (value, valueResponse) = readValue(connection, prop.liveCode, prop.signed)

    val note = buildString {
        append(if (advertised) "advertised in DeviceInfo" else "NOT in DeviceInfo list")
        append(" · read ")
        append(valueResponse?.let { ptpResponseName(it) } ?: "no response")
        append(" · ")
        append(if (prop.signed) "INT16" else "UINT16")
        desc?.let { append(" · ${if (it.writable) "read/write" else "READ-ONLY"} · ${it.formSummary}") }
    }
    return LivePropReading(prop, advertised, value, valueResponse, desc, descResponse, note)
}

suspend fun runLiveSettingsRead(
    connection: OpenPtpConnection,
    cameraModel: String?,
): LiveReadResult {
    val started = System.currentTimeMillis()

    val info: PtpDeviceInfo? = runCatching {
        val tx = connection.executeCommand(PtpConstants.GET_DEVICE_INFO)
        tx.data?.payload?.let { parseDeviceInfo(it) }
    }.getOrNull()

    val props = info?.supportedDeviceProperties.orEmpty().toSet()
    val descAdvertised = info?.supportsOperation(PtpConstants.GET_DEVICE_PROP_DESC) ?: false

    // GetDevicePropDesc is optional and some bodies advertise it but reject every call. Probe it
    // once against a property known to exist rather than per-property, so one broken optional
    // operation does not read like 23 separate failures.
    val descProbe = runCatching {
        connection.executeCommand(PtpConstants.GET_DEVICE_PROP_DESC, params = listOf(PtpConstants.FUJI_SLOT_SELECTOR))
    }.getOrNull()
    val descWorks = descProbe?.isOk == true && (descProbe.data?.payload?.isNotEmpty() == true)
    val descNote = when {
        descWorks -> "works"
        descProbe != null -> "advertised but returns ${ptpResponseName(descProbe.response.code)} — legal ranges unavailable"
        else -> "no response"
    }
    val tryDesc = descWorks

    // Raw operation diagnostic first: shows exactly what the camera replies with.
    val opProbes = runOperationDiagnostic(connection)

    // Controls: establish whether the operations themselves work on this body.
    val controls = CONTROL_CODES.map { (label, code) ->
        readOne(connection, LiveProp(label, 0, code, -1), code in props, tryDesc)
    }
    val readings = LIVE_PROPS.map { readOne(connection, it, it.liveCode in props, tryDesc) }
    val decompose = DECOMPOSE_CODES.map { (label, code) ->
        readOne(connection, LiveProp(label, 0, code, -1), code in props, tryDesc)
    }

    return LiveReadResult(
        cameraModel = cameraModel,
        deviceModel = info?.model.orEmpty().ifBlank { cameraModel ?: "unknown" },
        supportsPropDesc = descAdvertised,
        descNote = descNote,
        advertisedCount = props.size,
        advertisedCodes = info?.supportedDeviceProperties.orEmpty(),
        readings = readings,
        decompose = decompose,
        controls = controls,
        opProbes = opProbes,
        durationMs = System.currentTimeMillis() - started,
    )
}
