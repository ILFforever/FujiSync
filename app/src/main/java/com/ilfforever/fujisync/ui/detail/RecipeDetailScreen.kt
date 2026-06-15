package com.ilfforever.fujirecipes.ui.detail

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import com.ilfforever.fujirecipes.ui.components.IconClose
import com.ilfforever.fujirecipes.ui.components.IconEdit
import com.ilfforever.fujirecipes.ui.components.IconMore
import com.ilfforever.fujirecipes.ui.components.IconQrCode
import com.ilfforever.fujirecipes.ui.components.IconStar
import com.ilfforever.fujirecipes.ui.components.IconStarFilled
import com.ilfforever.fujirecipes.ui.components.IconTrash
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import com.ilfforever.fujirecipes.ui.haptics.FujiHapticEffect
import com.ilfforever.fujirecipes.ui.haptics.FujiHaptics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import com.ilfforever.fujirecipes.ui.components.DeleteConfirmDialog
import com.ilfforever.fujirecipes.ui.components.decodeSampledBitmap
import com.ilfforever.fujirecipes.ui.components.FilmSimLabel
import com.ilfforever.fujirecipes.ui.components.IconCheck
import com.ilfforever.fujirecipes.ui.components.IconCopy
import com.ilfforever.fujirecipes.ui.components.Pill
import com.ilfforever.fujirecipes.ui.components.PrimaryCTA
import com.ilfforever.fujirecipes.ui.components.PropRow
import com.ilfforever.fujirecipes.ui.components.recipePropertyRows
import com.ilfforever.fujirecipes.ui.components.BlurredImagePlaceholder
import com.ilfforever.fujirecipes.ui.components.ImageLoadState
import com.ilfforever.fujirecipes.ui.components.SectionLabel
import com.ilfforever.fujirecipes.ui.components.shimmerBrush
import com.ilfforever.fujirecipes.data.qr.RecipeQr
import com.ilfforever.fujirecipes.ui.model.RecipeUiModel
import com.ilfforever.fujirecipes.ui.model.sourceCameraDisplayName
import com.ilfforever.fujirecipes.ui.theme.Bg
import com.ilfforever.fujirecipes.ui.theme.Border
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelHigh
import com.ilfforever.fujirecipes.ui.theme.PanelLow
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(
    recipe: RecipeUiModel?,
    connected: Boolean,
    onClose: () -> Unit,
    onWrite: () -> Unit,
    onAddReferenceImage: (RecipeUiModel) -> Unit,
    onRemoveReferenceImage: (RecipeUiModel, String) -> Unit,
    onReorderReferenceImages: (RecipeUiModel, Int, Int) -> Unit = { _, _, _ -> },
    onToggleFavorite: (RecipeUiModel) -> Unit,
    onEdit: (RecipeUiModel) -> Unit,
    onClone: (RecipeUiModel) -> Unit,
    onDelete: (RecipeUiModel) -> Unit,
    writeBusy: Boolean,
    cameraModel: String = "",
    cameraName: String = "",
    cameraSlots: List<RecipeUiModel> = emptyList(),
    onWriteToSlot: ((String) -> Unit)? = null,
    interactionsEnabled: Boolean = true,
    showReferenceImageBlur: Boolean = true,
    maxReferenceImages: Int = 20,
) {
    var expandedImageIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDelete by remember { mutableStateOf(false) }
    var pendingConfirmWrite by remember { mutableStateOf(false) }
    var slotPickerOpen by remember { mutableStateOf(false) }
    var qrRecipe by remember { mutableStateOf<RecipeUiModel?>(null) }
    var pullDistance by remember { mutableStateOf(0f) }
    var displayedRecipe by remember { mutableStateOf<RecipeUiModel?>(null) }
    val scrollState = rememberScrollState()

    val focusManager = LocalFocusManager.current
    LaunchedEffect(recipe) {
        if (recipe != null) {
            displayedRecipe = recipe
            pullDistance = 0f
            focusManager.clearFocus()
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If user pulls down and we are at the top, consume the delta to track pull distance
                if (available.y > 0 && scrollState.value == 0) {
                    pullDistance += available.y
                    return available
                }
                // If user pushes up while we have pull distance, reduce it
                if (available.y < 0 && pullDistance > 0) {
                    val consumed = available.y.coerceAtLeast(-pullDistance)
                    pullDistance += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance > 400f) { // Threshold to close
                    onClose()
                    return Velocity.Zero
                }
                pullDistance = 0f
                return Velocity.Zero
            }
            
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < 0 && pullDistance > 0) {
                   pullDistance = 0f
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    AnimatedVisibility(
        visible = recipe != null,
        enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(300)) + fadeIn(tween(200)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(tween(150)),
    ) {
        val visibleRecipe = displayedRecipe ?: return@AnimatedVisibility
        val context = LocalContext.current
        val view = LocalView.current
        fun haptic() = FujiHaptics.perform(context, view, FujiHapticEffect.SoftConfirm)

        BackHandler(enabled = interactionsEnabled, onBack = onClose)

        LaunchedEffect(interactionsEnabled) {
            if (!interactionsEnabled) {
                pendingDelete = false
                pendingConfirmWrite = false
                slotPickerOpen = false
                qrRecipe = null
                expandedImageIndex = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .nestedScroll(nestedScrollConnection),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .graphicsLayer { translationY = pullDistance }
            ) {
                // Top nav bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(enabled = interactionsEnabled, onClick = onClose)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(IconClose, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "CLOSE",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.8.sp,
                            color = TextPrimary,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var moreExpanded by remember { mutableStateOf(false) }
                        LaunchedEffect(interactionsEnabled) {
                            if (!interactionsEnabled) moreExpanded = false
                        }
                        IconButton(
                            onClick = { haptic(); onToggleFavorite(visibleRecipe) },
                            enabled = interactionsEnabled,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                if (visibleRecipe.favorite) IconStarFilled else IconStar,
                                contentDescription = if (visibleRecipe.favorite) "Remove favorite" else "Add favorite",
                                tint = if (visibleRecipe.favorite) Gold else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { haptic(); onEdit(visibleRecipe) },
                            enabled = interactionsEnabled,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(IconEdit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                        Box {
                            IconButton(
                                onClick = { haptic(); moreExpanded = true },
                                enabled = interactionsEnabled,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(IconMore, contentDescription = "More", tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(
                                expanded = interactionsEnabled && moreExpanded,
                                onDismissRequest = { moreExpanded = false },
                                offset = DpOffset(x = 0.dp, y = 4.dp),
                                containerColor = PanelHigh,
                                tonalElevation = 0.dp,
                                shadowElevation = 12.dp,
                                border = BorderStroke(1.dp, Border),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                @Composable
                                fun MenuItem(
                                    label: String,
                                    icon: androidx.compose.ui.graphics.vector.ImageVector,
                                    tint: Color = TextDim,
                                    labelColor: Color = TextPrimary,
                                    onClick: () -> Unit,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable(enabled = interactionsEnabled, onClick = onClick)
                                            .padding(horizontal = 16.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
                                        Text(label, fontFamily = SansFamily, fontSize = 13.sp, color = labelColor)
                                    }
                                }

                                MenuItem("Clone", IconCopy, onClick = { haptic(); moreExpanded = false; onClone(visibleRecipe) })
                                MenuItem("QR code", IconQrCode, onClick = { haptic(); moreExpanded = false; qrRecipe = visibleRecipe })
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                                MenuItem(
                                    label = "Delete",
                                    icon = IconTrash,
                                    tint = Color(0xFFE05252),
                                    labelColor = Color(0xFFE05252),
                                    onClick = { moreExpanded = false; pendingDelete = true },
                                )
                            }
                        }
                    }
                }

                // Scrollable content
                Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                ) {
                    // Hero card
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PanelLow)
                            .border(1.dp, Border, RoundedCornerShape(16.dp))
                            .padding(horizontal = 22.dp)
                            .padding(top = 14.dp, bottom = 20.dp),
                    ) {
                        FilmSimLabel(sim = visibleRecipe.sim, imageSize = 28.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = visibleRecipe.name,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            letterSpacing = 0.1.sp,
                            color = TextPrimary,
                            lineHeight = 38.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            visibleRecipe.pills.take(3).forEach { Pill(text = it, large = true) }
                        }
                        RecipeReferenceImage(
                            referenceImageUris = visibleRecipe.referenceImageUris,
                            maxReferenceImages = maxReferenceImages,
                            onAddReferenceImage = { onAddReferenceImage(visibleRecipe) },
                            onRemoveReferenceImage = { uri -> onRemoveReferenceImage(visibleRecipe, uri) },
                            onReorderReferenceImages = { from, to -> onReorderReferenceImages(visibleRecipe, from, to) },
                            onExpandImage = { index -> expandedImageIndex = index },
                            interactionsEnabled = interactionsEnabled,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = visibleRecipe.description.takeIf { it.isNotEmpty() } ?: "No description given.",
                            fontFamily = SansFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = if (visibleRecipe.description.isNotEmpty()) TextMuted else TextDim,
                        )
                    }

                    // Property sections
                    Spacer(Modifier.height(4.dp))
                    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                        PropSectionDetail("Effects", visibleRecipe.effects)
                        PropSectionDetail("Tone", visibleRecipe.tone)
                        PropSectionDetail("White Balance", visibleRecipe.wb)
                        ShootingSettingsSection(visibleRecipe)
                        SavedFromSection(visibleRecipe)
                    }

                    // Slot selector (only for camera slots)
                    if (visibleRecipe.slot.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                            SectionLabel(
                                text = "Install to slot",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf("C1", "C2", "C3", "C4", "C5", "C6", "C7").forEach { slot ->
                                    val isCurrentSlot = slot == visibleRecipe.slot
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCurrentSlot) Gold else PanelHigh)
                                            .border(1.dp, if (isCurrentSlot) Gold else Border, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = slot,
                                            fontFamily = MonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.4.sp,
                                            color = if (isCurrentSlot) Bg else TextMuted,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    Spacer(Modifier.height(100.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Bg))
                        ),
                )
                } // Box

                // Sticky CTA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Bg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Border),
                    )
                    Spacer(Modifier.height(12.dp))
                    val isLibraryRecipe = visibleRecipe.slot.isEmpty() && onWriteToSlot != null
                    val ctaLabel = when {
                        writeBusy -> "Writing…"
                        isLibraryRecipe && !connected -> "Connect Camera to Sync"
                        isLibraryRecipe -> "Sync to Camera"
                        connected -> "Write to ${visibleRecipe.slot.ifEmpty { "C1" }}"
                        else -> "Save to Library"
                    }
                    PrimaryCTA(
                        label = ctaLabel,
                        onClick = if (isLibraryRecipe) { { slotPickerOpen = true } } else { { pendingConfirmWrite = true } },
                        busy = writeBusy,
                        enabled = interactionsEnabled && if (isLibraryRecipe) (connected && !writeBusy) else (connected || visibleRecipe.slot.isEmpty()),
                    )
                }
            }

            if (slotPickerOpen && onWriteToSlot != null) {
                SyncToCameraSheet(
                    connected = connected,
                    cameraModel = cameraModel,
                    cameraSlots = cameraSlots,
                    recipeName = visibleRecipe.name,
                    writeBusy = writeBusy,
                    onDismiss = { slotPickerOpen = false },
                    onWriteToSlot = { slot ->
                        slotPickerOpen = false
                        onWriteToSlot(slot)
                    },
                )
            }

            if (pendingConfirmWrite) {
                DeleteConfirmDialog(
                    eyebrow = "WRITE TO CAMERA",
                    title = visibleRecipe.slot,
                    body = "\"${visibleRecipe.name}\" will overwrite the current recipe in ${visibleRecipe.slot}.",
                    confirmLabel = "Write to ${visibleRecipe.slot}",
                    confirmColor = Gold,
                    onConfirm = onWrite,
                    onDismiss = { pendingConfirmWrite = false },
                )
            }

            // Fullscreen image lightbox
            expandedImageIndex?.let { startIndex ->
                ReferenceImageLightbox(
                    imageUris = visibleRecipe.referenceImageUris,
                    startIndex = startIndex,
                    blurEnabled = showReferenceImageBlur,
                    onDismiss = { expandedImageIndex = null },
                )
            }

            qrRecipe?.let { recipeForQr ->
                RecipeQrSheet(
                    recipe = recipeForQr,
                    onDismiss = { qrRecipe = null },
                )
            }

            if (pendingDelete) {
                DeleteConfirmDialog(
                    title = visibleRecipe.name,
                    body = "This recipe will be permanently removed from your library.",
                    confirmLabel = "Delete",
                    onConfirm = { onDelete(visibleRecipe) },
                    onDismiss = { pendingDelete = false },
                )
            }
        }
    }
}

