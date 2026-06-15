package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.ptp.CameraPresetName
import com.ilfforever.fujisync.data.ptp.MONO_SIM_CODES
import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.encodePtpString
import com.ilfforever.fujisync.data.ptp.uint16Le
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.FujiPropertyCode
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.toPreset
import kotlinx.coroutines.delay

// 7 distinct presets — one per slot. Different film sim, grain, tone, and WB per slot so
// the bench can't pass by coincidence (slot already had those values).
// NOTE: film sim labels MUST exactly match FujiFilmSimulation.label — toPreset() silently
// falls back to Provia (1) on a miss, which would make the film-sim test pass without testing it.
fun benchRoundTripPreset(slot: CameraSlot) = when (slot) {
    CameraSlot.C1 -> RecipeUiModel(
        slot = slot.label, name = "RW-Reala", sim = "Reala Ace", pills = emptyList(),
        effects = mapOf("Dynamic Range" to "DR200%", "D Range Priority" to "Off", "Grain Effect" to "Weak Small", "Color Chrome" to "Weak", "Color Chrome FX Blue" to "Off", "Smooth Skin" to "Off"),
        tone = mapOf("Highlight Tone" to "-1.0", "Shadow Tone" to "+1.5", "Color" to "+2", "Sharpness" to "0", "High ISO NR" to "0", "Clarity" to "+1"),
        wb = mapOf("White Balance" to "Auto", "WB Shift R" to "+1", "WB Shift B" to "-2"),
    ).toPreset(slot)
    CameraSlot.C2 -> RecipeUiModel(
        slot = slot.label, name = "RW-Nostalgic", sim = "Nostalgic Neg", pills = emptyList(),
        effects = mapOf("Dynamic Range" to "DR400%", "D Range Priority" to "Off", "Grain Effect" to "Off", "Color Chrome" to "Strong", "Color Chrome FX Blue" to "Weak", "Smooth Skin" to "Off"),
        tone = mapOf("Highlight Tone" to "-2.0", "Shadow Tone" to "+2.0", "Color" to "+1", "Sharpness" to "-1", "High ISO NR" to "0", "Clarity" to "+2"),
        wb = mapOf("White Balance" to "Daylight", "WB Shift R" to "+2", "WB Shift B" to "0"),
    ).toPreset(slot)
    CameraSlot.C3 -> RecipeUiModel(
        slot = slot.label, name = "RW-Bleach", sim = "Eterna Bleach Bypass", pills = emptyList(),
        effects = mapOf("Dynamic Range" to "DR200%", "D Range Priority" to "Off", "Grain Effect" to "Strong Large", "Color Chrome" to "Off", "Color Chrome FX Blue" to "Off", "Smooth Skin" to "Weak"),
        tone = mapOf("Highlight Tone" to "+1.0", "Shadow Tone" to "-1.5", "Color" to "-2", "Sharpness" to "+2", "High ISO NR" to "0", "Clarity" to "+3"),
        wb = mapOf("White Balance" to "Shade", "WB Shift R" to "-1", "WB Shift B" to "+3"),
    ).toPreset(slot)
    // Mono sim — exercises the color-only skip path (ColorChrome/CCFXBlue/Color/WB shift skipped).
    CameraSlot.C4 -> RecipeUiModel(
        slot = slot.label, name = "RW-AcrosR", sim = "Acros + R", pills = emptyList(),
        effects = mapOf("Dynamic Range" to "DR100%", "D Range Priority" to "Off", "Grain Effect" to "Strong Small", "Color Chrome" to "Off", "Color Chrome FX Blue" to "Off", "Smooth Skin" to "Off"),
        tone = mapOf("Highlight Tone" to "+2.0", "Shadow Tone" to "-2.0", "Color" to "0", "Sharpness" to "+3", "High ISO NR" to "0", "Clarity" to "-1"),
        wb = mapOf("White Balance" to "Auto", "WB Shift R" to "0", "WB Shift B" to "0"),
    ).toPreset(slot)
    CameraSlot.C5 -> RecipeUiModel(
        slot = slot.label, name = "RW-ClassicNeg", sim = "Classic Neg", pills = emptyList(),
        effects = mapOf("Dynamic Range" to "DR100%", "D Range Priority" to "Off", "Grain Effect" to "Weak Large", "Color Chrome" to "Strong", "Color Chrome FX Blue" to "Strong", "Smooth Skin" to "Off"),
        tone = mapOf("Highlight Tone" to "-0.5", "Shadow Tone" to "+1.0", "Color" to "0", "Sharpness" to "-3", "High ISO NR" to "0", "Clarity" to "0"),
        wb = mapOf("White Balance" to "Incandescent", "WB Shift R" to "-2", "WB Shift B" to "+1"),
    ).toPreset(slot)
    CameraSlot.C6 -> RecipeUiModel(
        slot = slot.label, name = "RW-Velvia", sim = "Velvia / Vivid", pills = emptyList(),
        effects = mapOf("Dynamic Range" to "DR400%", "D Range Priority" to "Off", "Grain Effect" to "Off", "Color Chrome" to "Weak", "Color Chrome FX Blue" to "Strong", "Smooth Skin" to "Weak"),
        tone = mapOf("Highlight Tone" to "-1.5", "Shadow Tone" to "0", "Color" to "+4", "Sharpness" to "+1", "High ISO NR" to "0", "Clarity" to "-2"),
        wb = mapOf("White Balance" to "Auto", "WB Shift R" to "+3", "WB Shift B" to "-3"),
    ).toPreset(slot)
    CameraSlot.C7 -> RecipeUiModel(
        slot = slot.label, name = "RW-Eterna", sim = "Eterna", pills = emptyList(),
        effects = mapOf("Dynamic Range" to "DR200%", "D Range Priority" to "Off", "Grain Effect" to "Weak Small", "Color Chrome" to "Off", "Color Chrome FX Blue" to "Weak", "Smooth Skin" to "Off"),
        tone = mapOf("Highlight Tone" to "0", "Shadow Tone" to "+2.0", "Color" to "-1", "Sharpness" to "0", "High ISO NR" to "0", "Clarity" to "+2"),
        wb = mapOf("White Balance" to "Fluorescent 1", "WB Shift R" to "+1", "WB Shift B" to "+2"),
    ).toPreset(slot)
}

