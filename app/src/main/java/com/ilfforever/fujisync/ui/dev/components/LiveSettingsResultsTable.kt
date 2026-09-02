package com.ilfforever.fujisync.ui.dev.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.data.usb.LivePropReading
import com.ilfforever.fujisync.data.usb.LiveReadResult
import com.ilfforever.fujisync.data.usb.LiveWriteTest
import com.ilfforever.fujisync.data.usb.formatOperationDiagnostic
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = SansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        letterSpacing = 1.4.sp,
        color = TextMuted,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun LiveSettingsResultsTable(
    result: LiveReadResult,
    testResults: Map<Int, LiveWriteTest>,
    busyCode: Int?,
    onTest: (LivePropReading) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("LIVE / C0  ·  ${result.readableCount}/${result.readings.size} READABLE  ·  ${result.advertisedHere} ADVERTISED")

        Text(
            text = "Camera ${result.deviceModel} lists ${result.advertisedCount} properties in DeviceInfo. " +
                "GetDevicePropDesc: ${result.descNote}.",
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = TextDim,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        SectionLabel("CONTROLS  ·  CODES KNOWN TO WORK ON THIS CAMERA")
        result.controls.forEach { reading ->
            LivePropRow(reading, null, false) {}
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
        }
        Spacer(Modifier.height(20.dp))
        SectionLabel("LIVE / C0 CANDIDATES")

        result.readings.forEach { reading ->
            LivePropRow(reading, testResults[reading.prop.liveCode], busyCode == reading.prop.liveCode, onTest)
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("DECOMPOSE SELECTORS  ·  READ ONLY")
        result.decompose.forEach { reading ->
            LivePropRow(reading, null, false) {}
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
        }
    }
}

@Composable
private fun LivePropRow(
    reading: LivePropReading,
    test: LiveWriteTest?,
    busy: Boolean,
    onTest: (LivePropReading) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(end = 10.dp)) {
                Text(
                    text = reading.prop.label,
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (reading.readable) TextPrimary else TextDim,
                )
                Text(
                    text = if (reading.prop.structOffset >= 0) {
                        "slot ${reading.prop.slotHex}  →  live ${reading.prop.liveHex}"
                    } else {
                        reading.prop.liveHex
                    },
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    color = TextDim,
                )
            }
            Text(
                text = reading.value?.toString() ?: "—",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (reading.readable) Gold else TextDim,
            )
        }

        Text(
            text = reading.note,
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = TextDim,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (reading.testable) {
            Text(
                text = when {
                    busy -> "TESTING…"
                    reading.desc?.alternateValue() != null -> "TEST WRITE  (change → verify → restore)"
                    else -> "TEST WRITE  (no declared range — tries value + 1)"
                },
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp,
                color = if (busy) TextMuted else Gold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Gold.copy(alpha = if (busy) 0.2f else 0.45f), RoundedCornerShape(8.dp))
                    .background(PanelLow)
                    .clickable(enabled = !busy) { onTest(reading) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        } else if (reading.excludedReason != null) {
            Text(
                text = reading.excludedReason!!,
                fontFamily = MonoFamily,
                fontSize = 9.5.sp,
                color = TextDim,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else if (reading.advertised) {
            Text(
                text = "advertised but not readable — cannot test",
                fontFamily = MonoFamily,
                fontSize = 9.5.sp,
                color = TextDim,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        test?.let {
            Text(
                text = it.verdict + when {
                    it.restored -> "  ·  restored"
                    it.effectivelyRestored -> "  ·  restored (${it.restoreNote})"
                    else -> "  ·  NOT RESTORED — ${it.restoreNote}"
                },
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = if (it.verified) Gold else TextPrimary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelLow)
                    .border(
                        1.dp,
                        if (it.verified) Gold.copy(alpha = 0.5f) else Border,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(10.dp),
            )
        }
    }
}

fun formatLiveSettingsShareText(
    result: LiveReadResult,
    testResults: Map<Int, LiveWriteTest>,
): String = buildString {
    appendLine("FujiSync — live / C0 push bench")
    appendLine("Camera: ${result.cameraModel ?: "unknown"}")
    appendLine("Model: ${result.deviceModel}   DeviceInfo lists ${result.advertisedCount} properties")
    appendLine("GetDevicePropDesc: ${result.descNote}")
    appendLine("Readable: ${result.readableCount}/${result.readings.size}   Advertised: ${result.advertisedHere}")
    appendLine("Read pass: %.2fs".format(result.durationMs / 1000.0))
    appendLine()
    appendLine("slot    live    value   property / desc")
    result.readings.forEach { r ->
        appendLine(
            "%-7s %-7s %-7s %s — %s".format(
                if (r.prop.structOffset >= 0) r.prop.slotHex else "-",
                r.prop.liveHex,
                r.value?.toString() ?: "-",
                r.prop.label,
                r.note,
            ),
        )
    }
    appendLine()
    result.decompose.forEach { r ->
        appendLine("%-7s %-7s %s — %s".format("-", r.prop.liveHex, r.value?.toString() ?: "-", r.prop.label))
    }
    appendLine()
    appendLine(formatOperationDiagnostic(result.opProbes))
    appendLine()
    appendLine("CONTROLS (codes this app already reads successfully):")
    result.controls.forEach { r ->
        appendLine("  %-7s %-7s %s" .format(r.prop.liveHex, r.value?.toString() ?: "-", r.note))
    }
    appendLine()
    appendLine("EVERY property this camera advertises (${result.advertisedCodes.size}):")
    result.advertisedCodes.chunked(8).forEach { row ->
        appendLine("  " + row.joinToString(" ") { "0x%04X".format(it) })
    }
    if (testResults.isNotEmpty()) {
        appendLine()
        appendLine("WRITE TESTS (change → verify → restore)")
        testResults.values.forEach { t ->
            appendLine("${t.prop.liveHex} ${t.prop.label}: ${t.verdict}")
            appendLine("    restore → " + when {
                t.restored -> "ok"
                t.effectivelyRestored -> "ok (${t.restoreNote})"
                else -> "NOT RESTORED — ${t.restoreNote}"
            })
        }
    }
}
