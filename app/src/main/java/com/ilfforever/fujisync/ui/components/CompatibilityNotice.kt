package com.ilfforever.fujisync.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.model.ChangeKind
import com.ilfforever.fujisync.ui.model.CompatibilityChange
import com.ilfforever.fujisync.ui.model.CompatibilitySummary
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.SheetBg
import com.ilfforever.fujisync.ui.theme.SheetBorder
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A single line above the write button saying how much of this recipe will not reach the camera.
 *
 * It is a control, not a banner: tapping opens the detail as a sheet rather than expanding in
 * place, so the write button never moves under the user's thumb.
 */
@Composable
fun CompatibilityNotice(
    summary: CompatibilitySummary,
    modifier: Modifier = Modifier,
) {
    if (summary.isEmpty) return

    var detailOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Gold.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
            .clickable { detailOpen = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = headline(summary),
            fontFamily = SansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = if (summary.isBlocked) Gold else TextPrimary,
        )
        Text(
            text = "DETAILS",
            fontFamily = MonoFamily,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = Gold,
        )
    }

    if (detailOpen) {
        CompatibilityDetailSheet(summary = summary, onDismiss = { detailOpen = false })
    }
}

/**
 * The recipe's affected settings, shown in the app's own property rows so they read the way the
 * recipe does everywhere else.
 *
 * Only changed settings are listed — everything untouched is covered by one sentence, which does
 * the same reassuring work as a full recipe dump without the height.
 */
@Composable
private fun CompatibilityDetailSheet(
    summary: CompatibilitySummary,
    onDismiss: () -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }

    fun dismissWithMotion() {
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) { visible = true }

    val overlayTransition = updateTransition(targetState = visible, label = "compat-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "compat-overlay-alpha",
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
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp),
                ) {
                    Text(
                        text = if (summary.isBlocked) "CANNOT BE WRITTEN" else summary.cameraName.uppercase(),
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.8.sp,
                        color = Gold.copy(alpha = 0.74f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        // The recipe's name in both cases — the reason belongs in the panel below,
                        // where it can name the simulation without repeating the title.
                        text = summary.recipeName.ifBlank { "This recipe" },
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        lineHeight = 25.sp,
                        color = TextPrimary,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    summary.blocked?.let { blocked ->
                        BlockedPanel(
                            cameraName = summary.cameraName,
                            filmSimulation = blocked.setting,
                        )
                    }
                    if (!summary.isBlocked) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Everything not listed below transfers unchanged.",
                            fontFamily = SansFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = TextMuted,
                        )
                        summary.sections.forEach { (section, items) ->
                            SectionRows(section = section, items = items)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    PrimaryCTA(label = "Close", onClick = ::dismissWithMotion, secondary = true)
                }
            }
        }
    }
}

/**
 * Inset panel for the refusal. No accent rail — the inset and the ground shift carry it, and the
 * single gold accent is already doing work in the eyebrow above.
 */
@Composable
private fun BlockedPanel(cameraName: String, filmSimulation: String) {
    Spacer(Modifier.height(14.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Nothing will be written",
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            color = TextPrimary,
        )
        Text(
            text = "The $cameraName does not have $filmSimulation, so this recipe cannot be used " +
                "on it. The slot keeps the recipe it already has.",
            fontFamily = SansFamily,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = TextMuted,
        )
    }
    Spacer(Modifier.height(14.dp))
    Text(
        text = "Change the film simulation, or save a separate version of this recipe for the $cameraName.",
        fontFamily = SansFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextDim,
    )
}

@Composable
private fun SectionRows(section: String, items: List<CompatibilityChange>) {
    Spacer(Modifier.height(18.dp))
    Text(
        text = section,
        fontFamily = MonoFamily,
        fontSize = 9.sp,
        letterSpacing = 1.5.sp,
        color = TextMuted,
    )
    Spacer(Modifier.height(8.dp))
    items.forEach { change ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = change.label,
                fontFamily = SansFamily,
                fontSize = 13.5.sp,
                color = if (change.kind == ChangeKind.Dropped) TextDim else TextMuted,
            )
            Text(
                text = change.value,
                fontFamily = SansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                color = when (change.kind) {
                    ChangeKind.Dropped -> TextDim
                    ChangeKind.Changed, ChangeKind.Warned -> Gold
                    ChangeKind.Interlock -> TextDim
                },
                textDecoration = if (change.kind == ChangeKind.Dropped) {
                    TextDecoration.LineThrough
                } else {
                    null
                },
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        Text(
            text = change.reason,
            fontFamily = SansFamily,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = TextDim,
            modifier = Modifier.padding(top = 1.dp, bottom = 3.dp),
        )
    }
}

private fun headline(summary: CompatibilitySummary): String {
    summary.blocked?.let { return "${it.setting} is not on this camera" }

    val count = summary.affectedCount
    val noun = if (count == 1) "setting" else "settings"
    return when {
        summary.dropped.size == count -> "$count $noun not on this camera"
        else -> "$count $noun won't transfer exactly"
    }
}
