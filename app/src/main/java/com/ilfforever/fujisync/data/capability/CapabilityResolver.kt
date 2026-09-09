package com.ilfforever.fujisync.data.capability

import com.ilfforever.fujisync.data.ptp.PtpPropDesc
import com.ilfforever.fujisync.data.ptp.PtpPropForm
import com.ilfforever.fujisync.domain.model.FujiPropertyCode

/**
 * Merges everything known about a connected body into one [CameraCapability].
 *
 * Three sources, in decreasing order of trust:
 *
 *  1. **DeviceInfo's supported-property list** — the camera's own statement of which property codes
 *     it has. Authoritative, and the only thing allowed to block a write.
 *  2. **GetDevicePropDesc** — exact legal values straight from the body. Optional: several Fuji
 *     bodies advertise the operation and then refuse every call, so it is pure enrichment and its
 *     absence costs nothing.
 *  3. **Fuji's XRFC capability table** — the only source that knows *value* limits per generation
 *     (film-simulation ceiling, tone half-steps, the two extra WB modes, continuous vs listed
 *     Kelvin, whether a grain size axis exists). Client-side data, so it informs and warns but
 *     never blocks.
 *
 * Pure and synchronous — every input is already-read data, which keeps it unit-testable without a
 * camera.
 */
object CapabilityResolver {

    /** Recipe properties this app reads and writes: 0xD190..0xD1A2. */
    private val RECIPE_CODES: Set<Int> = FujiPropertyCode.entries.map { it.code }.toSet()

    fun resolve(
        deviceKey: String?,
        keyOrigin: KeyOrigin,
        table: TableMatch?,
        tableVersion: String?,
        advertisedProperties: Collection<Int>,
        descriptors: Map<Int, PtpPropDesc> = emptyMap(),
    ): CameraCapability {
        val advertised = advertisedProperties.toSet()

        // A body that lists none of the recipe block is not enumerating vendor properties at all —
        // treating that as "every property is missing" would block every write on a camera that
        // works fine. In that case DeviceInfo is ignored rather than believed.
        val deviceInfoUsable = advertised.any { it in RECIPE_CODES }

        val properties = FujiPropertyCode.entries.associateWith { property ->
            resolveOne(
                property = property,
                deviceInfoUsable = deviceInfoUsable,
                advertised = advertised,
                descriptor = descriptors[property.code],
                tableProperty = table?.properties?.get(property),
            )
        }

        return CameraCapability(
            deviceKey = deviceKey,
            keyOrigin = keyOrigin,
            configName = table?.configName,
            tableVersion = tableVersion,
            properties = properties,
            grainSizeSupported = table?.grainSizeSupported,
        )
    }

    private fun resolveOne(
        property: FujiPropertyCode,
        deviceInfoUsable: Boolean,
        advertised: Set<Int>,
        descriptor: PtpPropDesc?,
        tableProperty: TableProperty?,
    ): PropertyCapability {
        // The camera's own list decides support, in both directions, and the table only fills in
        // where there is no list to consult.
        //
        // It is tempting to let the table veto an advertised code. That was tried and is wrong,
        // because **the table's flags name features while the block names property codes, and the
        // two are not one to one.** A code outlives the feature it was introduced for: `0xD193`'s
        // field name is `lBlackImageTone`, the single-axis monochrome toning of the X-T3 era, and
        // the flag of that name is true on exactly the four configurations that had that feature.
        // On later bodies the same code carries the warm/cool axis of its successor, Monochromatic
        // Color — confirmed by reading an X-H2 that had been set by hand. So `BlackImageTone =
        // false` is a true statement about an X-H2 and still tells you nothing about whether
        // `0xD193` works there.
        //
        // The table is not unreliable; mapping a feature flag onto a property code is.
        val (support, supportSource) = when {
            deviceInfoUsable && property.code in advertised ->
                Support.Present to CapabilitySource.Camera

            deviceInfoUsable ->
                Support.Absent to CapabilitySource.Camera

            tableProperty != null ->
                (if (tableProperty.supported) Support.Present else Support.Absent) to
                    CapabilitySource.FujiTable

            else -> Support.Unknown to CapabilitySource.Unknown
        }

        // GetDevicePropDesc also reports a get/set flag, but it is deliberately not used to block:
        // Dynamic Range and the tone dials legitimately go read-only while Dynamic Range Priority
        // is active, so a read-only flag can describe a temporary interlock rather than a missing
        // feature. The interlock is handled explicitly in the write path instead.
        val descValues = descriptor?.toValueSet() ?: ValueSet.Unknown
        val (values, valuesSource) = when {
            descValues != ValueSet.Unknown -> descValues to CapabilitySource.Camera
            tableProperty != null && tableProperty.values != ValueSet.Unknown ->
                tableProperty.values to CapabilitySource.FujiTable

            else -> ValueSet.Unknown to CapabilitySource.Unknown
        }

        return PropertyCapability(
            property = property,
            support = support,
            supportSource = supportSource,
            values = values,
            valuesSource = valuesSource,
            variant = tableProperty?.variant,
        )
    }

    private fun PtpPropDesc.toValueSet(): ValueSet = when (form) {
        PtpPropForm.Enum -> if (enumValues.isEmpty()) ValueSet.Unknown else ValueSet.Enumerated(enumValues)
        PtpPropForm.Range -> {
            val low = min
            val high = max
            if (low == null || high == null || low > high) {
                ValueSet.Unknown
            } else {
                ValueSet.Range(low, high, (step ?: 1).coerceAtLeast(1))
            }
        }

        PtpPropForm.None -> ValueSet.Unknown
    }
}
