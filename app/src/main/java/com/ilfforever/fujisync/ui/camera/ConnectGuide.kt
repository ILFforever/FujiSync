package com.ilfforever.fujisync.ui.camera

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconCamera
import com.ilfforever.fujisync.ui.components.IconPhone
import com.ilfforever.fujisync.ui.components.IconTool
import com.ilfforever.fujisync.ui.components.IconUSB
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldFaint
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.SheetBg
import com.ilfforever.fujisync.ui.theme.SheetBorder
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Connect guide (disconnected state) ────────────────────────────
@Composable
fun ConnectGuide(
    scanning: Boolean = false,
    scanError: String? = null,
    onSimulateConnect: () -> Unit,
) {
    var troubleshootOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(scanError) {
        if (scanError != null) FujiHaptics.perform(context, view, FujiHapticEffect.WarningPause)
    }
    val steps = listOf(
        ConnectStepUi("01", "Set USB mode", "Connection Setting → USB Mode → USB RAW CONV."),
        ConnectStepUi("02", "Connect directly", "Use a USB-C data cable from phone to camera."),
        ConnectStepUi("03", "Approve access", "Accept the Android USB prompt when it appears."),
    )

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONNECT CAMERA",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 31.sp,
                    lineHeight = 32.sp,
                    letterSpacing = 0.4.sp,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Read the C1-C7 slots over USB-C before editing or writing recipes.",
                    fontFamily = SansFamily,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = TextMuted,
                )
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IconUSB, contentDescription = null, tint = Gold, modifier = Modifier.size(25.dp))
            }
        }

        Spacer(Modifier.height(26.dp))
        ConnectionPathPanel()

        Spacer(Modifier.height(26.dp))
        Text(
            text = "SETUP",
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 2.4.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(16.dp)),
        ) {
            steps.forEachIndexed { index, step ->
                ConnectStepRow(step = step)
                if (index < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Border),
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        PrimaryCTA(label = "Scan USB Camera", busy = scanning, enabled = !scanning, onClick = onSimulateConnect)
        if (scanError != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = scanError,
                fontFamily = SansFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFFB05060),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Border, RoundedCornerShape(14.dp))
                .clickable { troubleshootOpen = true }
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(IconTool, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "TROUBLESHOOT",
                fontFamily = MonoFamily,
                fontSize = 10.5.sp,
                letterSpacing = 1.6.sp,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
    if (troubleshootOpen) {
        TroubleshootSheet(onDismiss = { troubleshootOpen = false })
    }
    }
}

private data class ConnectStepUi(val number: String, val title: String, val body: String)

@Composable
private fun ConnectionPathPanel() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConnectionNode(icon = IconPhone, label = "PHONE")
        Box(modifier = Modifier.weight(1f).height(1.dp).offset(y = (-9).dp).background(GoldFaint))
        ConnectionNode(icon = IconUSB, label = "USB-C")
        Box(modifier = Modifier.weight(1f).height(1.dp).offset(y = (-9).dp).background(GoldFaint))
        ConnectionNode(icon = IconCamera, label = "CAMERA")
    }
}

@Composable
private fun ConnectionNode(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Bg)
                .border(1.dp, GoldFaint, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(text = label, fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = 1.3.sp, color = TextDim)
    }
}

@Composable
private fun ConnectStepRow(step: ConnectStepUi) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(text = step.number, fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.4.sp, color = Gold)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = step.title.uppercase(), fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.2.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(text = step.body, fontFamily = SansFamily, fontSize = 13.sp, lineHeight = 19.sp, color = TextMuted)
        }
    }
}

// ── Troubleshoot sheet ────────────────────────────────────────────

@Composable
private fun TroubleshootSheet(onDismiss: () -> Unit) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(220); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "troubleshoot-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "troubleshoot-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * overlayAlpha))
            .clickable(onClick = ::dismissWithMotion),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150, easing = FastOutSlowInEasing)) + slideInVertically(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 3 },
            ),
            exit = fadeOut(tween(105, easing = FastOutSlowInEasing)) + slideOutVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 4 },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 700.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(SheetBg)
                    .border(1.dp, SheetBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = {})
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp)
                        .width(38.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(TextDim.copy(alpha = 0.55f)),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(text = "TROUBLESHOOT", fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = 1.1.sp, color = TextPrimary)
                            Spacer(Modifier.height(3.dp))
                            Text(text = "USB CONNECTION GUIDE", fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 1.3.sp, color = TextDim)
                        }
                        Text(
                            text = "DONE",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.3.sp,
                            color = Gold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = ::dismissWithMotion)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))

                    TroubleshootSection("EXPECTED BEHAVIOR") {
                        TroubleshootIssueRow("Camera screen goes black", "When USB RAW CONV. mode is active the camera display turns off — this is normal and required.", isLast = false)
                        TroubleshootIssueRow("Phone does not charge", "The camera draws power from the cable, so your phone won't charge while connected.", isLast = false)
                        TroubleshootIssueRow("Status LED blinks", "A blinking LED on the camera means it's in PTP mode and ready. A solid or off LED means it isn't.", isLast = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    TroubleshootSection("USB MODE") {
                        TroubleshootIssueRow("Set USB RAW CONV. / PTP", "On your camera: Menu → Connection Setting → USB Mode → USB RAW CONV.", isLast = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    TroubleshootSection("CABLE") {
                        TroubleshootIssueRow("Use a data cable", "Charge-only USB-C cables don't carry PTP data. Most USB-C cables labelled 'data' work.", isLast = false)
                        TroubleshootIssueRow("Connect directly", "Adapters or hubs between phone and camera usually block the connection.", isLast = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    TroubleshootSection("NO ANDROID PROMPT") {
                        TroubleshootIssueRow("Replug after camera restart", "Power camera off, unplug cable, power on, then reconnect.", isLast = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    TroubleshootSection("SUPPORTED MODELS") {
                        TroubleshootIssueRow("Fujifilm X-series only", "X-H2 · X-H2S · X-T5 · X-S20 · X100VI · X-T50 · and models with USB RAW CONV. in their menu.", isLast = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    TroubleshootSection("CAMERA FROZEN") {
                        TroubleshootIssueRow("Black screen & blinking LED", "If the camera screen goes black and the LED blinks — even after disconnecting USB-C — remove the camera battery to force a reset.", isLast = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun TroubleshootSection(label: String, content: @Composable () -> Unit) {
    Text(text = label, fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 1.5.sp, color = TextMuted, modifier = Modifier.padding(bottom = 9.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TroubleshootControlBg)
            .border(1.dp, SheetBorder, RoundedCornerShape(14.dp)),
    ) {
        content()
    }
}

@Composable
private fun TroubleshootIssueRow(title: String, body: String, isLast: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp)) {
        Text(text = title.uppercase(), fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.8.sp, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(text = body, fontFamily = SansFamily, fontSize = 13.sp, lineHeight = 19.sp, color = TextMuted)
    }
    if (!isLast) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
    }
}

private val TroubleshootControlBg = Color(0xFF141210)
