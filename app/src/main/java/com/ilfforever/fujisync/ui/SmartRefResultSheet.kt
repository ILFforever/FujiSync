package com.ilfforever.fujirecipes.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ilfforever.fujirecipes.ui.model.DuplicateMatchKind
import com.ilfforever.fujirecipes.ui.model.SmartRefResult
import com.ilfforever.fujirecipes.ui.theme.Border
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Smart Reference result sheet ──────────────────────────────────

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun SmartRefResultSheet(
    result: SmartRefResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onScanAnother: (() -> Unit)? = null,
) {
    val motionEnabled = android.animation.ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        com.ilfforever.fujirecipes.ui.haptics.FujiHaptics.perform(context, view, com.ilfforever.fujirecipes.ui.haptics.FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) {
        visible = true
        com.ilfforever.fujirecipes.ui.haptics.FujiHaptics.perform(context, view, com.ilfforever.fujirecipes.ui.haptics.FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "smart-ref-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "smart-ref-overlay-alpha",
    ) { if (it) 1f else 0f }

    val matchLabel = when (result.matchKind) {
        DuplicateMatchKind.ExactSettings -> "EXACT MATCH"
        DuplicateMatchKind.SameName -> "NAME MATCH"
        DuplicateMatchKind.Similar -> "CLOSE MATCH"
    }

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
                    .background(com.ilfforever.fujirecipes.ui.theme.SheetBg)
                    .border(1.dp, com.ilfforever.fujirecipes.ui.theme.SheetBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
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
                        .padding(top = 18.dp, bottom = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RECIPE FOUND",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = 1.1.sp,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "This photo will be added as a reference image.",
                                fontFamily = SansFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = com.ilfforever.fujirecipes.ui.theme.TextMuted,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "CANCEL",
                            fontFamily = MonoFamily,
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

                    Spacer(Modifier.height(18.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141210))
                            .border(1.dp, com.ilfforever.fujirecipes.ui.theme.SheetBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "IN LIBRARY",
                                fontFamily = MonoFamily,
                                fontSize = 9.5.sp,
                                letterSpacing = 1.6.sp,
                                color = TextDim,
                            )
                            Text(
                                text = matchLabel,
                                fontFamily = MonoFamily,
                                fontSize = 9.5.sp,
                                letterSpacing = 1.4.sp,
                                color = Gold.copy(alpha = 0.7f),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = result.matchedRecipe.name,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 0.1.sp,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = result.matchedRecipe.sim.uppercase(),
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.3.sp,
                            color = Gold,
                        )
                        if (result.matchedRecipe.pills.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                result.matchedRecipe.pills.take(5).forEach {
                                    com.ilfforever.fujirecipes.ui.components.Pill(text = it)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Border),
                            ) {
                                coil.compose.AsyncImage(
                                    model = result.localImageUri,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            if (result.isAlreadyRef) {
                                Column {
                                    Text(
                                        text = "ALREADY SAVED",
                                        fontFamily = MonoFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 9.5.sp,
                                        letterSpacing = 1.4.sp,
                                        color = Gold.copy(alpha = 0.75f),
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "This photo is already a reference\nimage for this recipe",
                                        fontFamily = SansFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = com.ilfforever.fujirecipes.ui.theme.TextMuted,
                                    )
                                }
                            } else {
                                Text(
                                    text = "Photo will be attached as a\nreference image",
                                    fontFamily = SansFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = com.ilfforever.fujirecipes.ui.theme.TextMuted,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    com.ilfforever.fujirecipes.ui.components.PrimaryCTA(
                        label = if (result.isAlreadyRef) "Done" else "Add to ${result.matchedRecipe.name}",
                        onClick = {
                            com.ilfforever.fujirecipes.ui.haptics.FujiHaptics.perform(context, view, com.ilfforever.fujirecipes.ui.haptics.FujiHapticEffect.Confirm)
                            if (result.isAlreadyRef) onDismiss() else onConfirm()
                        },
                        busy = false,
                    )
                    if (onScanAnother != null) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onScanAnother)
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (result.isAlreadyRef) "SCAN ANOTHER" else "ADD AND SCAN ANOTHER",
                                fontFamily = MonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.4.sp,
                                color = com.ilfforever.fujirecipes.ui.theme.TextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
