package com.ilfforever.fujisync.data.capability

import com.ilfforever.fujisync.domain.model.FujiPropertyCode

/**
 * What a specific camera body accepts for each recipe property.
 *
 * Assembled from three sources of decreasing trust — see [CapabilitySource]. The distinction
 * matters and is deliberately carried all the way into the UI: Fuji's own capability table is
 * strong evidence about a body, but it describes what X RAW STUDIO offers, not what the camera
 * firmware accepts on the wire. The project's protocol notes are explicit that it is "a risk
 * ranking, not a compatibility guarantee".
 *
 * The rule that follows from that, and which the write path implements:
 * **only the camera's own word ([Support.Absent] from DeviceInfo) blocks a write.** Everything the
 * table alone objects to becomes a warning, and the write is still attempted so the camera gets the
 * final say.
 */
data class CameraCapability(
    /** Fuji's `<Model>_<FirmwareGeneration>` key, e.g. `X-H2_0200`. Null when it could not be built. */
    val deviceKey: String?,
    val keyOrigin: KeyOrigin,
    /** The `PropertyGroup` this body resolved to, e.g. `X-H2Config2`. Null when unmatched. */
    val configName: String?,
    val tableVersion: String?,
    val properties: Map<FujiPropertyCode, PropertyCapability>,
    /** Whether the body has a grain *size* axis. Null when unknown. See [XrfcCapabilityTable]. */
    val grainSizeSupported: Boolean?,
) {
    /** True when nothing at all is known — no table match and no camera-reported property list. */
    val isEmpty: Boolean
        get() = properties.values.all { it.support == Support.Unknown && it.values == ValueSet.Unknown }

    fun of(property: FujiPropertyCode): PropertyCapability =
        properties[property] ?: PropertyCapability.unknown(property)

    /** The camera itself said this property does not exist. The only condition that blocks a write. */
    fun isBlocked(property: FujiPropertyCode): Boolean = of(property).isBlocked

    /** Properties the camera reported as missing, in protocol order. */
    fun blocked(): List<FujiPropertyCode> =
        FujiPropertyCode.entries.filter { isBlocked(it) }

    /** Properties Fuji's table says this body lacks, but the camera has not denied. */
    fun doubtful(): List<FujiPropertyCode> =
        FujiPropertyCode.entries.filter { of(it).isDoubtful }

    /**
     * Widens what this body is known to accept using values it is already holding.
     *
     * Every film simulation sitting in C1–C7 is proof by demonstration: the camera stored it, so
     * the camera takes it. That can only ever *raise* the ceiling, never lower it — a body can
     * support Reala Ace with no slot using it, so absence proves nothing. It exists to stop a body
     * on firmware newer than Fuji's table being blocked from a simulation it plainly supports.
     *
     * Sound because the simulation numbering is a strictly nested chain: if the highest value seen
     * is *n*, every value below it is legal too.
     */
    fun withObservedFilmSimulations(observed: Collection<Int>): CameraCapability {
        val highest = observed.filter { it > 0 }.maxOrNull() ?: return this
        val current = of(FujiPropertyCode.FilmSimulation)

        val widened = when (val values = current.values) {
            is ValueSet.Enumerated -> {
                if (values.values.contains(highest)) return this
                ValueSet.Enumerated((values.values + (1..highest)).distinct().sorted())
            }
            // Nothing to widen against: with no known ceiling nothing was being blocked anyway.
            is ValueSet.Range, ValueSet.Unknown -> return this
        }

        return copy(
            properties = properties + (
                FujiPropertyCode.FilmSimulation to current.copy(
                    // The camera demonstrated these, so this is now the camera's own word.
                    support = Support.Present,
                    supportSource = CapabilitySource.Camera,
                    values = widened,
                    valuesSource = CapabilitySource.Camera,
                )
                ),
        )
    }

    companion object {
        /** Used before a camera is seen, and for editing with no target body. Gates nothing. */
        val Unknown = CameraCapability(
            deviceKey = null,
            keyOrigin = KeyOrigin.None,
            configName = null,
            tableVersion = null,
            properties = emptyMap(),
            grainSizeSupported = null,
        )
    }
}

/** How the `<Model>_<Firmware>` lookup key was obtained. */
enum class KeyOrigin {
    /** Read from `0xD186` / `0xD187`. Carries the real firmware generation. */
    ReportedByCamera,

    /**
     * Built as `<model>_0100` because the identity read failed. This mirrors what Fuji's own
     * client does for pre-`0xD187` bodies (`XRFCClass::OpenUSB`), so it is the documented path
     * rather than a guess — but it cannot see firmware updates, so it may understate a body.
     */
    SynthesizedFromModel,

    /** No key at all. */
    None,
}

/** Where one piece of capability knowledge came from, most trustworthy first. */
enum class CapabilitySource {
    /** The camera answered for itself — DeviceInfo's property list, or GetDevicePropDesc. */
    Camera,

    /** Fuji's XRFC capability table, matched on the device key. Evidence, not proof. */
    FujiTable,

    /** Nothing known. */
    Unknown,
}

enum class Support { Present, Absent, Unknown }

data class PropertyCapability(
    val property: FujiPropertyCode,
    val support: Support,
    val supportSource: CapabilitySource,
    val values: ValueSet,
    val valuesSource: CapabilitySource,
    /** The XRFC encoding variant, e.g. `Std6`. Diagnostic only; never sent to the camera. */
    val variant: String? = null,
) {
    /** The camera itself denied this property. A write must not be attempted. */
    val isBlocked: Boolean
        get() = support == Support.Absent && supportSource == CapabilitySource.Camera

    /** Fuji's table says this body lacks the property, but the camera has not confirmed it. */
    val isDoubtful: Boolean
        get() = support == Support.Absent && supportSource == CapabilitySource.FujiTable

    /** True when [value] is known to be outside what this body accepts. Unknown ranges allow. */
    fun rejects(value: Int): Boolean = !values.allows(value)

    companion object {
        fun unknown(property: FujiPropertyCode) = PropertyCapability(
            property = property,
            support = Support.Unknown,
            supportSource = CapabilitySource.Unknown,
            values = ValueSet.Unknown,
            valuesSource = CapabilitySource.Unknown,
        )
    }
}

/** The legal wire values for one property on one body. */
sealed interface ValueSet {
    /** Whether [value] is permitted. [Unknown] permits everything — absence of evidence. */
    fun allows(value: Int): Boolean

    data class Enumerated(val values: List<Int>) : ValueSet {
        override fun allows(value: Int): Boolean = value in values
    }

    data class Range(val min: Int, val max: Int, val step: Int) : ValueSet {
        override fun allows(value: Int): Boolean =
            value in min..max && (step <= 1 || (value - min) % step == 0)

        /** Nearest legal value, for clamping an out-of-range dial instead of refusing it. */
        fun clamp(value: Int): Int {
            val bounded = value.coerceIn(min, max)
            if (step <= 1) return bounded
            val snapped = min + ((bounded - min) + step / 2) / step * step
            return snapped.coerceIn(min, max)
        }
    }

    data object Unknown : ValueSet {
        override fun allows(value: Int): Boolean = true
    }
}
