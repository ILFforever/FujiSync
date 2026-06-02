package com.paeki.fujirecipes.data.usb

import com.paeki.fujirecipes.data.ptp.CameraPresetName
import com.paeki.fujirecipes.data.ptp.MONO_SIM_CODES
import com.paeki.fujirecipes.data.ptp.PtpConstants
import com.paeki.fujirecipes.data.ptp.encodePtpString
import com.paeki.fujirecipes.data.ptp.uint16Le
import com.paeki.fujirecipes.domain.model.FujiPropertyCode
import com.paeki.fujirecipes.domain.model.RecipePreset
import kotlinx.coroutines.delay

data class DelayBenchResult(
    val delayMs: Long,
    val success: Int,
    val failed: Int,
    val durationMs: Long,
) {
    val total: Int get() = success + failed
}

val BENCH_DELAY_CANDIDATES: List<Long> = listOf(50L, 75L, 100L, 125L, 150L)

private val COLOR_ONLY_PROPS = setOf(
    FujiPropertyCode.ColorChrome,
    FujiPropertyCode.ColorChromeFxBlue,
    FujiPropertyCode.Color,
    FujiPropertyCode.WbShiftRed,
    FujiPropertyCode.WbShiftBlue,
)

// Writes all presets back to the camera once per candidate delay and counts errors.
// Uses the same write logic as FujiRecipeCamera.writePreset so failure rates are realistic.
// Each preset's data is unchanged — this is a timing probe, not a recipe edit.
suspend fun benchWriteDelay(
    connection: OpenPtpConnection,
    presets: List<RecipePreset>,
    candidates: List<Long> = BENCH_DELAY_CANDIDATES,
    onProgress: (phase: String) -> Unit = {},
): List<DelayBenchResult> = candidates.map { delayMs ->
    onProgress("Testing ${delayMs}ms…")
    val started = System.currentTimeMillis()
    var success = 0
    var failed = 0

    for (preset in presets) {
        val slotTx = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_SLOT_SELECTOR),
            payload = uint16Le(preset.slot.protocolValue),
        )
        if (!slotTx.isOk) { failed++; continue }
        delay(SLOT_SWITCH_DELAY_MS)

        connection.executeCommand(PtpConstants.GET_DEVICE_INFO)

        val filmSimValue = preset.properties[FujiPropertyCode.FilmSimulation]
        val isMono = filmSimValue != null && filmSimValue in MONO_SIM_CODES
        val isColorTemp = preset.properties[FujiPropertyCode.WhiteBalance] == 0x8007

        val ordered = buildList {
            preset.properties[FujiPropertyCode.FilmSimulation]
                ?.let { add(FujiPropertyCode.FilmSimulation to it) }
            preset.properties
                .filter { (k, _) -> k != FujiPropertyCode.FilmSimulation }
                .forEach { (k, v) -> add(k to v) }
        }

        for ((prop, value) in ordered) {
            if (isMono && prop in COLOR_ONLY_PROPS) continue
            if (!isColorTemp && prop == FujiPropertyCode.ColorTemperature) continue

            val tx = connection.executeCommandWithData(
                code = PtpConstants.SET_DEVICE_PROP_VALUE,
                params = listOf(prop.code),
                payload = uint16Le(value),
            )
            if (tx.isOk) success++ else failed++
            delay(delayMs)
        }

        val safeName = CameraPresetName.sanitizeOrFallback(preset.name, fallback = preset.slot.label)
        val nameTx = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_PRESET_NAME),
            payload = encodePtpString(safeName),
        )
        if (nameTx.isOk) success++ else failed++
    }

    DelayBenchResult(
        delayMs = delayMs,
        success = success,
        failed = failed,
        durationMs = System.currentTimeMillis() - started,
    )
}

private const val SLOT_SWITCH_DELAY_MS = 100L
