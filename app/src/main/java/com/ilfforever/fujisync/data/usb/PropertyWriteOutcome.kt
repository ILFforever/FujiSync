package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.capability.Adjustment
import com.ilfforever.fujisync.data.capability.PlannedWrite
import com.ilfforever.fujisync.data.capability.WriteBlock
import com.ilfforever.fujisync.data.capability.WriteSkipReason
import com.ilfforever.fujisync.data.ptp.ptpResponseName
import com.ilfforever.fujisync.domain.model.FujiPropertyCode

/**
 * What happened to one property during a slot write.
 *
 * The reason this exists rather than a success/failure tally: a rejection and an absent setting
 * look identical in a count, and on Fuji they even share a response code — Dynamic Range Priority
 * makes the tone dials answer `InvalidDevicePropValue`, the same code an out-of-range value gives.
 * Recording the property, the value and the camera's own response is what makes the difference
 * legible afterwards, in the app and in a bug report.
 */
data class PropertyWriteOutcome(
    /** Null for the preset name, which is written by string rather than as a recipe property. */
    val property: FujiPropertyCode?,
    val label: String,
    val value: Int?,
    val status: WriteStatus,
    val skipReason: WriteSkipReason? = null,
    val adjustment: Adjustment? = null,
    val responseCode: Int? = null,
) {
    val responseName: String? get() = responseCode?.let { ptpResponseName(it) }

    /** One line fit for a log, a dev screen, or a report pasted into an issue. */
    val summary: String
        get() = buildString {
            append(label)
            value?.let { append(" = ").append(it) }
            append(" · ")
            when (status) {
                WriteStatus.Written -> {
                    append("written")
                    adjustment?.let { append(" (adjusted from ${it.from})") }
                }

                WriteStatus.Skipped -> append("skipped — ${skipReason?.description ?: "no reason given"}")
                WriteStatus.Failed -> append("failed — ${responseName ?: "no response"}")
                WriteStatus.Blocked -> append("blocked — this camera does not have this film simulation")
            }
        }

    companion object {
        fun attempted(write: PlannedWrite, ok: Boolean, responseCode: Int?) = PropertyWriteOutcome(
            property = write.property,
            label = write.property.displayName,
            value = write.value,
            status = if (ok) WriteStatus.Written else WriteStatus.Failed,
            adjustment = write.adjustment,
            responseCode = responseCode,
        )

        fun skipped(property: FujiPropertyCode, value: Int, reason: WriteSkipReason) =
            PropertyWriteOutcome(
                property = property,
                label = property.displayName,
                value = value,
                status = WriteStatus.Skipped,
                skipReason = reason,
            )

        /** The whole recipe was refused before anything was sent. */
        fun blocked(block: WriteBlock) = PropertyWriteOutcome(
            property = FujiPropertyCode.FilmSimulation,
            label = FujiPropertyCode.FilmSimulation.displayName,
            value = block.filmSimulationValue,
            status = WriteStatus.Blocked,
        )

        fun name(name: String, ok: Boolean, responseCode: Int?) = PropertyWriteOutcome(
            property = null,
            label = "Preset name \"$name\"",
            value = null,
            status = if (ok) WriteStatus.Written else WriteStatus.Failed,
            responseCode = responseCode,
        )
    }
}

enum class WriteStatus {
    Written,
    Skipped,
    Failed,

    /** The recipe was refused before anything was sent, so the slot is untouched. */
    Blocked,
}

/** Plain-language reason, phrased for a photographer rather than for a protocol log. */
val WriteSkipReason.description: String
    get() = when (this) {
        WriteSkipReason.MonochromeSimulation -> "colour setting, and the film simulation is monochrome"
        WriteSkipReason.ColorSimulation -> "monochrome toning, and the film simulation is in colour"
        WriteSkipReason.WhiteBalanceNotKelvin -> "White Balance is not set to Colour Temperature"
        WriteSkipReason.DynamicRangePriorityActive -> "D Range Priority controls this while it is on"
        WriteSkipReason.NotOnThisBody -> "this camera does not have this setting"
    }
