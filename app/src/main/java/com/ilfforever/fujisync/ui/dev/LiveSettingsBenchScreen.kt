package com.ilfforever.fujisync.ui.dev

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.data.usb.runWbModeSweep
import com.ilfforever.fujisync.ui.dev.components.LiveSettingsResultsTable
import com.ilfforever.fujisync.ui.dev.components.WbModeBenchSection
import com.ilfforever.fujisync.ui.dev.components.formatLiveSettingsShareText
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
fun LiveSettingsBenchScreen(
    viewModel: LiveSettingsBenchViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val tests by viewModel.tests.collectAsState()
    val busyCode by viewModel.busyCode.collectAsState()
    val wbSweep by viewModel.wbSweep.collectAsState()
    val wbRunning by viewModel.wbRunning.collectAsState()
    val running = state is LiveSettingsBenchViewModel.State.Running
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "LIVE / C0 BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state is LiveSettingsBenchViewModel.State.Done || state is LiveSettingsBenchViewModel.State.Error) {
                    Text(
                        text = "RESET",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.3.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { viewModel.reset() }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
                Text(
                    text = "CLOSE",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.3.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelLow)
                    .border(1.dp, if (running) Border else Gold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !running && busyCode == null) { viewModel.runAll() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (running) {
                    CircularProgressIndicator(color = Gold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = if (running) {
                        (state as LiveSettingsBenchViewModel.State.Running).phase
                    } else {
                        "RUN EVERYTHING  (read → test each → restore)"
                    },
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (running) TextMuted else Gold,
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "READ ONLY  (no writes)",
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = 1.1.sp,
                color = if (running) TextMuted else TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                    .clickable(enabled = !running && busyCode == null) { viewModel.run() }
                    .padding(vertical = 11.dp),
            )

            Text(
                text = "X RAW STUDIO writes a recipe to C1–C7 and to a parallel set of live " +
                    "property codes. With slot 0 it writes only the live set — the P/A/S/M case. " +
                    "This reads each live code's descriptor to see what this body exposes.",
                fontFamily = SansFamily,
                fontSize = 11.sp,
                color = TextDim,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 10.dp),
            )

            when (val s = state) {
                is LiveSettingsBenchViewModel.State.Error -> {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = s.message,
                        fontFamily = MonoFamily,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Gold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PanelLow)
                            .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    )
                }
                is LiveSettingsBenchViewModel.State.Done -> {
                    Spacer(Modifier.height(20.dp))
                    s.result.cameraModel?.let {
                        Text("Camera: $it", fontFamily = MonoFamily, fontSize = 11.sp, color = TextDim)
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        text = "Each test changes one property, reads it back, then restores it. " +
                            "Watch the camera — a verified change is the proof the live path works. " +
                            "Tap a single row to retest just that one.",
                        fontFamily = SansFamily,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(16.dp))

                    LiveSettingsResultsTable(
                        result = s.result,
                        testResults = tests,
                        busyCode = busyCode,
                        onTest = viewModel::test,
                    )

                    Spacer(Modifier.height(28.dp))
                    WbModeBenchSection(
                        sweep = wbSweep,
                        running = wbRunning,
                        onRun = viewModel::runWbModeSweep,
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Read pass: %.2fs".format(s.result.durationMs / 1000.0),
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "SHARE RESULTS",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.3.sp,
                        color = Gold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PanelLow)
                            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, formatLiveSettingsShareText(s.result, tests))
                                }
                                context.startActivity(Intent.createChooser(intent, "Share live bench results"))
                            }
                            .padding(vertical = 14.dp),
                    )
                }
                else -> Unit
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