@Composable
private fun RecipeReferenceImage(
    referenceImageUris: List<String>,
    maxReferenceImages: Int,
    onAddReferenceImage: () -> Unit,
    onRemoveReferenceImage: (String) -> Unit,
    onReorderReferenceImages: (Int, Int) -> Unit = { _, _ -> },
    onExpandImage: (Int) -> Unit,
    interactionsEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val bitmaps by produceState(
        initialValue = referenceImageUris.map { it to ImageLoadState(null, null) },
        referenceImageUris,
    ) {
        val current = referenceImageUris.map { it to ImageLoadState(null, null) }.toMutableList()
        value = current.toList() // reset immediately so stale images from previous recipe don't persist
        if (referenceImageUris.isEmpty()) return@produceState
        // previews in batches of 3 — first batch visible immediately
        referenceImageUris.chunked(3).forEachIndexed { batchIdx, batch ->
            val batchPreviews = withContext(Dispatchers.IO) {
                batch.map { uri -> uri to decodeSampledBitmap(context, Uri.parse(uri), maxPx = 16) }
            }
            batchPreviews.forEachIndexed { i, (uri, preview) ->
                current[batchIdx * 3 + i] = uri to ImageLoadState(preview = preview, full = null)
            }
            value = current.toList()
        }
        // full-res in batches of 3
        referenceImageUris.chunked(3).forEachIndexed { batchIdx, batch ->
            val batchFulls = withContext(Dispatchers.IO) {
                batch.map { uri -> uri to decodeSampledBitmap(context, Uri.parse(uri)) }
            }
            batchFulls.forEachIndexed { i, (uri, full) ->
                val idx = batchIdx * 3 + i
                current[idx] = uri to ImageLoadState(preview = current[idx].second.preview, full = full)
            }
            value = current.toList()
        }
    }
    val hasImages = bitmaps.isNotEmpty()
    var isEditing by remember { mutableStateOf(false) }
    LaunchedEffect(interactionsEnabled) {
        if (!interactionsEnabled) isEditing = false
    }

    // Local reorder state — updated during drag, committed to ViewModel on drag end
    var localOrder by remember(referenceImageUris) { mutableStateOf(referenceImageUris) }
    var draggingUri by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var currentDragIdx by remember { mutableStateOf<Int?>(null) }
    var dragOriginalOrder by remember { mutableStateOf(referenceImageUris) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val bitmapMap = remember(bitmaps) { bitmaps.toMap() }
    val imageScrollState = rememberScrollState()
    var imageRowWidthPx by remember { mutableIntStateOf(0) }
    // -1 = scroll left, 0 = idle, +1 = scroll right
    var edgeScrollDir by remember { mutableIntStateOf(0) }
    val edgeZonePx = with(LocalDensity.current) { 64.dp.toPx() }
    LaunchedEffect(edgeScrollDir, draggingUri) {
        while (draggingUri != null && edgeScrollDir != 0) {
            val delta = edgeScrollDir * 10f
            val before = imageScrollState.value
            imageScrollState.scrollBy(delta)
            val actual = (imageScrollState.value - before).toFloat()
            if (actual != 0f) dragOffsetX += actual
            delay(16)
        }
    }

    Spacer(Modifier.height(14.dp))
    if (hasImages) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelHigh)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "REFERENCE IMAGES",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.6.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${referenceImageUris.size}/$maxReferenceImages added",
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        color = TextMuted,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isEditing && referenceImageUris.size < maxReferenceImages) {
                        Text(
                            text = "ADD",
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.3.sp,
                            color = Gold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = interactionsEnabled, onClick = onAddReferenceImage)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                    if (isEditing) {
                        Text(
                            text = "DONE",
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.3.sp,
                            color = Gold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = interactionsEnabled) { isEditing = false }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = interactionsEnabled) { isEditing = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                IconEdit,
                                contentDescription = "Edit images",
                                tint = TextDim,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(imageScrollState)
                    .onSizeChanged { imageRowWidthPx = it.width },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val itemWidthPx = with(LocalDensity.current) { 118.dp.toPx() }
                val itemGapPx = with(LocalDensity.current) { 8.dp.toPx() }
                val itemStridePx = itemWidthPx + itemGapPx

                localOrder.forEachIndexed { index, uriString ->
                    val loadState = bitmapMap[uriString] ?: ImageLoadState(null, null)
                    val isDragging = draggingUri == uriString
                    // Compute where the dragged item would land based on current offset
                    val dragTargetIdx = if (draggingUri != null && dragStartIndex != null) {
                        (dragStartIndex!! + (dragOffsetX / itemStridePx).roundToInt())
                            .coerceIn(0, localOrder.lastIndex)
                    } else -1
                    // Visual shift for non-dragged items to make space
                    val visualOffsetX = when {
                        isDragging -> dragOffsetX
                        draggingUri == null || dragStartIndex == null -> 0f
                        dragStartIndex!! < dragTargetIdx && index in (dragStartIndex!! + 1)..dragTargetIdx -> -itemStridePx
                        dragStartIndex!! > dragTargetIdx && index in dragTargetIdx until dragStartIndex!! -> itemStridePx
                        else -> 0f
                    }
                    key(uriString) {
                        Box(
                            modifier = Modifier
                                .size(width = 118.dp, height = 86.dp)
                                .graphicsLayer {
                                    translationX = visualOffsetX
                                    if (isDragging) {
                                        scaleX = 1.07f
                                        scaleY = 1.07f
                                        shadowElevation = 16f
                                    }
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isDragging) 1.5.dp else 0.dp,
                                    color = if (isDragging) Gold.copy(alpha = 0.7f) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable(enabled = interactionsEnabled && !isDragging) { onExpandImage(index) }
                                .then(
                                    if (isEditing && referenceImageUris.size > 1) {
                                        Modifier.pointerInput(uriString) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { _ ->
                                                    FujiHaptics.perform(context, view, FujiHapticEffect.DragStart)
                                                    draggingUri = uriString
                                                    dragStartIndex = index
                                                    currentDragIdx = index
                                                    dragOriginalOrder = localOrder
                                                    dragOffsetX = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetX += dragAmount.x
                                                    val newTarget = (index + (dragOffsetX / itemStridePx).roundToInt())
                                                        .coerceIn(0, localOrder.lastIndex)
                                                    if (newTarget != currentDragIdx) {
                                                        FujiHaptics.perform(context, view, FujiHapticEffect.SoftConfirm)
                                                        currentDragIdx = newTarget
                                                    }
                                                    // Edge auto-scroll
                                                    val itemScreenX = (dragStartIndex ?: index) * itemStridePx + dragOffsetX - imageScrollState.value
                                                    edgeScrollDir = when {
                                                        itemScreenX < edgeZonePx -> -1
                                                        itemScreenX > imageRowWidthPx - edgeZonePx -> 1
                                                        else -> 0
                                                    }
                                                },
                                                onDragEnd = {
                                                    edgeScrollDir = 0
                                                    val start = dragStartIndex
                                                    val end = currentDragIdx
                                                    if (start != null && end != null && start != end) {
                                                        FujiHaptics.perform(context, view, FujiHapticEffect.DragEnd)
                                                        val m = localOrder.toMutableList()
                                                        m.add(end, m.removeAt(start))
                                                        localOrder = m
                                                        onReorderReferenceImages(start, end)
                                                    }
                                                    draggingUri = null; dragStartIndex = null; currentDragIdx = null; dragOffsetX = 0f
                                                },
                                                onDragCancel = {
                                                    edgeScrollDir = 0
                                                    localOrder = dragOriginalOrder
                                                    draggingUri = null; dragStartIndex = null; currentDragIdx = null; dragOffsetX = 0f
                                                },
                                            )
                                        }
                                    } else Modifier
                                ),
                        ) {
                            BlurredImagePlaceholder(
                                state = loadState,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            if (isEditing) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(5.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .clickable(enabled = interactionsEnabled) { onRemoveReferenceImage(uriString) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        IconClose,
                                        contentDescription = "Remove image",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelHigh)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .clickable(enabled = interactionsEnabled, onClick = onAddReferenceImage)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "REFERENCE IMAGE",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    color = TextDim,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Add up to 20 visual samples for this recipe",
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                )
            }
            Text(
                text = "ADD",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                letterSpacing = 1.3.sp,
                color = Gold,
            )
        }
    }
}

