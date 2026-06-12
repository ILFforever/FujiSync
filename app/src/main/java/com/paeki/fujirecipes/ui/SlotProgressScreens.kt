package com.paeki.fujirecipes.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.haptics.FujiHaptics
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelHigh
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

// ── Restore set loading ───────────────────────────────────────────

@Composable
internal fun RestoreSetLoadingScreen(currentSlotIndex: Int) {
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(currentSlotIndex) {
        if (currentSlotIndex >= 0) FujiHaptics.performStepClick(context, view, step = currentSlotIndex, total = 7)
    }
    val transition = rememberInfiniteTransition(label = "restore-set")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "restore-pulse",
    )
    val activeIndex = currentSlotIndex.coerceIn(0, 6)
    val progress = ((activeIndex + 1) / 7f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg.copy(alpha = 0.66f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(318.dp)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(PanelLow)
                .border(
                    1.dp,
                    Gold.copy(alpha = 0.22f + 0.12f * pulse),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                )
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "RESTORE SET",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "Writing C${activeIndex + 1} of C7 to camera",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "Keep the USB cable connected until the set finishes.",
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PanelHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Gold.copy(alpha = 0.8f)),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(7) { index ->
                    val active = index == activeIndex
                    val completed = index < activeIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (active) 38.dp else 32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        active -> Gold.copy(alpha = 0.14f + 0.16f * pulse)
                                        completed -> Gold.copy(alpha = 0.12f)
                                        else -> PanelHigh
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        active -> Gold.copy(alpha = 0.62f + 0.22f * pulse)
                                        completed -> Gold.copy(alpha = 0.32f)
                                        else -> Border
                                    },
                                    RoundedCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "C${index + 1}",
                                fontFamily = MonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (active || completed) Gold else TextDim,
                            )
                        }
                        Text(
                            text = when {
                                completed -> "DONE"
                                active -> "NOW"
                                else -> "WAIT"
                            },
                            fontFamily = MonoFamily,
                            fontSize = 7.sp,
                            letterSpacing = 0.6.sp,
                            color = if (active) Gold else TextDim,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelHigh)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SLOTS RESTORED",
                    fontFamily = MonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    color = TextDim,
                )
                Text(
                    text = "${activeIndex + 1}/7",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Gold,
                )
            }
        }
    }
}

// ── Rearrange slots loading ───────────────────────────────────────

@Composable
internal fun RearrangeSlotsLoadingScreen(
    currentSlotIndex: Int,
    writeIndex: Int,
    writeTotal: Int,
) {
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(writeIndex) {
        if (writeIndex >= 0) FujiHaptics.performStepClick(context, view, writeIndex, writeTotal)
    }
    val transition = rememberInfiniteTransition(label = "rearrange-slots")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rearrange-pulse",
    )
    val activeIndex = currentSlotIndex.coerceIn(0, 6)
    val total = writeTotal.coerceAtLeast(1)
    val hasStarted = currentSlotIndex >= 0 && writeIndex >= 0
    val activeWrite = writeIndex.coerceIn(0, total - 1)
    val completedLabel = if (hasStarted) activeWrite + 1 else 0
    val progress = if (hasStarted) ((activeWrite + 1) / total.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg.copy(alpha = 0.74f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(298.dp)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(PanelLow)
                .border(
                    1.dp,
                    Gold.copy(alpha = 0.24f + 0.14f * pulse),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                )
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "REARRANGE RECIPES",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = TextPrimary,
                )
                Text(
                    text = if (hasStarted) "Writing C${activeIndex + 1} to camera" else "Preparing slot writes",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "Keep the USB cable connected until the slot swap finishes.",
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PanelHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Gold.copy(alpha = 0.82f)),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(7) { index ->
                    val active = hasStarted && index == activeIndex
                    Box(
                        modifier = Modifier
                            .size(if (active) 40.dp else 34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) Gold.copy(alpha = 0.14f + 0.16f * pulse) else PanelHigh)
                            .border(
                                1.dp,
                                if (active) Gold.copy(alpha = 0.62f + 0.22f * pulse) else Border,
                                RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "C${index + 1}",
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = if (active) Gold else TextDim,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelHigh)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "WRITE PROGRESS",
                    fontFamily = MonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    color = TextDim,
                )
                Text(
                    text = "$completedLabel/$total",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Gold,
                )
            }
        }
    }
}
