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
import com.ilfforever.fujisync.data.usb.WbModeResult
import com.ilfforever.fujisync.data.usb.WbModeSweep
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
private fun Label(text: String) {
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
private fun WbModeRow(result: WbModeResult) {
    val statusColor = when {
        result.accepted && !result.mode.documented -> Gold
        result.accepted -> TextPrimary
        else -> TextDim
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = result.mode.label,
                fontFamily = SansFamily,
                fontWeight = if (result.mode.documented) FontWeight.Normal else FontWeight.Bold,
                fontSize = 12.5.sp,
                color = if (result.mode.documented) TextPrimary else Gold,
            )
            Text(
                text = result.mode.hex,
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                color = TextMuted,
            )
        }
        Text(
            text = result.verdict,
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = statusColor,
            modifier = Modifier.padding(top = 3.dp),
        )
        if (!result.mode.documented) {
            Text(
                text = "derived from X RAW STUDIO — not previously seen on a camera",
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                color = TextDim,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * White Balance mode sweep: a button and, once a run has happened, one row per mode.
 *
 * The point of the section is the five modes that came out of static analysis and have never been
 * seen on hardware. They are drawn in gold so a run's outcome is readable at a glance.
 */
@Composable
fun WbModeBenchSection(
    sweep: WbModeSweep?,
    running: Boolean,
    onRun: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Label("WHITE BALANCE MODES  ·  CONFIRM THE DERIVED FIVE")

        Text(
            text = "Writes all 14 known WB modes to the live property one at a time, reads each " +
                "back, then restores the mode the camera started in. Nine are already confirmed " +
                "on hardware and act as controls; five come from X RAW STUDIO and are unproven.",
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = TextDim,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (running) Border else Gold, RoundedCornerShape(8.dp))
                .background(PanelLow)
                .clickable(enabled = !running, onClick = onRun)
                .padding(vertical = 13.dp),
        ) {
            Text(
                text = if (running) "SWEEPING…" else "RUN WB MODE SWEEP",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                letterSpacing = 1.2.sp,
                color = if (running) TextMuted else Gold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (sweep == null) return@Column

        Spacer(Modifier.height(16.dp))

        Text(
            text = sweep.summary,
            fontFamily = MonoFamily,
            fontSize = 10.5.sp,
            lineHeight = 15.sp,
            color = if (sweep.trustworthy) TextPrimary else Gold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = buildString {
                append("Started in ")
                append(sweep.original?.let { "0x%04X".format(it) } ?: "unknown")
                append(" · ")
                append(if (sweep.cameraRestored) "restored" else "NOT RESTORED — set WB by hand")
                append(" · ${sweep.durationMs} ms")
            },
            fontFamily = MonoFamily,
            fontSize = 9.5.sp,
            color = if (sweep.cameraRestored) TextDim else Gold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        sweep.results.forEach { result ->
            WbModeRow(result)
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
        }
    }
}

/** Plain-text form for sharing a run into the protocol repo as a test report. */
fun formatWbModeSweep(sweep: WbModeSweep, cameraModel: String): String = buildString {
    appendLine("White Balance mode sweep — $cameraModel")
    appendLine(sweep.summary)
    appendLine(
        "started 0x%04X · %s · %d ms".format(
            sweep.original ?: 0,
            if (sweep.cameraRestored) "restored" else "NOT RESTORED",
            sweep.durationMs,
        ),
    )
    appendLine()
    sweep.results.forEach { r ->
        appendLine(
            "%-24s %-8s %-10s %s".format(
                r.mode.label,
                r.mode.hex,
                if (r.mode.documented) "documented" else "DERIVED",
                r.verdict,
            ),
        )
    }
}
