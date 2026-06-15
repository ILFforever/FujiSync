package com.ilfforever.fujisync.ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHapticResult
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

private data class AppHapticOption(
    val label: String,
    val effect: FujiHapticEffect,
    val detail: String,
)

@Composable
fun HapticBenchScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var lastResult by remember { mutableStateOf(FujiHapticResult(false, "not run")) }

    fun play(effect: FujiHapticEffect) {
        lastResult = FujiHaptics.perform(context, view, effect)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "HAPTIC BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "STOP",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.3.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            FujiHaptics.cancel(context)
                            lastResult = FujiHapticResult(true, "cancelled active vibration")
                        }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                )
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
            Spacer(Modifier.height(18.dp))
            StatusCard(lastResult = lastResult)

            Spacer(Modifier.height(24.dp))
            BenchSection(title = "APP HAPTICS") {
                appHaptics.forEachIndexed { index, option ->
                    BenchActionRow(
                        label = option.label,
                        detail = option.detail,
                        onClick = { play(option.effect) },
                    )
                    if (index < appHaptics.lastIndex) Divider()
                }
            }

            Spacer(Modifier.height(24.dp))
            BenchSection(title = "EXPERIMENTS") {
                experimentalHaptics.forEachIndexed { index, option ->
                    BenchActionRow(
                        label = option.label,
                        detail = option.detail,
                        onClick = { play(option.effect) },
                    )
                    if (index < experimentalHaptics.lastIndex) Divider()
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatusCard(lastResult: FujiHapticResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelHigh)
            .border(1.dp, if (lastResult.played) Gold.copy(alpha = 0.42f) else Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "LAST RESULT",
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                letterSpacing = 1.6.sp,
                color = TextDim,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = lastResult.path,
                fontFamily = SansFamily,
                fontSize = 13.sp,
                color = TextPrimary,
            )
        }
        Text(
            text = if (lastResult.played) "PLAYED" else "IDLE",
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = if (lastResult.played) Gold else TextDim,
        )
    }
}

@Composable
private fun BenchSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 1.6.sp,
            color = TextDim,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(14.dp)),
            content = content,
        )
    }
}

@Composable
private fun BenchActionRow(
    label: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = detail,
                fontFamily = SansFamily,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = TextDim,
            )
        }
        Text(
            text = "PLAY",
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = Gold,
        )
    }
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
}

private val appHaptics = listOf(
    AppHapticOption("Selection", FujiHapticEffect.Selection, "Used for Library controls, filters, selection toggles, and lightweight choices."),
    AppHapticOption("Soft selection", FujiHapticEffect.SoftSelection, "Single soft click for non-destructive light actions."),
    AppHapticOption("Sheet open", FujiHapticEffect.SheetOpen, "Used when modal sheets and editors appear."),
    AppHapticOption("Sheet dismiss", FujiHapticEffect.SheetDismiss, "Used when modal sheets and editors are dismissed."),
    AppHapticOption("Drawer swoosh open", FujiHapticEffect.DrawerSwooshOpen, "Used by the Library add drawer opening motion."),
    AppHapticOption("Drawer swoosh dismiss", FujiHapticEffect.DrawerSwooshDismiss, "Used by the Library add drawer dismiss motion."),
    AppHapticOption("Confirm", FujiHapticEffect.Confirm, "Used for save, apply, update, assign, and create actions."),
    AppHapticOption("Soft confirm", FujiHapticEffect.SoftConfirm, "Used for navigation panel taps and light confirmations."),
    AppHapticOption("Reject", FujiHapticEffect.Reject, "Used before destructive or risky actions."),
    AppHapticOption("Drag start", FujiHapticEffect.DragStart, "Used for long-press selection/grab moments."),
    AppHapticOption("Soft success", FujiHapticEffect.SoftSuccess, "Used for favorite toggles and quiet positive actions."),
)

private val experimentalHaptics = listOf(
    AppHapticOption("Rumble rise", FujiHapticEffect.RumbleRise, "Linear-ish ramp that gets stronger over time."),
    AppHapticOption("Rumble fall", FujiHapticEffect.RumbleFall, "Reverse ramp that drops away quickly."),
    AppHapticOption("Rumble pop", FujiHapticEffect.RumblePop, "Small build-up into a strong final punch."),
    AppHapticOption("Triple knock", FujiHapticEffect.TripleKnock, "Heavy-light-light knock pattern."),
    AppHapticOption("Scan ramp", FujiHapticEffect.ScanRamp, "Stepped scanning/building feedback."),
    AppHapticOption("Delayed ramp", FujiHapticEffect.DelayedRamp, "Eight separated buzzes with 100ms gaps, ramping to full power."),
    AppHapticOption("Elastic snap", FujiHapticEffect.ElasticSnap, "Rise, snap, and fall release."),
    AppHapticOption("Warning buzz", FujiHapticEffect.WarningBuzz, "Longer warning/error buzz pattern."),
    AppHapticOption("Calm double", FujiHapticEffect.CalmDouble, "Click, clear pause, stronger click."),
    AppHapticOption("Deliberate double", FujiHapticEffect.DeliberateDouble, "Heavier first hit, longer pause, strong finish."),
    AppHapticOption("Warning pause", FujiHapticEffect.WarningPause, "Slow warning-style pause pattern."),
    AppHapticOption("Success pause", FujiHapticEffect.SuccessPause, "Light primer, pause, strong success hit."),
)
