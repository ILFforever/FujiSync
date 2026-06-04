package com.paeki.fujirecipes.data.usb

import com.paeki.fujirecipes.data.ptp.CameraPresetName
import com.paeki.fujirecipes.data.ptp.MONO_SIM_CODES
import com.paeki.fujirecipes.data.ptp.PtpConstants
import com.paeki.fujirecipes.data.ptp.encodePtpString
import com.paeki.fujirecipes.data.ptp.uint16Le
import com.paeki.fujirecipes.domain.model.CameraSlot
import com.paeki.fujirecipes.domain.model.FujiPropertyCode
import com.paeki.fujirecipes.domain.model.RecipePreset
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.model.toPreset
import kotlinx.coroutines.delay

data class DelayBenchResult(
    val delayMs: Long,
    val success: Int,
    val failed: Int,
    val durationMs: Long,
    // null key = slot-selector or name write failure
    val failedProps: Map<FujiPropertyCode?, Int> = emptyMap(),
) {
    val total: Int get() = success + failed
}

val BENCH_DELAY_CANDIDATES: List<Long> = listOf(5L, 0L, 10L, 15L, 20L)

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
    val failedProps = mutableMapOf<FujiPropertyCode?, Int>()

    for (preset in presets) {
        val slotTx = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_SLOT_SELECTOR),
            payload = uint16Le(preset.slot.protocolValue),
        )
        if (!slotTx.isOk) {
            failed++
            failedProps[null] = (failedProps[null] ?: 0) + 1
            continue
        }
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
            if (tx.isOk) {
                success++
            } else {
                failed++
                failedProps[prop] = (failedProps[prop] ?: 0) + 1
            }
            if (delayMs > 0) delay(delayMs)
        }

        val safeName = CameraPresetName.sanitizeOrFallback(preset.name, fallback = preset.slot.label)
        val nameTx = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_PRESET_NAME),
            payload = encodePtpString(safeName),
        )
        if (nameTx.isOk) success++ else {
            failed++
            failedProps[null] = (failedProps[null] ?: 0) + 1
        }
    }

    DelayBenchResult(
        delayMs = delayMs,
        success = success,
        failed = failed,
        durationMs = System.currentTimeMillis() - started,
        failedProps = failedProps,
    )
}

private const val SLOT_SWITCH_DELAY_MS = 100L

// Builds a bench preset via the standard UI→preset mapper so all values are correctly encoded.
fun randomBenchPreset(slot: CameraSlot): RecipePreset = RecipeUiModel(
    slot        = slot.label,
    name        = "BENCH-${slot.label}",
    sim         = "Velvia / Vivid",
    pills       = emptyList(),
    effects     = mapOf(
        "Dynamic Range"        to "DR100%",
        "Grain Effect"         to "Off",
        "Color Chrome"         to "Off",
        "Color Chrome FX Blue" to "Off",
        "Smooth Skin"          to "Off",
    ),
    tone        = mapOf(
        "Highlight Tone" to "+1",
        "Shadow Tone"    to "0",
        "Color"          to "+2",
        "Sharpness"      to "0",
        "High ISO NR"    to "0",
        "Clarity"        to "0",
    ),
    wb          = mapOf(
        "White Balance" to "Auto",
        "WB Shift R"    to "+1",
        "WB Shift B"    to "0",
    ),
).toPreset(slot)
