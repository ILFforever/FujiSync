package com.ilfforever.fujisync.ui.dev.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.data.usb.PropListEntry
import com.ilfforever.fujisync.data.usb.PropProbeResult
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

// 0xD235 — the live per-camera property list. See NewPathsProbe.kt for how this is parsed.
@Composable
fun PropListResultsTable(raw: PropProbeResult, entries: List<PropListEntry>) {
    SectionLabel(text = "0xD235 — live property list")
    Spacer(Modifier.height(8.dp))
    Text(
        text = raw.note,
        fontFamily = MonoFamily,
        fontSize = 9.5.sp,
        color = TextDim,
        lineHeight = 13.sp,
    )
    Spacer(Modifier.height(8.dp))

    if (!raw.ok) {
        Text(
            text = "status: ERR — ${raw.decoded} — raw: ${raw.rawHex}",
            fontFamily = MonoFamily,
            fontSize = 11.sp,
            color = Gold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PanelLow)
                .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(12.dp),
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text("PROP",  fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp, color = TextDim, modifier = Modifier.weight(0.2f))
            Text("BYTES", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp, color = TextDim, modifier = Modifier.weight(0.15f))
            Text("VALUE", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp, color = TextDim, modifier = Modifier.weight(0.65f))
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))

        entries.forEachIndexed { idx, e ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    "0x%04X".format(e.propCode),
                    fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    color = TextPrimary, modifier = Modifier.weight(0.2f),
                )
                Text("${e.valueLength}", fontFamily = MonoFamily, fontSize = 11.sp, color = TextMuted, modifier = Modifier.weight(0.15f))
                Text(e.valueHex, fontFamily = MonoFamily, fontSize = 10.sp, color = TextDim, modifier = Modifier.weight(0.65f))
            }
            if (idx < entries.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Text(
        text = "${entries.size} propert${if (entries.size == 1) "y" else "ies"} reported by the camera right now",
        fontFamily = MonoFamily,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
        color = TextMuted,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelHigh)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}
