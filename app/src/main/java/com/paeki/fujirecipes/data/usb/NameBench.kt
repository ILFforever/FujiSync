package com.paeki.fujirecipes.data.usb

import com.paeki.fujirecipes.data.ptp.CameraPresetName
import com.paeki.fujirecipes.domain.model.CameraSlot

data class NameBenchCase(
    val slot: CameraSlot,
    val label: String,
    val rawName: String,
) {
    val expectedName: String = CameraPresetName.sanitizeOrFallback(rawName, fallback = slot.label)
}

data class NameBenchResult(
    val case: NameBenchCase,
    val writeOk: Boolean,
    val readName: String?,
) {
    val matchesExpected: Boolean get() = writeOk && readName == case.expectedName
}

val NAME_BENCH_CASES: List<NameBenchCase> = listOf(
    NameBenchCase(CameraSlot.C1, "Basic", "NAMEBENCH-BASIC"),
    NameBenchCase(CameraSlot.C2, "25 chars", "ABCDEFGHIJKLMNOPQRSTUVWXY"),
    NameBenchCase(CameraSlot.C3, "Punctuation A", "!\"#$%&'()*+,-./"),
    NameBenchCase(CameraSlot.C4, "Punctuation B", ":;<=>?@[]\\^_{}|~"),
    NameBenchCase(CameraSlot.C5, "Accent quote", "Café Nat’s Résumé"),
    NameBenchCase(CameraSlot.C6, "Dash dot", "Long-Dash.Dot-End"),
    NameBenchCase(CameraSlot.C7, "Emoji collapse", "Emoji🎞️Collapse  Test"),
)

suspend fun benchPresetNames(
    camera: FujiRecipeCamera,
    onProgress: (phase: String) -> Unit = {},
): List<NameBenchResult> {
    val written = NAME_BENCH_CASES.map { case ->
        onProgress("Writing ${case.slot.label}…")
        case to runCatching { camera.writePresetName(case.slot, case.rawName) }.getOrDefault(false)
    }

    return written.map { (case, writeOk) ->
        onProgress("Reading ${case.slot.label}…")
        val readName = runCatching { camera.readPreset(case.slot).name }.getOrNull()
        NameBenchResult(case = case, writeOk = writeOk, readName = readName)
    }
}
