package com.ilfforever.fujisync.ui.model

import com.ilfforever.fujisync.data.capability.CameraCapability
import com.ilfforever.fujisync.data.capability.CapabilitySource
import com.ilfforever.fujisync.data.capability.KeyOrigin
import com.ilfforever.fujisync.data.capability.ValueSet
import com.ilfforever.fujisync.domain.model.FujiPropertyCode

/**
 * A body's capability rendered for the camera detail sheet: what it can do, what it cannot, and
 * how confident the app is about either.
 *
 * The provenance line matters as much as the facts. Where the answer comes from Fuji's shipped
 * compatibility data rather than from the camera, the app says so, because that data describes
 * X RAW STUDIO's feature set and has never been checked against camera firmware on anything but
 * X-Trans V.
 */
data class CapabilityProfile(
    val deviceKey: String?,
    val keyNote: String,
    val configName: String?,
    val facts: List<CapabilityFact>,
    /** Settings the camera itself did not list. */
    val missing: List<String>,
    /** Settings only Fuji's table doubts. Shown separately because they are weaker evidence. */
    val doubtful: List<String>,
) {
    val isKnown: Boolean get() = deviceKey != null || facts.isNotEmpty()
}

data class CapabilityFact(val label: String, val value: String)

fun capabilityProfile(capability: CameraCapability): CapabilityProfile {
    val facts = buildList {
        filmSimulationFact(capability)?.let { add(it) }
        toneStepFact(capability)?.let { add(it) }
        whiteBalanceFact(capability)?.let { add(it) }
        kelvinFact(capability)?.let { add(it) }
        grainFact(capability)?.let { add(it) }
    }

    return CapabilityProfile(
        deviceKey = capability.deviceKey,
        keyNote = when (capability.keyOrigin) {
            KeyOrigin.ReportedByCamera -> "reported by the camera"
            // Fuji's own client does exactly this for bodies that do not answer the identity read.
            KeyOrigin.SynthesizedFromModel -> "assumed — this body does not report one"
            KeyOrigin.None -> "unknown"
        },
        configName = capability.configName,
        facts = facts,
        missing = capability.blocked().map { it.displayName },
        doubtful = capability.doubtful().map { it.displayName },
    )
}

private fun CameraCapability.enumerated(property: FujiPropertyCode): List<Int>? =
    (of(property).values as? ValueSet.Enumerated)?.values?.takeIf { it.isNotEmpty() }

private fun filmSimulationFact(capability: CameraCapability): CapabilityFact? {
    val values = capability.enumerated(FujiPropertyCode.FilmSimulation) ?: return null
    val top = values.max()
    val newest = com.ilfforever.fujisync.domain.model.FujiFilmSimulation.entries
        .firstOrNull { it.protocolValue == top }
        ?.label
    return CapabilityFact(
        "Film simulations",
        if (newest != null) "${values.size} · up to $newest" else "${values.size}",
    )
}

private fun toneStepFact(capability: CameraCapability): CapabilityFact? {
    val values = capability.enumerated(FujiPropertyCode.HighlightTone) ?: return null
    // Wire values are the dial x 10, so a 5 in the set is a half step on the dial.
    val hasHalfSteps = values.any { it % 10 != 0 }
    return CapabilityFact(
        "Highlight / Shadow",
        if (hasHalfSteps) "half steps" else "whole steps only",
    )
}

private fun whiteBalanceFact(capability: CameraCapability): CapabilityFact? {
    val values = capability.enumerated(FujiPropertyCode.WhiteBalance) ?: return null
    val hasAutoPriority = values.any { it == 0x8020 || it == 0x8021 }
    return CapabilityFact(
        "White balance",
        "${values.size} modes" + if (hasAutoPriority) " · incl. auto priority" else "",
    )
}

private fun kelvinFact(capability: CameraCapability): CapabilityFact? =
    when (val values = capability.of(FujiPropertyCode.ColorTemperature).values) {
        is ValueSet.Range -> CapabilityFact(
            "Colour temperature",
            "${values.min}–${values.max}K in ${values.step}K steps",
        )

        is ValueSet.Enumerated -> CapabilityFact(
            "Colour temperature",
            "${values.values.size} fixed steps",
        )

        ValueSet.Unknown -> null
    }

private fun grainFact(capability: CameraCapability): CapabilityFact? =
    when (capability.grainSizeSupported) {
        true -> CapabilityFact("Grain", "strength and size")
        false -> CapabilityFact("Grain", "strength only — no size")
        null -> null
    }

/** Whether any of this profile came from the camera rather than from Fuji's shipped table. */
fun CameraCapability.hasCameraEvidence(): Boolean =
    properties.values.any {
        it.supportSource == CapabilitySource.Camera || it.valuesSource == CapabilitySource.Camera
    }
