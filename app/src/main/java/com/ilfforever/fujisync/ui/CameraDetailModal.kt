package com.ilfforever.fujisync.ui

import android.animation.ValueAnimator
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.camera.CameraCardUiModel
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun BoxScope.CameraDetailModal(
    camera: CameraCardUiModel,
    name: String,
    onRename: (String) -> Unit,
    onClose: () -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    var editing by remember { mutableStateOf(false) }
    var draft by remember(name) { mutableStateOf(name) }
    val modalInteraction = remember { MutableInteractionSource() }

    fun dismissWithMotion() {
        if (!motionEnabled) { onClose(); return }
        scope.launch { visible = false; delay(220); onClose() }
    }

    fun commitRename() {
        val trimmed = draft.trim().ifBlank { "My Camera" }
        onRename(trimmed)
        draft = trimmed
        editing = false
    }

    LaunchedEffect(motionEnabled) { visible = true }
    androidx.activity.compose.BackHandler(enabled = true) { dismissWithMotion() }

    val overlayTransition = updateTransition(targetState = visible, label = "camera-detail-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 160 else 120, easing = FastOutSlowInEasing) },
        label = "camera-detail-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = 0.62f * overlayAlpha))
            .clickable(onClick = ::dismissWithMotion),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 2 },
            exit  = fadeOut(tween(110, easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CameraModalBg)
                .border(1.dp, CameraModalBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(interactionSource = modalInteraction, indication = null, onClick = {})
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Drag zone — pill + header combined so dragging anywhere in this area dismisses
            var swipeDy by remember { mutableStateOf(0f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { swipeDy = 0f },
                            onDrag = { change, amount -> change.consume(); swipeDy += amount.y },
                            onDragEnd = { if (swipeDy > 60f) dismissWithMotion() else swipeDy = 0f },
                            onDragCancel = { swipeDy = 0f },
                        )
                    },
            ) {
                // Pill — visual only, gesture is on the parent Column
                Box(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(TextDim.copy(alpha = 0.55f)),
                    )
                }

                // Header — model ID + custom name + DONE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 16.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            text = camera.model.uppercase(),
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp,
                            color = Gold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = name,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.2).sp,
                            color = TextPrimary,
                        )
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
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
            }

            // Rename row
            Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                Text(
                    text = "LABEL",
                    fontFamily = MonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 1.8.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                if (editing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TextPrimary,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CameraModalControlBg)
                                .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.Enter -> { commitRename(); true }
                                        Key.Escape -> { draft = name; editing = false; true }
                                        else -> false
                                    }
                                }
                                .padding(horizontal = 13.dp, vertical = 11.dp),
                        )
                        Text(
                            text = "SAVE",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.3.sp,
                            color = if (draft.trim().isNotBlank()) Gold else TextDim,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(enabled = draft.trim().isNotBlank(), onClick = ::commitRename)
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CameraModalControlBg)
                            .border(1.dp, CameraModalBorder, RoundedCornerShape(10.dp))
                            .clickable { editing = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = name,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                        )
                        Text(
                            text = "RENAME",
                            fontFamily = MonoFamily,
                            fontSize = 9.sp,
                            letterSpacing = 1.6.sp,
                            color = Gold,
                        )
                    }
                }
            }

            // Stats card
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CameraModalControlBg)
                    .border(1.dp, CameraModalBorder, RoundedCornerShape(14.dp)),
            ) {
                CameraStatRow("Firmware", camera.firmware, divider = true)
                CameraStatRow("Battery", camera.battery, divider = true)
                CameraStatRow("USB ID", camera.usbId.ifBlank { "—" }, divider = false, valueMonospace = true)
            }

            Text(
                text = "To disconnect, unplug the USB cable.",
                fontFamily = SansFamily,
                fontSize = 11.5.sp,
                color = TextDim,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
            )
        }
        } // AnimatedVisibility
    }
}

@Composable
private fun CameraStatRow(label: String, value: String, divider: Boolean, valueMonospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontFamily = SansFamily,
            fontSize = 13.sp,
            color = TextMuted,
        )
        Text(
            text = value,
            fontFamily = if (valueMonospace) MonoFamily else SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (valueMonospace) 11.sp else 13.sp,
            letterSpacing = if (valueMonospace) 0.6.sp else 0.sp,
            color = TextPrimary,
        )
    }
    if (divider) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CameraModalBorder))
    }
}

private val CameraModalBg = Color(0xFF0D0C0B)
private val CameraModalBorder = Color(0xFF232018)
private val CameraModalControlBg = Color(0xFF161411)
