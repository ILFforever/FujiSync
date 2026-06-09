package com.paeki.fujirecipes.ui.profile

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.components.FilmSimBadgeImage
import com.paeki.fujirecipes.ui.components.FilmSimLabel
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.GoldFaint
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextPrimary
import kotlinx.coroutines.delay

private const val TOTAL_SLOTS = 7

@Composable
fun SlotReadingAnimationMockup(
    currentSlotIndex: Int,
    loadedSlots: List<RecipeUiModel>,
    isDone: Boolean,
    onDismissed: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(isDone) {
        if (isDone) {
            delay(500)
            visible = false
            delay(200)
            onDismissed()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "slot-pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val animatedProgress by animateFloatAsState(
        targetValue = when {
            isDone -> 1f
            currentSlotIndex < 0 -> 0f
            else -> loadedSlots.size / TOTAL_SLOTS.toFloat()
        },
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "progress",
    )

    // The content eases in/out, but the backdrop is mounted instantly (no fade)
    // the whole time the overlay is up. Cross-fading the backdrop in would ramp
    // it up from transparent and briefly reveal the camera image behind it — so
    // we snap the backdrop in and only animate the content on top of it.
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "reading-content-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.97f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha }
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
                Spacer(Modifier.height(96.dp))

                Text(
                    text = if (isDone) "SYNC COMPLETE" else "READING FROM CAMERA",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = Gold,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "C1 — C7",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = 2.sp,
                    color = TextPrimary,
                )

                Spacer(Modifier.height(36.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    repeat(TOTAL_SLOTS) { index ->
                        val recipe = loadedSlots.getOrNull(index)
                        SlotReadingCard(
                            slotLabel = "C${index + 1}",
                            sim = recipe?.sim,
                            name = recipe?.name,
                            isActive = !isDone && index == currentSlotIndex,
                            isRead = index < loadedSlots.size,
                            pulse = pulse,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border.copy(alpha = 0.4f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(1.dp)
                            .background(Brush.horizontalGradient(listOf(GoldFaint, Gold))),
                    )
                }

                Spacer(Modifier.height(40.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.25f to GoldFaint,
                                0.75f to GoldFaint,
                                1f to Color.Transparent,
                            )
                        ),
                )

                Spacer(Modifier.height(32.dp))

                // Show the most recently loaded slot; nothing until at least one is done
                val displaySlot = if (isDone) loadedSlots.lastOrNull() else loadedSlots.lastOrNull()
                if (displaySlot != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = displaySlot.slot,
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = Gold,
                        )
                        FilmSimLabel(sim = displaySlot.sim)
                        Text(
                            text = displaySlot.name.uppercase(),
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.6.sp,
                            color = TextPrimary,
                        )
                    }
                }
            }
        }
    }

@Composable
private fun SlotReadingCard(
    slotLabel: String,
    sim: String?,
    name: String?,
    isActive: Boolean,
    isRead: Boolean,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.07f else 1f,
        animationSpec = tween(130, easing = FastOutSlowInEasing),
        label = "card-scale",
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isRead || isActive) 1f else 0.22f,
        animationSpec = tween(130),
        label = "card-alpha",
    )

    val bgAlpha by animateFloatAsState(
        targetValue = when {
            isActive -> 0.09f
            isRead -> 0.04f
            else -> 0f
        },
        animationSpec = tween(130),
        label = "card-bg",
    )

    val borderColor = when {
        isActive -> Gold.copy(alpha = pulse)
        isRead -> GoldFaint
        else -> Border.copy(alpha = 0.45f)
    }

    val loadedAlpha by animateFloatAsState(
        targetValue = if (isRead) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "loaded-alpha",
    )

    Box(
        modifier = modifier
            .height(78.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = slotLabel,
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp,
                color = Gold.copy(alpha = contentAlpha),
            )
            if (sim != null) {
                FilmSimBadgeImage(
                    sim = sim,
                    size = 22.dp,
                    modifier = Modifier.graphicsLayer { alpha = loadedAlpha },
                )
            } else {
                Spacer(Modifier.size(22.dp))
            }
            Text(
                text = if (isRead && name != null) name else "",
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 7.5.sp,
                letterSpacing = 0.2.sp,
                color = TextPrimary.copy(alpha = loadedAlpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = loadedAlpha },
            )
        }

        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Gold.copy(alpha = pulse), Color.Transparent)
                        )
                    ),
            )
        }
    }
}
