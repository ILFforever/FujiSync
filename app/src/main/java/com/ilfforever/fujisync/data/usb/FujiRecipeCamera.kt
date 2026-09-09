package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.capability.CameraCapability
import com.ilfforever.fujisync.data.capability.RecipeWritePlanner
import com.ilfforever.fujisync.data.ptp.CameraPresetName
import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.decodeInt16Le
import com.ilfforever.fujisync.data.ptp.decodeUInt16Le
import com.ilfforever.fujisync.data.ptp.encodePtpString
import com.ilfforever.fujisync.data.ptp.parsePtpString
import com.ilfforever.fujisync.data.ptp.uint16Le
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.FujiFilmSimulation
import com.ilfforever.fujisync.domain.model.FujiPropertyCode
import com.ilfforever.fujisync.domain.model.RecipePreset
import kotlinx.coroutines.delay

class FujiRecipeCamera(
    private val connection: OpenPtpConnection,
    private val propertyWriteDelayMs: Long = 0L,
    /**
     * What the attached body accepts. Defaults to [CameraCapability.Unknown], which gates nothing —
     * so a caller that has not probed capability behaves exactly as before.
     */
    private val capability: CameraCapability = CameraCapability.Unknown,
) {
    suspend fun readPreset(slot: CameraSlot): RecipePreset {
        check(selectSlot(slot)) { "Failed to select slot ${slot.label}" }
        delay(SLOT_SWITCH_DELAY_MS)

        val name = readPresetName().ifBlank { slot.label }
        val properties = mutableMapOf<FujiPropertyCode, Int>()

        for (code in PtpConstants.PRESET_BLOCK_START..PtpConstants.PRESET_BLOCK_END) {
            var transaction = connection.executeCommand(
                code = PtpConstants.GET_DEVICE_PROP_VALUE,
                params = listOf(code),
            )
            if (!transaction.isOk) {
                for (retry in 1..2) {
                    delay(SLOT_SWITCH_DELAY_MS)
                    transaction = connection.executeCommand(
                        code = PtpConstants.GET_DEVICE_PROP_VALUE,
                        params = listOf(code),
                    )
                    if (transaction.isOk) break
                }
            }

            val property = FujiPropertyCode.fromCode(code)
            if (transaction.isOk && property != null) {
                val payload = transaction.data?.payload ?: ByteArray(0)
                val value = if (property.signed) decodeInt16Le(payload) else decodeUInt16Le(payload)
                if (value != null) properties[property] = value
            }
        }

        return RecipePreset(
            slot = slot,
            name = name,
            properties = properties,
        )
    }

    suspend fun writePresetName(slot: CameraSlot, name: String): Boolean {
        if (!selectSlot(slot)) return false
        delay(SLOT_SWITCH_DELAY_MS)

        val safe = CameraPresetName.sanitizeOrFallback(name, fallback = slot.label)
        val transaction = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_PRESET_NAME),
            payload = encodePtpString(safe),
        )
        return transaction.isOk
    }

    suspend fun writeFilmSimulation(slot: CameraSlot, filmSimulation: FujiFilmSimulation): Boolean {
        if (!selectSlot(slot)) return false

        connection.executeCommand(PtpConstants.GET_DEVICE_INFO)

        val transaction = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(FujiPropertyCode.FilmSimulation.code),
            payload = uint16Le(filmSimulation.protocolValue),
        )
        return transaction.isOk
    }

    suspend fun writePreset(preset: RecipePreset): WriteResult {
        // Refused before anything is sent — not even the slot selector. A blocked recipe must leave
        // the slot exactly as it was, because a half-written preset reads as a successful one.
        val plan = RecipeWritePlanner.plan(preset, capability)
        plan.block?.let { block ->
            return WriteResult(0, 0, 0, listOf(PropertyWriteOutcome.blocked(block)), blocked = true)
        }

        // Select target slot
        if (!selectSlot(preset.slot)) return WriteResult(0, 1, 0)
        delay(SLOT_SWITCH_DELAY_MS)

        // Refresh camera state before pushing (per §9.6)
        connection.executeCommand(code = PtpConstants.GET_DEVICE_INFO)

        val outcomes = mutableListOf<PropertyWriteOutcome>()

        plan.skipped.forEach { skip ->
            outcomes += PropertyWriteOutcome.skipped(skip.property, skip.value, skip.reason)
        }

        for (write in plan.writes) {
            var tx = connection.executeCommandWithData(
                code    = PtpConstants.SET_DEVICE_PROP_VALUE,
                params  = listOf(write.property.code),
                payload = uint16Le(write.value),
            )
            if (!tx.isOk) {
                for (retry in 1..2) {
                    if (propertyWriteDelayMs > 0) delay(propertyWriteDelayMs)
                    tx = connection.executeCommandWithData(
                        code    = PtpConstants.SET_DEVICE_PROP_VALUE,
                        params  = listOf(write.property.code),
                        payload = uint16Le(write.value),
                    )
                    if (tx.isOk) break
                }
            }
            outcomes += PropertyWriteOutcome.attempted(write, tx.isOk, tx.response.code)
            if (propertyWriteDelayMs > 0) delay(propertyWriteDelayMs)
        }

        // Name is always written last (§9.6 step 5)
        val safeName = CameraPresetName.sanitizeOrFallback(preset.name, fallback = preset.slot.label)
        val nameTx = connection.executeCommandWithData(
            code    = PtpConstants.SET_DEVICE_PROP_VALUE,
            params  = listOf(PtpConstants.FUJI_PRESET_NAME),
            payload = encodePtpString(safeName),
        )
        outcomes += PropertyWriteOutcome.name(safeName, nameTx.isOk, nameTx.response.code)

        return WriteResult(
            success = outcomes.count { it.status == WriteStatus.Written },
            failed = outcomes.count { it.status == WriteStatus.Failed },
            skipped = outcomes.count { it.status == WriteStatus.Skipped },
            outcomes = outcomes,
        )
    }

    /**
     * Counts kept for existing callers; [outcomes] carries the detail. Reporting a failure by
     * property code rather than as a bare total is what makes a rejection diagnosable — "2 failed"
     * cannot distinguish an unsupported setting from a value the body would not take.
     */
    data class WriteResult(
        val success: Int,
        val failed: Int,
        val skipped: Int,
        val outcomes: List<PropertyWriteOutcome> = emptyList(),
        /** Nothing was sent: the body cannot take this recipe's film simulation. */
        val blocked: Boolean = false,
    ) {
        val isOk: Boolean get() = failed == 0 && !blocked
    }

    private fun selectSlot(slot: CameraSlot): Boolean {
        val transaction = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_SLOT_SELECTOR),
            payload = uint16Le(slot.protocolValue),
        )
        return transaction.isOk
    }

    private fun readPresetName(): String {
        val transaction = connection.executeCommand(
            code = PtpConstants.GET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_PRESET_NAME),
        )
        return if (transaction.isOk) {
            parsePtpString(transaction.data?.payload ?: ByteArray(0))
        } else {
            ""
        }
    }

    private companion object {
        const val SLOT_SWITCH_DELAY_MS = 25L
    }
}
