package com.ilfforever.fujisync.ui.dev.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.data.usb.NewPathsProbeResult
import com.ilfforever.fujisync.data.usb.PropProbeResult
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
fun ScalarProbeResultsTable(results: List<PropProbeResult>) {
    SectionLabel(text = "Check codes / battery info")
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        results.forEachIndexed { idx, r ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (r.ok) PanelLow else Gold.copy(alpha = 0.04f))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "0x%04X".format(r.propCode),
                        fontFamily = MonoFamily, fontWeight = FontWeight.Bold,
                        fontSize = 12.sp, color = TextPrimary,
                    )
                    Text(r.label, fontFamily = MonoFamily, fontSize = 12.sp, color = TextPrimary)
                    Text(
                        text = if (r.ok) "OK" else "ERR",
                        fontFamily = MonoFamily, fontSize = 11.sp,
                        color = if (r.ok) Gold else TextMuted,
                    )
                }
                Text(
                    text = "raw: ${r.rawHex}",
                    fontFamily = MonoFamily, fontSize = 10.sp, color = TextDim,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "decoded: ${r.decoded}",
                    fontFamily = MonoFamily, fontSize = 10.sp, color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = r.note,
                    fontFamily = MonoFamily, fontSize = 9.5.sp, color = TextDim,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (idx < results.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            }
        }
    }
}

fun formatNewPathsProbeShareText(result: NewPathsProbeResult): String = buildString {
    appendLine("NEW PATHS PROBE — FujiSync")
    result.cameraModel?.let { appendLine("Camera: $it") }
    appendLine("Total: %.2fs".format(result.durationMs / 1000.0))
    appendLine()
    for (r in result.scalarResults) {
        appendLine("0x%04X  %s".format(r.propCode, r.label))
        appendLine("  status: ${if (r.ok) "OK" else "ERR"}")
        appendLine("  raw: ${r.rawHex}")
        appendLine("  decoded: ${r.decoded}")
    }
    appendLine()
    appendLine("0x%04X  %s".format(result.propListRaw.propCode, result.propListRaw.label))
    appendLine("  status: ${if (result.propListRaw.ok) "OK" else "ERR"}")
    appendLine("  raw: ${result.propListRaw.rawHex}")
    appendLine("  decoded: ${result.propListRaw.decoded}")
    for (entry in result.propList) {
        appendLine("    0x%04X (%d bytes)  %s".format(entry.propCode, entry.valueLength, entry.valueHex))
    }
}
