package com.ilfforever.fujisync.ui.library.components

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.emoji2.emojipicker.EmojiPickerView
import com.ilfforever.fujisync.ui.components.BlurredImagePlaceholder
import com.ilfforever.fujisync.ui.components.DeleteConfirmDialog
import com.ilfforever.fujisync.ui.components.IconMoreVertical
import com.ilfforever.fujisync.ui.components.IconPlus
import com.ilfforever.fujisync.ui.components.ImageLoadState
import com.ilfforever.fujisync.ui.components.decodeSampledBitmap
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.model.LibraryGroupStyle
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun LibraryGroupCard(
    label: String,
    count: Int,
    style: LibraryGroupStyle,
    entranceIndex: Int,
    entranceRun: Int,
    alreadyEntered: Boolean = false,
    onEntered: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onOpenEditor: () -> Unit,
) {
    val context = LocalContext.current
    val imageState by produceState(ImageLoadState(null, null), style.imageUri) {
        val uri = style.imageUri?.let { Uri.parse(it) } ?: return@produceState
        val preview = withContext(Dispatchers.IO) { decodeSampledBitmap(context, uri, maxPx = 16) }
        value = ImageLoadState(preview = preview, full = null)
        val full = withContext(Dispatchers.IO) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                val targetPx = (context.resources.displayMetrics.density * 320).toInt().coerceAtLeast(1)
                opts.inSampleSize = generateSequence(1) { it * 2 }
                    .first { it * targetPx >= opts.outWidth.coerceAtLeast(1) }
                opts.inJustDecodeBounds = false
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                    ?.asImageBitmap()
            }.getOrNull()
        }
        value = ImageLoadState(preview = preview, full = full)
    }
    val accent = groupAccent(style.color)
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    var entered by remember(entranceRun) { mutableStateOf(!motionEnabled || alreadyEntered) }
    LaunchedEffect(entranceRun, motionEnabled, alreadyEntered) {
        if (!entered) {
            kotlinx.coroutines.delay((entranceIndex * 55L).coerceAtMost(220L))
            entered = true
        }
        onEntered()
    }
    val entranceProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "library-folder-entrance",
    )

    Column(
        modifier = modifier
            .height(156.dp)
            .graphicsLayer {
                if (motionEnabled) {
                    alpha = entranceProgress
                    translationY = (1f - entranceProgress) * 22.dp.toPx()
                }
            }
            .clip(RoundedCornerShape(14.dp))
            .background(PanelGroupBg)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .background(accent.copy(alpha = 0.18f)),
        ) {
            if (style.imageUri != null) {
                BlurredImagePlaceholder(
                    state = imageState,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // dim overlay so text below stays readable
                val overlayAlpha by animateFloatAsState(
                    targetValue = if (imageState.full != null) 0.28f else 0f,
                    animationSpec = tween(380, easing = FastOutSlowInEasing),
                    label = "group-overlay-alpha",
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = overlayAlpha)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Bg.copy(alpha = 0.72f))
                        .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = style.icon, fontSize = 22.sp, lineHeight = 22.sp)
                }
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label.uppercase(),
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.2.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    IconMoreVertical,
                    contentDescription = "Edit group",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenEditor)
                        .padding(4.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$count RECIPES",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = TextDim,
            )
        }
    }
}

@Composable
internal fun CreateLibraryGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val context = LocalContext.current
    val view = LocalView.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 238.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PanelGroupBg)
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .imePadding()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "NEW FOLDER",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            color = TextDim,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "CREATE FOLDER",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            letterSpacing = 0.6.sp,
                            color = TextPrimary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.14f))
                            .border(1.dp, Gold.copy(alpha = 0.48f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(IconPlus, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(28.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Bg)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
                        textStyle = TextStyle(
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (draft.isBlank()) {
                                Text(
                                    text = "Folder name",
                                    fontFamily = SansFamily,
                                    fontSize = 15.sp,
                                    color = TextDim,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "CANCEL",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.3.sp,
                    color = TextDim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
                            onDismiss()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "CREATE",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.3.sp,
                    color = if (draft.isBlank()) TextDim else Gold,
                    modifier = Modifier.clickable(enabled = draft.isNotBlank()) {
                        FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                        onCreate(draft)
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun LibraryGroupEditorSheet(
    group: String,
    count: Int,
    style: LibraryGroupStyle,
    canRename: Boolean = true,
    canDelete: Boolean = true,
    onChangeStyle: (LibraryGroupStyle) -> Unit,
    onRename: (String) -> Unit,
    onAddImage: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = groupAccent(style.color)
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    var nameDraft by remember(group) { mutableStateOf(group) }
    var pendingDeleteGroup by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) {
            onDismiss()
            return
        }
        scope.launch {
            visible = false
            delay(180)
            onDismiss()
        }
    }

    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "library-editor-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(durationMillis = if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "library-editor-overlay-alpha",
    ) { isVisible -> if (isVisible) 1f else 0f }

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
                    .heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(LibrarySheetBg)
                    .border(1.dp, LibrarySheetBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = {})
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                // Pinned drag handle — outside the scroll
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp)
                        .width(38.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(TextDim.copy(alpha = 0.55f)),
                )
                // Scrollable content below
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 14.dp),
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = group.uppercase(),
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            letterSpacing = 1.1.sp,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$count RECIPES",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = TextDim,
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
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
                if (canRename) {
                    Spacer(Modifier.height(20.dp))
                    SheetSectionLabel("NAME")
                    SheetNameRow(
                        value = nameDraft,
                        originalValue = group,
                        color = accent,
                        onValueChange = { nameDraft = it },
                        onSave = {
                            FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                            onRename(nameDraft)
                        },
                    )
                    Spacer(Modifier.height(18.dp))
                } else {
                    Spacer(Modifier.height(20.dp))
                }
                SheetSectionLabel("IDENTITY")
                SheetIdentityRow(
                    icon = style.icon,
                    hasCover = style.imageUri != null,
                    color = accent,
                    onIconChange = { if (it.isNotEmpty()) onChangeStyle(style.copy(icon = it)) },
                    onChooseImage = onAddImage,
                    onRemoveImage = {
                        if (style.imageUri != null) onChangeStyle(style.copy(imageUri = null))
                    },
                )
                Spacer(Modifier.height(18.dp))
                SheetSectionLabel("ACCENT")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GroupColor.entries.forEach { color ->
                        SheetColorChoice(
                            color = color.value,
                            active = style.color == color.name,
                            onClick = {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                                onChangeStyle(style.copy(color = color.name))
                            },
                        )
                    }
                }
                if (canDelete) {
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "DELETE GROUP",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = Color(0xFFD45B4A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LibrarySheetControlBg)
                            .border(1.dp, Color(0xFFD45B4A).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Reject)
                                pendingDeleteGroup = true
                            }
                            .padding(vertical = 13.dp),
                    )
                }
                } // inner scrollable Column
            }
        }
    }

    if (pendingDeleteGroup) {
        DeleteConfirmDialog(
            title = group,
            body = "This group will be deleted. Recipes inside will not be removed from your library.",
            confirmLabel = "Delete Group",
            onConfirm = onDelete,
            onDismiss = { pendingDeleteGroup = false },
        )
    }
}