@Composable
private fun RecipeQrSheet(
    recipe: RecipeUiModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = recipe) {
        value = withContext(Dispatchers.Default) {
            RecipeQr.createBitmap(RecipeQr.encode(recipe))
        }
    }
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var entered by remember { mutableStateOf(!motionEnabled) }
    var dismissing by remember { mutableStateOf(false) }

    fun requestDismiss() {
        if (dismissing) return
        if (!motionEnabled) {
            onDismiss()
        } else {
            dismissing = true
            entered = false
        }
    }

    LaunchedEffect(motionEnabled) {
        entered = true
    }

    LaunchedEffect(dismissing) {
        if (dismissing) {
            delay(if (motionEnabled) 220 else 0)
            onDismiss()
        }
    }

    BackHandler(onBack = ::requestDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(onClick = ::requestDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = entered,
            enter = slideInVertically(
                animationSpec = tween(if (motionEnabled) 300 else 0, easing = FastOutSlowInEasing),
                initialOffsetY = { it },
            ),
            exit = slideOutVertically(
                animationSpec = tween(if (motionEnabled) 210 else 0, easing = FastOutSlowInEasing),
                targetOffsetY = { it },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = {})
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(top = 14.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(TextDim.copy(alpha = 0.42f)),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RECIPE QR",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.8.sp,
                            color = TextDim,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = recipe.name,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            color = TextPrimary,
                        )
                    }
                    IconButton(
                        onClick = ::requestDismiss,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(IconClose, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val readyBitmap = bitmap
                    if (readyBitmap != null) {
                        Image(
                            bitmap = readyBitmap.asImageBitmap(),
                            contentDescription = "Recipe QR code",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Gold,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Building QR",
                                fontFamily = MonoFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                color = Bg.copy(alpha = 0.68f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Scan with FujiSync to import this recipe.",
                    fontFamily = SansFamily,
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryCTA(
                    label = "Share Recipe Card",
                    busy = false,
                    enabled = bitmap != null,
                    onClick = {
                        bitmap?.let { qr ->
                            scope.launch(Dispatchers.IO) {
                                shareRecipeQr(context, recipe, qr)
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun shareRecipeQr(context: Context, recipe: RecipeUiModel, qrBitmap: Bitmap) {
    val referenceBitmap = decodeShareReferenceBitmap(context, recipe.referenceImageUris.firstOrNull())
    val card = createRecipeShareCard(recipe, qrBitmap, referenceBitmap)
    referenceBitmap?.recycle()
    val dir = File(context.cacheDir, "qr_codes").also { it.mkdirs() }
    val safeName = recipe.name
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "recipe" }
    val file = File(dir, "$safeName.png")
    file.outputStream().use { output ->
        card.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, recipe.name)
        putExtra(Intent.EXTRA_TEXT, "FujiSync recipe: ${recipe.name}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun decodeShareReferenceBitmap(context: Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        val uri = Uri.parse(uriString)
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val targetPx = 1080
        opts.inSampleSize = generateSequence(1) { it * 2 }
            .first { sample ->
                val sampledW = opts.outWidth.coerceAtLeast(1) / sample
                val sampledH = opts.outHeight.coerceAtLeast(1) / sample
                sampledW <= targetPx * 2 && sampledH <= targetPx * 2
            }
        opts.inJustDecodeBounds = false
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()
}

@Composable
private fun ReferenceImageLightbox(
    imageUris: List<String>,
    startIndex: Int,
    blurEnabled: Boolean = true,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val displayUris = remember(imageUris) { imageUris.filter { it.isNotBlank() } }
    if (displayUris.isEmpty()) return

    BackHandler(onBack = onDismiss)
    val initialPage = startIndex.coerceIn(displayUris.indices)
    val pagerState = rememberPagerState(initialPage = initialPage) { displayUris.size }
    LaunchedEffect(initialPage, displayUris.size) {
        if (pagerState.currentPage != initialPage) {
            pagerState.scrollToPage(initialPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = {}),
        ) { page ->
            val uriString = displayUris[page]
            val loadState by produceState(ImageLoadState(null, null), uriString) {
                val uri = Uri.parse(uriString)
                val preview = withContext(Dispatchers.IO) {
                    decodeSampledBitmap(context, uri, maxPx = 20)
                }
                value = ImageLoadState(preview = preview, full = null)
                val full = withContext(Dispatchers.IO) {
                    decodeSampledBitmap(context, uri, maxPx = 1920)
                }
                value = ImageLoadState(preview = preview, full = full)
            }
            BlurredImagePlaceholder(
                state = loadState,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                blurEnabled = blurEnabled,
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 14.dp, end = 12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            Icon(IconClose, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShootingSettingsSection(recipe: RecipeUiModel) {
    val hasIso = recipe.isoMin != null || recipe.isoMax != null
    val hasExposure = recipe.exposureCompMin != null || recipe.exposureCompMax != null
    val hasSensor = recipe.sensorGens.isNotEmpty()
    if (!hasIso && !hasExposure && !hasSensor) return

    fun fmtEv(v: Float): String {
        val n = if (v % 1f == 0f) v.toInt().toString() else v.toString()
        return if (v > 0) "+$n" else n
    }

    val rows = buildList {
        if (hasIso) {
            val min = recipe.isoMin
            val max = recipe.isoMax
            val value = when {
                min != null && max != null && min == max -> "$min"
                min != null && max != null -> "$min – $max"
                min != null -> "≥ $min"
                max != null -> "≤ $max"
                else -> "—"
            }
            add("Recommended ISO" to value)
        }
        if (hasExposure) {
            val min = recipe.exposureCompMin
            val max = recipe.exposureCompMax
            val value = when {
                min != null && max != null && min == max -> fmtEv(min)
                min != null && max != null -> "${fmtEv(min)} – ${fmtEv(max)}"
                min != null -> "≥ ${fmtEv(min)}"
                max != null -> "≤ ${fmtEv(max)}"
                else -> "—"
            }
            add("Exposure Comp" to value)
        }
        if (hasSensor) {
            add("Sensors" to recipe.sensorGens.joinToString(", ") { "X-Trans $it" })
        }
    }

    Spacer(Modifier.height(8.dp))
    SectionLabel(text = "Shooting Settings", modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        rows.forEachIndexed { i, (label, value) ->
            PropRow(label = label, value = value, isLast = i == rows.lastIndex)
            if (i < rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border),
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SavedFromSection(recipe: RecipeUiModel) {
    val cameraSource = recipe.sourceCameraDisplayName() ?: return

    val rows = buildList {
        add("Camera" to cameraSource)
        recipe.saved?.let { add("Added" to it) }
        recipe.sourceUsbId?.let { add("USB ID" to it) }
    }

    Spacer(Modifier.height(8.dp))
    SectionLabel(text = "Saved From", modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        rows.forEachIndexed { i, (label, value) ->
            SavedFromRow(label = label, value = value)
            if (i < rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border),
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SavedFromRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontFamily = SansFamily,
            fontSize = 14.5.sp,
            color = TextPrimary,
        )
        Text(
            text = value,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            letterSpacing = 0.2.sp,
            color = TextMuted,
        )
    }
}

@Composable
private fun SyncToCameraSheet(
    connected: Boolean,
    cameraModel: String,
    cameraSlots: List<RecipeUiModel>,
    recipeName: String,
    writeBusy: Boolean,
    onDismiss: () -> Unit,
    onWriteToSlot: (String) -> Unit,
) {
    val slotNames = listOf("C1", "C2", "C3", "C4", "C5", "C6", "C7")
    val slotMap = cameraSlots.associateBy { it.slot }
    var selectedSlot by remember { mutableStateOf("C1") }
    var sheetVisible by remember { mutableStateOf(false) }
    var confirmingWrite by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { sheetVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.76f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = sheetVisible,
            enter = slideInVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 3 },
            ) + fadeIn(tween(200)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF111009))
                    .border(1.dp, Border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
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
                        .background(TextDim.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "SYNC TO CAMERA",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp,
                            color = TextMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (connected && cameraModel.isNotBlank()) cameraModel.uppercase() else "NO CAMERA",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            letterSpacing = 0.4.sp,
                            color = TextPrimary,
                        )
                    }
                    if (connected) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Gold.copy(alpha = 0.12f))
                                .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Gold),
                            )
                            Text(
                                text = "CONNECTED",
                                fontFamily = MonoFamily,
                                fontSize = 9.sp,
                                letterSpacing = 1.2.sp,
                                color = Gold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (!connected) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Connect your Fujifilm camera via USB-C to sync this recipe.",
                            fontFamily = SansFamily,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = TextDim,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "SET CAMERA TO USB RAW CONV. / BACKUP MODE",
                            fontFamily = MonoFamily,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.2.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Border),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        slotNames.forEach { slot ->
                            val currentRecipe = slotMap[slot]
                            val isSelected = slot == selectedSlot
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Gold.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { selectedSlot = slot }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Text(
                                        text = slot,
                                        fontFamily = MonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.2.sp,
                                        color = if (isSelected) Gold else TextMuted,
                                        modifier = Modifier.width(26.dp),
                                    )
                                    Column {
                                        Text(
                                            text = currentRecipe?.name?.ifBlank { "—" } ?: "—",
                                            fontFamily = SansFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.6f),
                                        )
                                        if (currentRecipe?.sim?.isNotBlank() == true) {
                                            Text(
                                                text = currentRecipe.sim.uppercase(),
                                                fontFamily = MonoFamily,
                                                fontSize = 9.5.sp,
                                                letterSpacing = 1.2.sp,
                                                color = TextDim,
                                            )
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        IconCheck,
                                        contentDescription = null,
                                        tint = Gold,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Border.copy(alpha = 0.5f)),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        PrimaryCTA(
                            label = if (writeBusy) "Writing…" else "Write to $selectedSlot",
                            onClick = { confirmingWrite = true },
                            busy = writeBusy,
                            enabled = !writeBusy,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (confirmingWrite) {
        DeleteConfirmDialog(
            eyebrow = "WRITE TO CAMERA",
            title = selectedSlot,
            body = "\"$recipeName\" will overwrite the current recipe in $selectedSlot.",
            confirmLabel = "Write to $selectedSlot",
            confirmColor = Gold,
            onConfirm = { onWriteToSlot(selectedSlot) },
            onDismiss = { confirmingWrite = false },
        )
    }
}

@Composable
private fun PropSectionDetail(label: String, data: Map<String, String>) {
    if (data.isEmpty()) return
    val entries = recipePropertyRows(data)
    if (entries.isEmpty()) return
    Spacer(Modifier.height(8.dp))
    SectionLabel(text = label, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        entries.forEachIndexed { i, (k, v) ->
            PropRow(
                label = k,
                value = v,
                isLast = i == entries.lastIndex,
            )
            if (i < entries.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border),
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}


