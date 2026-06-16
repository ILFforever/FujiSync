package com.ilfforever.fujisync.ui.profile

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
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ilfforever.fujisync.R
import com.ilfforever.fujisync.ui.camera.cameraImageTuning
import com.ilfforever.fujisync.ui.components.DeleteConfirmDialog
import com.ilfforever.fujisync.ui.components.IconEdit
import com.ilfforever.fujisync.ui.components.IconMoreVertical
import com.ilfforever.fujisync.ui.components.IconRefresh
import com.ilfforever.fujisync.ui.components.IconTrash
import com.ilfforever.fujisync.ui.components.MetaRow
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.Bg
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

@Composable
internal fun MyCamerasScreen(
    cameraLabels: Map<String, String>,
    cameraModels: Map<String, String>,
    cameraFirmwares: Map<String, String>,
    activeCameraSerial: String,
    activeCameraModel: String,
    onRenameCameraLabel: (String, String) -> Unit,
    onDeleteCamera: (String) -> Unit,
    onResetCameraLabel: (String) -> Unit,
    onBack: () -> Unit,
) {
    var menuSerial by remember { mutableStateOf<String?>(null) }
    var renamingSerial by remember { mutableStateOf<String?>(null) }
    var deletingSerial by remember { mutableStateOf<String?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    BackHandler(enabled = menuSerial != null) { menuSerial = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Gold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
                )
                Text(
                    text = "MY CAMERAS",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.8.sp,
                    color = TextPrimary,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (cameraLabels.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PanelLow)
                            .border(1.dp, Border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                    ) {
                        Text(
                            text = "No cameras registered yet. Connect a camera via USB to register it here.",
                            fontFamily = SansFamily,
                            fontSize = 14.sp,
                            color = TextMuted,
                        )
                    }
                } else {
                    Spacer(Modifier.height(4.dp))
                    cameraLabels.entries.forEach { (serial, name) ->
                        val model = cameraModels[serial]
                            ?: if (serial == activeCameraSerial && activeCameraModel.isNotBlank()) activeCameraModel else null
                        val firmware = cameraFirmwares[serial]
                        CameraCard(
                            name = name,
                            model = model,
                            firmware = firmware,
                            serial = serial,
                            onOpenMenu = { menuSerial = serial },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        menuSerial?.let { serial ->
            val name = cameraLabels[serial] ?: return@let
            CameraActionSheet(
                name = name,
                onRename = {
                    renameDraft = name
                    renamingSerial = serial
                    menuSerial = null
                },
                onReset = {
                    onResetCameraLabel(serial)
                    menuSerial = null
                },
                onDelete = {
                    deletingSerial = serial
                    menuSerial = null
                },
                onDismiss = { menuSerial = null },
            )
        }

        renamingSerial?.let { serial ->
            val currentName = cameraLabels[serial] ?: return@let
            fun commit() {
                val trimmed = renameDraft.trim().ifBlank { currentName }
                onRenameCameraLabel(serial, trimmed)
                renamingSerial = null
            }
            Dialog(onDismissRequest = { renamingSerial = null; renameDraft = "" }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                ) {
                    Text("RENAME", fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.8.sp, color = Gold.copy(alpha = 0.74f))
                    Spacer(Modifier.height(6.dp))
                    Text("Camera nickname", fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    BasicTextField(
                        value = renameDraft,
                        onValueChange = { renameDraft = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary),
                        cursorBrush = SolidColor(TextPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Bg)
                            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                                when (event.key) {
                                    Key.Enter -> { commit(); true }
                                    Key.Escape -> { renamingSerial = null; true }
                                    else -> false
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Bg).border(1.dp, Border, RoundedCornerShape(10.dp)).clickable { renamingSerial = null }.padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("CANCEL", fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 1.4.sp, color = TextDim)
                        }
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Gold.copy(alpha = 0.12f)).border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(10.dp)).clickable(enabled = renameDraft.trim().isNotBlank(), onClick = ::commit).padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("SAVE", fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 1.4.sp, color = if (renameDraft.trim().isNotBlank()) Gold else TextDim)
                        }
                    }
                }
            }
        }

        deletingSerial?.let { serial ->
            val name = cameraLabels[serial] ?: return@let
            DeleteConfirmDialog(
                title = "Remove “$name”?",
                body = "This removes the camera from your registered cameras. Library recipes and slot backups are not affected.",
                confirmLabel = "Remove camera",
                eyebrow = "REMOVE",
                onConfirm = { onDeleteCamera(serial) },
                onDismiss = { deletingSerial = null },
            )
        }
    }
}

