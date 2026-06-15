package com.ilfforever.fujisync.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay

private const val FADE_IN_MS = 450
private const val HOLD_MS = 1100L
private const val FADE_OUT_MS = 400

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(FADE_IN_MS))
        delay(HOLD_MS)
        alpha.animateTo(0f, tween(FADE_OUT_MS))
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .graphicsLayer { this.alpha = alpha.value },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SplashWordmark()
            Spacer(Modifier.height(14.dp))
            Subtitle()
            Spacer(Modifier.height(20.dp))
            ScanLine()
        }
    }
}

@Composable
private fun SplashWordmark() {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "FUJI",
            fontFamily = SansFamily,
            fontWeight = FontWeight.Black,
            fontSize = 56.sp,
            letterSpacing = (-1).sp,
            color = TextPrimary,
        )
        Text(
            text = "SYNC",
            fontFamily = SansFamily,
            fontWeight = FontWeight.Light,
            fontSize = 56.sp,
            letterSpacing = (-1).sp,
            color = Gold,
        )
    }
}

@Composable
private fun Subtitle() {
    Text(
        text = "FILM RECIPE MANAGER",
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 3.sp,
        color = TextMuted,
    )
}

@Composable
private fun ScanLine(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanProgress",
    )

    val lineColor = Gold.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 48.dp)
            .clipToBounds()
            .drawBehind {
                val lineWidth = size.width * 0.35f
                val startX = (size.width + lineWidth) * progress - lineWidth
                drawLine(
                    color = lineColor,
                    start = Offset(startX, 0f),
                    end = Offset(startX + lineWidth, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    )
}
