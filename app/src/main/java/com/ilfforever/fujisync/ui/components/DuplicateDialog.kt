package com.ilfforever.fujirecipes.ui.components

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujirecipes.ui.haptics.FujiHapticEffect
import com.ilfforever.fujirecipes.ui.haptics.FujiHaptics
import com.ilfforever.fujirecipes.ui.model.DuplicateDialogState
import com.ilfforever.fujirecipes.ui.model.DuplicateMatchKind
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.SheetBg
import com.ilfforever.fujirecipes.ui.theme.SheetBorder
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DuplicateDialog(
    dialog: DuplicateDialogState,
    onSaveAsNew: () -> Unit,
    onUpdateExisting: () -> Unit,
    onDismiss: () -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)

    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "duplicate-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "duplicate-overlay-alpha",
    ) { if (it) 1f else 0f }

    val match = dialog.topMatch
    val incoming = dialog.incomingRecipe
    val existing = match.libraryRecipe

    val (title, subtitle) = when (match.kind) {
        DuplicateMatchKind.ExactSettings -> "EXACT MATCH" to "These settings already exist in your library."
        DuplicateMatchKind.SameName -> "NAME CONFLICT" to "A recipe with this name is already saved."
        DuplicateMatchKind.Similar -> "POSSIBLE DUPLICATE" to "A similar recipe was found in your library."
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = 1.1.sp,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = subtitle,
                                fontFamily = SansFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = TextMuted,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "CANCEL",
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

                    Spacer(Modifier.height(18.dp))

                    DuplicateMatchCard(
                        label = "IN LIBRARY",
                        name = existing.name,
                        sim = existing.sim,
                        pills = existing.pills,
                    )

                    if (match.kind == DuplicateMatchKind.ExactSettings &&
                        incoming.name.trim().lowercase() != existing.name.trim().lowercase()
                    ) {
                        Spacer(Modifier.height(8.dp))
                        DuplicateMatchCard(
                            label = "FROM CAMERA",
                            name = incoming.name,
                            sim = incoming.sim,
                            pills = incoming.pills,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    PrimaryCTA(
                        label = "Update Existing",
                        onClick = {
                            FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                            onUpdateExisting()
                        },
                        busy = false,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, SheetBorder, RoundedCornerShape(14.dp))
                            .clickable(onClick = {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                                onSaveAsNew()
                            })
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "SAVE AS NEW",
                            fontFamily = MonoFamily,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.6.sp,
                            color = TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuplicateMatchCard(
    label: String,
    name: String,
    sim: String,
    pills: List<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DuplicateCardBg)
            .border(1.dp, SheetBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            text = label,
            fontFamily = MonoFamily,
            fontSize = 9.5.sp,
            letterSpacing = 1.6.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 0.1.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = sim.uppercase(),
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 1.3.sp,
            color = Gold,
        )
        if (pills.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                pills.take(5).forEach { Pill(text = it) }
            }
        }
    }
}

private val DuplicateCardBg = Color(0xFF141210)