@Composable
private fun CameraCard(
    name: String,
    model: String?,
    firmware: String?,
    serial: String,
    onOpenMenu: () -> Unit,
) {
    val displayModel = model ?: name
    val drawableRes = cameraDrawableRes(displayModel) ?: cameraDrawableRes(name)
    val tuning = cameraImageTuning(displayModel)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(166.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(18.dp)),
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .wrapContentSize(unbounded = true, align = Alignment.TopEnd)
                    .requiredWidth(tuning.width)
                    .requiredHeight(tuning.height)
                    .offset(x = tuning.offsetX, y = tuning.offsetY)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(0f to Color.Transparent, 0.35f to Color.Black, 1f to Color.Black),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp)) {
            SectionLabel(text = name)
            Spacer(Modifier.height(8.dp))
            Text(text = displayModel, fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = (-0.5).sp, color = TextPrimary, lineHeight = 44.sp)
            Spacer(Modifier.height(15.dp))
            if (!firmware.isNullOrBlank()) {
                MetaRow("Firmware", firmware)
                Spacer(Modifier.height(8.dp))
            }
            MetaRow("USB ID", serial)
        }

        // Three-dots button anchored top-end
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(40.dp)
                .clip(RoundedCornerShape(bottomStart = 10.dp))
                .clickable(onClick = onOpenMenu),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = IconMoreVertical,
                contentDescription = "Options",
                tint = TextDim,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun cameraDrawableRes(model: String): Int? = when {
    model.contains("X-T30 III", ignoreCase = true) || model.contains("XT30III", ignoreCase = true) -> R.drawable.camera_xt30iii
    model.contains("X-T50",  ignoreCase = true) || model.contains("XT50",  ignoreCase = true) -> R.drawable.camera_xt50
    model.contains("X-H2",   ignoreCase = true) || model.contains("XH2",   ignoreCase = true) -> R.drawable.camera_xh2
    model.contains("X-T5",   ignoreCase = true) || model.contains("XT5",   ignoreCase = true) -> R.drawable.camera_xt5
    model.contains("X-S20",  ignoreCase = true) || model.contains("XS20",  ignoreCase = true) -> R.drawable.camera_xs20
    model.contains("X-M5",   ignoreCase = true) || model.contains("XM5",   ignoreCase = true) -> R.drawable.camera_xm5
    model.contains("X-E5",   ignoreCase = true) || model.contains("XE5",   ignoreCase = true) -> R.drawable.camera_xe5
    model.contains("X100VI", ignoreCase = true) -> R.drawable.camera_x100vi
    model.contains("X-Pro3", ignoreCase = true) || model.contains("XPro3", ignoreCase = true) -> R.drawable.camera_xpro3
    else -> null
}

@Composable
private fun CameraActionSheet(
    name: String,
    onRename: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val motionEnabled = android.animation.ValueAnimator.areAnimatorsEnabled()
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

    val overlayTransition = updateTransition(targetState = visible, label = "action-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "action-overlay-alpha",
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
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp)
                        .width(38.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(TextDim.copy(alpha = 0.55f)),
                )
                // Header
                Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                    Text(
                        text = "CAMERA",
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        color = Gold.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = name,
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.2).sp,
                        color = TextPrimary,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
                ActionSheetRow("Rename", icon = IconEdit, onClick = { onRename() })
                Box(Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
                ActionSheetRow("Reset name", icon = IconRefresh, onClick = { onReset() })
                Box(Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
                ActionSheetRow("Remove camera", icon = IconTrash, color = Color(0xFFE05A4F), onClick = { onDelete() })
                Box(Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
            }
        }
    }
}

@Composable
private fun ActionSheetRow(
    label: String,
    icon: ImageVector? = null,
    color: Color = TextPrimary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = if (color == TextPrimary) 0.55f else 0.8f),
                modifier = Modifier.size(17.dp),
            )
        }
        Text(
            text = label,
            fontFamily = SansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = color,
        )
    }
}
