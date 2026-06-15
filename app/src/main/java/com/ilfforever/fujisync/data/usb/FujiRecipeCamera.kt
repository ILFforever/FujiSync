package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.ptp.CameraPresetName
import com.ilfforever.fujisync.data.ptp.MONO_SIM_CODES
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

private val COLOR_ONLY_PROPS = setOf(
    FujiPropertyCode.ColorChrome,
    FujiPropertyCode.ColorChromeFxBlue,
    FujiPropertyCode.Color,
    FujiPropertyCode.WbShiftRed,
    FujiPropertyCode.WbShiftBlue,
)

class FujiRecipeCamera(
    private val connection: OpenPtpConnection,
    private val propertyWriteDelayMs: Long = 0L,
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
        var success = 0
        var failed  = 0
        var skipped = 0

        // Select target slot
        if (!selectSlot(preset.slot)) return WriteResult(0, 1, 0)
        delay(SLOT_SWITCH_DELAY_MS)

        // Refresh camera state before pushing (per §9.6)
        connection.executeCommand(code = PtpConstants.GET_DEVICE_INFO)

        // Suppression flags
        val filmSimValue = preset.properties[FujiPropertyCode.FilmSimulation]
        val isMono      = filmSimValue != null && filmSimValue in MONO_SIM_CODES
        val isColorTemp = preset.properties[FujiPropertyCode.WhiteBalance] == 0x8007
        val dRangePriority = preset.properties[FujiPropertyCode.DRangePriority] ?: 0

        // FilmSimulation must be written first so the camera accepts subsequent prop ranges
        val ordered = buildList {
            preset.properties[FujiPropertyCode.FilmSimulation]
                ?.let { add(FujiPropertyCode.FilmSimulation to it) }
            preset.properties
                .filter { (k, _) -> k != FujiPropertyCode.FilmSimulation }
                .forEach { (k, v) -> add(k to v) }
        }

        for ((prop, value) in ordered) {
            if (isMono && prop in COLOR_ONLY_PROPS) { skipped++; continue }
            if (!isColorTemp && prop == FujiPropertyCode.ColorTemperature) { skipped++; continue }
            if (dRangePriority != 0 && prop == FujiPropertyCode.DynamicRange) { skipped++; continue }

            var tx = connection.executeCommandWithData(
                code    = PtpConstants.SET_DEVICE_PROP_VALUE,
                params  = listOf(prop.code),
                payload = uint16Le(value),
            )
            if (!tx.isOk) {
                for (retry in 1..2) {
                    if (propertyWriteDelayMs > 0) delay(propertyWriteDelayMs)
                    tx = connection.executeCommandWithData(
                        code    = PtpConstants.SET_DEVICE_PROP_VALUE,
                        params  = listOf(prop.code),
                        payload = uint16Le(value),
                    )
                    if (tx.isOk) break
                }
            }
            if (tx.isOk) success++ else failed++
            if (propertyWriteDelayMs > 0) delay(propertyWriteDelayMs)
        }

        // Name is always written last (§9.6 step 5)
        val safeName = CameraPresetName.sanitizeOrFallback(preset.name, fallback = preset.slot.label)
        val nameTx = connection.executeCommandWithData(
            code    = PtpConstants.SET_DEVICE_PROP_VALUE,
            params  = listOf(PtpConstants.FUJI_PRESET_NAME),
            payload = encodePtpString(safeName),
        )
        if (nameTx.isOk) success++ else failed++

        return WriteResult(success, failed, skipped)
    }

    data class WriteResult(val success: Int, val failed: Int, val skipped: Int) {
        val isOk: Boolean get() = failed == 0
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
