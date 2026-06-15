package com.ilfforever.fujisync.ui.camera

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldDim
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.SheetBg
import com.ilfforever.fujisync.ui.theme.SheetBorder
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BackupControlBg = Color(0xFF141210)

@Composable
fun BackupSheet(
    saving: Boolean = false,
    savingSlotIndex: Int = -1,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val defaultLabel = remember {
        "C1–C7 · " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }
    var label by remember { mutableStateOf(defaultLabel) }
    var submitted by remember { mutableStateOf(false) }
    var observedSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion(playHaptic: Boolean = true) {
        if (saving) return
        if (playHaptic) FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)

    fun save() {
        val trimmed = label.trim().ifBlank { defaultLabel }
        submitted = true
        onConfirm(trimmed)
    }

    LaunchedEffect(saving, submitted) {
        if (saving) {
            observedSaving = true
            FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
        }
        if (submitted && observedSaving && !saving) dismissWithMotion(playHaptic = false)
    }

    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "backup-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "backup-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * overlayAlpha))
            .imePadding()
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
                    .heightIn(max = 500.dp)
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
                        .padding(horizontal = 22.dp)
                        .padding(top = 18.dp, bottom = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "BACKUP SETS",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = 0.4.sp,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "SAVE C1–C7 TO PHONE",
                                fontFamily = MonoFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.3.sp,
                                color = TextDim,
                            )
                        }
                        Text(
                            text = "CANCEL",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.3.sp,
                            color = Gold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(enabled = !saving, onClick = ::dismissWithMotion)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "NAME",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp,
                        letterSpacing = 2.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it },
                        enabled = !saving,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BackupControlBg)
                            .border(1.dp, Gold, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Reads C1–C7 from the camera, then saves a global backup set to this device.",
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = TextDim,
                    )
                    AnimatedVisibility(visible = saving) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            BackupReadProgress(currentSlotIndex = savingSlotIndex)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    PrimaryCTA(
                        label = if (saving) "Reading Camera" else "Save Set",
                        busy = saving,
                        enabled = !saving,
                        onClick = ::save,
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupReadProgress(currentSlotIndex: Int) {
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(currentSlotIndex) {
        if (currentSlotIndex >= 0) FujiHaptics.performStepClick(context, view, step = currentSlotIndex, total = 7)
    }
    val transition = rememberInfiniteTransition(label = "backup-read-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "backup-read-pulse-alpha",
    )
    val safeIndex = currentSlotIndex.coerceIn(-1, 6)
    val statusText = if (safeIndex >= 0) {
        "Reading C${safeIndex + 1} from camera"
    } else {
        "Opening camera session"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelHigh.copy(alpha = 0.72f))
            .border(1.dp, GoldDim.copy(alpha = 0.35f + (0.18f * pulse)), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "VALIDATING CAMERA",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = Gold,
            )
            Text(
                text = statusText.uppercase(Locale.US),
                fontFamily = SansFamily,
                fontSize = 11.sp,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(7) { index ->
                val active = safeIndex == index || (safeIndex < 0 && index == 0)
                val complete = safeIndex > index
                val slotAlpha = when {
                    active -> 0.18f + (0.20f * pulse)
                    complete -> 0.28f
                    else -> 0.08f
                }
                val scale by animateFloatAsState(
                    targetValue = if (active) 1.06f else 1f,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "backup-slot-scale-$index",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(Gold.copy(alpha = slotAlpha))
                        .border(
                            1.dp,
                            when {
                                active -> Gold.copy(alpha = 0.82f)
                                complete -> GoldDim.copy(alpha = 0.62f)
                                else -> Border.copy(alpha = 0.65f)
                            },
                            RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "C${index + 1}",
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (complete || active) TextPrimary else TextMuted,
                    )
                }
            }
        }
    }
}
