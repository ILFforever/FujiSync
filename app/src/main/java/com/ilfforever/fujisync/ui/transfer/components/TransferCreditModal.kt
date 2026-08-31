package com.ilfforever.fujisync.ui.transfer.components

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconStar
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldFaint
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One-time modal shown the first time the Transfer tab is opened. Dismissing it persists
 * [com.ilfforever.fujisync.ui.model.AppSettings.creditNoticeSeen], so it never returns.
 */
@Composable
internal fun TransferCreditModal(onDismiss: () -> Unit) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(160); onDismiss() }
    }

    BackHandler(enabled = true) { dismissWithMotion() }

    LaunchedEffect(motionEnabled) { visible = true }

    val overlayTransition = updateTransition(targetState = visible, label = "credit-modal-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 180 else 130, easing = FastOutSlowInEasing) },
        label = "credit-modal-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f * overlayAlpha))
            .clickable(onClick = ::dismissWithMotion),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(170, easing = FastOutSlowInEasing)) +
                scaleIn(tween(240, easing = FastOutSlowInEasing), initialScale = 0.92f),
            exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                scaleOut(tween(160, easing = FastOutSlowInEasing), targetScale = 0.96f),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(20.dp))
                    .clickable(onClick = {})
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GoldFaint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = IconStar,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "CREDIT THE MAKER",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    color = Gold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Recipes are someone's work.\n\n" +
                        "If you didn't create it, keep the creator's name on it when you save " +
                        "or share — and respect creators who ask that their recipes not be " +
                        "passed around.",
                    fontFamily = SansFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = TextDim,
                )
                Spacer(Modifier.height(24.dp))
                PrimaryCTA(
                    label = "Got It",
                    onClick = ::dismissWithMotion,
                )
            }
        }
    }
}
