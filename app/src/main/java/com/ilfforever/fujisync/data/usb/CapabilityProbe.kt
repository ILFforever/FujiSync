package com.ilfforever.fujisync.data.usb

import android.util.Log
import com.ilfforever.fujisync.data.capability.CameraCapability
import com.ilfforever.fujisync.data.capability.CameraDeviceKey
import com.ilfforever.fujisync.data.capability.CapabilityResolver
import com.ilfforever.fujisync.data.capability.KeyOrigin
import com.ilfforever.fujisync.data.capability.XrfcCapabilityTable
import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.PtpDeviceInfo
import com.ilfforever.fujisync.data.ptp.PtpPropDesc
import com.ilfforever.fujisync.data.ptp.decodePtpString
import com.ilfforever.fujisync.data.ptp.parsePropDesc
import com.ilfforever.fujisync.domain.model.FujiPropertyCode

// Reads everything the camera can tell us about its own capabilities, in one already-open session.
//
// Two vendor properties carry the body's identity: 0xD186 (tether RAW condition code) and 0xD187
// (tether RAW compatibility code). Both return a string of the form "<Model>_<FirmwareGeneration>",
// e.g. "X-H2_0200", confirmed on hardware. The camera returns no verdict of its own — it states who
// it is, and the client looks that identity up. Older bodies fail the read, and Fuji's own client
// then synthesises "<model>_0100"; this does the same.

/** Identity strings as read from the camera, kept raw so a dev screen can show exactly what came back. */
data class CameraIdentity(
    val conditionCode: String?,
    val compatibilityCode: String?,
    val deviceKey: String?,
    val keyOrigin: KeyOrigin,
) {
    companion object {
        val Unknown = CameraIdentity(null, null, null, KeyOrigin.None)
    }
}

private const val TAG = "CapabilityProbe"
private const val TETHER_CONDITION_CODE = 0xD186
private const val TETHER_COMPATIBILITY_CODE = 0xD187

/**
 * The properties whose legal values differ by camera generation. Only these are worth a
 * GetDevicePropDesc round-trip: for the rest the table records support, not values, so a descriptor
 * would add nothing and connecting should stay fast.
 */
private val GENERATION_BOUND_CODES = listOf(
    FujiPropertyCode.FilmSimulation,
    FujiPropertyCode.GrainEffect,
    FujiPropertyCode.WhiteBalance,
    FujiPropertyCode.ColorTemperature,
    FujiPropertyCode.HighlightTone,
    FujiPropertyCode.ShadowTone,
    FujiPropertyCode.HighIsoNr,
)

private fun readString(connection: OpenPtpConnection, code: Int): String? = runCatching {
    val tx = connection.executeCommand(PtpConstants.GET_DEVICE_PROP_VALUE, params = listOf(code))
    if (!tx.isOk) return@runCatching null
    tx.data?.payload?.let { decodePtpString(it) }?.takeIf { it.isNotBlank() }
}.getOrNull()

fun readCameraIdentity(connection: OpenPtpConnection, model: String?): CameraIdentity {
    val condition = readString(connection, TETHER_CONDITION_CODE)
    val compatibility = readString(connection, TETHER_COMPATIBILITY_CODE)

    val reported = CameraDeviceKey.fromReported(condition)
        ?: CameraDeviceKey.fromReported(compatibility)

    return if (reported != null) {
        CameraIdentity(condition, compatibility, reported, KeyOrigin.ReportedByCamera)
    } else {
        val synthesized = CameraDeviceKey.synthesize(model)
        CameraIdentity(
            conditionCode = condition,
            compatibilityCode = compatibility,
            deviceKey = synthesized,
            keyOrigin = if (synthesized != null) KeyOrigin.SynthesizedFromModel else KeyOrigin.None,
        )
    }
}

/**
 * Fetches legal value sets straight from the camera where it will answer.
 *
 * GetDevicePropDesc is optional in PTP and some Fuji bodies advertise it but reject every call, so
 * it is probed once against a property known to exist. If that probe fails, no further descriptor
 * calls are made — one unsupported operation should cost one round-trip, not seven.
 */
private fun readDescriptors(connection: OpenPtpConnection): Map<Int, PtpPropDesc> {
    val probe = runCatching {
        connection.executeCommand(
            PtpConstants.GET_DEVICE_PROP_DESC,
            params = listOf(PtpConstants.FUJI_SLOT_SELECTOR),
        )
    }.getOrNull()

    val works = probe?.isOk == true && probe.data?.payload?.isNotEmpty() == true
    if (!works) return emptyMap()

    return GENERATION_BOUND_CODES.mapNotNull { property ->
        val desc = runCatching {
            val tx = connection.executeCommand(
                PtpConstants.GET_DEVICE_PROP_DESC,
                params = listOf(property.code),
            )
            if (tx.isOk) tx.data?.payload?.let { parsePropDesc(it) } else null
        }.getOrNull()
        desc?.let { property.code to it }
    }.toMap()
}

/**
 * Builds the full capability picture for a connected body. Runs inside an existing PTP session and
 * costs two property reads plus, only where the camera supports it, a handful of descriptor reads.
 */
fun probeCameraCapability(
    connection: OpenPtpConnection,
    deviceInfo: PtpDeviceInfo,
    table: XrfcCapabilityTable,
): Pair<CameraIdentity, CameraCapability> {
    val identity = readCameraIdentity(connection, deviceInfo.model)
    val match = table.lookup(identity.deviceKey)
    val descriptors = readDescriptors(connection)

    val capability = CapabilityResolver.resolve(
        deviceKey = identity.deviceKey,
        keyOrigin = identity.keyOrigin,
        table = match,
        tableVersion = table.version,
        advertisedProperties = deviceInfo.supportedDeviceProperties,
        descriptors = descriptors,
    )

    Log.d(
        TAG,
        "identity=${identity.deviceKey ?: "<none>"} (${identity.keyOrigin}) " +
            "config=${match?.configName ?: "<unmatched>"}" +
            (if (match?.exact == false) " (fell back from ${match.requestedKey})" else "") +
            " descriptors=${descriptors.size} blocked=${capability.blocked().size}",
    )

    return identity to capability
}