// Grain Effect: camera writes back 6 for Off after receiving 1 (both mean Off per §6.3).
private fun propValuesMatch(prop: FujiPropertyCode, written: Int, readBack: Int?): Boolean {
    if (written == readBack) return true
    if (prop == FujiPropertyCode.GrainEffect) {
        // 1 = write-Off, 6 = factory-default Off, 7 = observed on X-H2 fw5+ after writing 1 (also Off)
        val offValues = setOf(1, 6, 7)
        if (written in offValues && readBack in offValues) return true
    }
    return false
}

data class PropMismatch(
    val name: String,
    val expected: String,
    val actual: String?,
)

data class SlotRoundTripResult(
    val slot: CameraSlot,
    val readOk: Boolean,
    val writeSuccess: Int = 0,
    val writeFailed: Int = 0,
    val writeSkipped: Int = 0,
    val failedWriteProps: List<String> = emptyList(),
    val verifyMatched: Int = 0,
    val verifyTotal: Int = 0,
    val mismatchedProps: List<PropMismatch> = emptyList(),
    val durationMs: Long,
) {
    val writeAttempted: Int get() = writeSuccess + writeFailed
    val allOk: Boolean get() = readOk && writeFailed == 0 && verifyTotal > 0 && verifyMatched == verifyTotal
}

private val COLOR_ONLY_PROPS = setOf(
    FujiPropertyCode.ColorChrome,
    FujiPropertyCode.ColorChromeFxBlue,
    FujiPropertyCode.Color,
    FujiPropertyCode.WbShiftRed,
    FujiPropertyCode.WbShiftBlue,
)

