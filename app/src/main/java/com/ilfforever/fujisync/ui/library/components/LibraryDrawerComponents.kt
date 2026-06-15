package com.ilfforever.fujisync.ui.library.components

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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconCamera
import com.ilfforever.fujisync.ui.components.IconEdit
import com.ilfforever.fujisync.ui.components.IconImage
import com.ilfforever.fujisync.ui.components.IconQrCode
import com.ilfforever.fujisync.ui.components.IconScan
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun AddRecipeDrawer(
    onCreateRecipe: () -> Unit,
    onImportFromPhoto: () -> Unit,
    onImportFromScreenshot: () -> Unit,
    onImportFromQr: () -> Unit,
    onScanTileGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        FujiHaptics.perform(context, view, FujiHapticEffect.DrawerSwooshDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.DrawerSwooshOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "add-drawer-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "add-drawer-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LibrarySheetOverlay.copy(alpha = 0.88f * overlayAlpha))
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
                    .offset { IntOffset(0, dragOffset.roundToInt().coerceAtLeast(0)) }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(LibrarySheetBg)
                    .border(1.dp, LibrarySheetBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = {})
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffset > 100.dp.toPx()) dismissWithMotion()
                                else dragOffset = 0f
                            },
                            onDragCancel = { dragOffset = 0f },
                        ) { _, amount -> dragOffset = (dragOffset + amount).coerceAtLeast(0f) }
                    }
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
                Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                    Text(
                        text = "ADD RECIPE",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 0.4.sp,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Add a new recipe to your library.",
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        color = TextDim,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("New recipe", "Build a clean preset from scratch", icon = IconEdit, onClick = onCreateRecipe)
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("From JPEG", "Read EXIF from a camera photo", icon = IconCamera, onClick = onImportFromPhoto)
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("From screenshot", "Extract settings from recipe image", icon = IconImage, onClick = onImportFromScreenshot)
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("QR code", "Import a shared FujiSync recipe", icon = IconQrCode, onClick = onImportFromQr)
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("Scan tile", "Capture from app recipe tile", icon = IconScan, onClick = onScanTileGuide)
            }
        }
    }
}


@Composable
internal fun DrawerOptionRow(
    label: String,
    subtitle: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) TextMuted else TextDim,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = SansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (enabled) TextPrimary else TextDim,
            )
            Text(
                text = subtitle,
                fontFamily = SansFamily,
                fontSize = 12.sp,
                color = TextDim,
            )
        }
        if (!enabled) {
            Text(
                text = "SOON",
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                color = TextDim,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(LibrarySheetControlBg)
                    .border(1.dp, LibrarySheetBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