// Reads original, writes a known bench preset property-by-property tracking each failure,
// re-reads to verify values landed, then restores the original.
suspend fun benchRoundTripSlot(
    connection: OpenPtpConnection,
    cam: FujiRecipeCamera,
    slot: CameraSlot,
): SlotRoundTripResult {
    val started = System.currentTimeMillis()

    val original = runCatching { cam.readPreset(slot) }.getOrNull()
        ?: return SlotRoundTripResult(slot = slot, readOk = false, durationMs = System.currentTimeMillis() - started)

    val bench = benchRoundTripPreset(slot)

    // Slot select
    val slotTx = connection.executeCommandWithData(
        code = PtpConstants.SET_DEVICE_PROP_VALUE,
        params = listOf(PtpConstants.FUJI_SLOT_SELECTOR),
        payload = uint16Le(slot.protocolValue),
    )
    if (!slotTx.isOk) {
        runCatching { cam.writePreset(original) }
        return SlotRoundTripResult(
            slot = slot, readOk = true, writeFailed = 1,
            failedWriteProps = listOf("slot-select"),
            durationMs = System.currentTimeMillis() - started,
        )
    }
    delay(25L)
    connection.executeCommand(PtpConstants.GET_DEVICE_INFO)

    val filmSimValue = bench.properties[FujiPropertyCode.FilmSimulation]
    val isMono = filmSimValue != null && filmSimValue in MONO_SIM_CODES
    val isColorTemp = bench.properties[FujiPropertyCode.WhiteBalance] == 0x8007
    val dRangePriority = bench.properties[FujiPropertyCode.DRangePriority] ?: 0

    val ordered = buildList {
        bench.properties[FujiPropertyCode.FilmSimulation]
            ?.let { add(FujiPropertyCode.FilmSimulation to it) }
        bench.properties.filter { (k, _) -> k != FujiPropertyCode.FilmSimulation }
            .forEach { (k, v) -> add(k to v) }
    }

    var writeSuccess = 0
    var writeFailed = 0
    var writeSkipped = 0
    val failedWriteProps = mutableListOf<String>()
    val skippedProps = mutableSetOf<FujiPropertyCode>()

    for ((prop, value) in ordered) {
        if (isMono && prop in COLOR_ONLY_PROPS) { writeSkipped++; skippedProps.add(prop); continue }
        if (!isColorTemp && prop == FujiPropertyCode.ColorTemperature) { writeSkipped++; skippedProps.add(prop); continue }
        if (dRangePriority != 0 && prop == FujiPropertyCode.DynamicRange) { writeSkipped++; skippedProps.add(prop); continue }

        val tx = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(prop.code),
            payload = uint16Le(value),
        )
        if (tx.isOk) writeSuccess++ else { writeFailed++; failedWriteProps.add(prop.displayName) }
    }

    val nameTx = connection.executeCommandWithData(
        code = PtpConstants.SET_DEVICE_PROP_VALUE,
        params = listOf(PtpConstants.FUJI_PRESET_NAME),
        payload = encodePtpString(CameraPresetName.sanitizeOrFallback(bench.name, fallback = slot.label)),
    )
    if (nameTx.isOk) writeSuccess++ else { writeFailed++; failedWriteProps.add("name") }

    // Verify — re-read and confirm name, film sim, and every written property landed.
    val verified = runCatching { cam.readPreset(slot) }.getOrNull()
    var verifyMatched = 0
    var verifyTotal = 0
    val mismatchedProps = mutableListOf<PropMismatch>()

    if (verified != null) {
        // Name
        val expectedName = CameraPresetName.sanitizeOrFallback(bench.name, fallback = slot.label)
        verifyTotal++
        if (verified.name == expectedName) verifyMatched++
        else mismatchedProps.add(PropMismatch("Name", expectedName, verified.name))

        // Film sim + all other written properties (skipped ones excluded)
        for ((prop, expected) in bench.properties) {
            if (prop in skippedProps) continue
            verifyTotal++
            val actual = verified.properties[prop]
            if (propValuesMatch(prop, expected, actual)) verifyMatched++
            else mismatchedProps.add(PropMismatch(prop.displayName, expected.toString(), actual?.toString()))
        }
    }

    runCatching { cam.writePreset(original) }

    return SlotRoundTripResult(
        slot = slot,
        readOk = true,
        writeSuccess = writeSuccess,
        writeFailed = writeFailed,
        writeSkipped = writeSkipped,
        failedWriteProps = failedWriteProps,
        verifyMatched = verifyMatched,
        verifyTotal = verifyTotal,
        mismatchedProps = mismatchedProps,
        durationMs = System.currentTimeMillis() - started,
    )
}
