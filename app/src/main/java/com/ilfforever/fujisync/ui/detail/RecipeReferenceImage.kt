package com.ilfforever.fujisync.ui.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.components.IconEdit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import com.ilfforever.fujisync.ui.components.decodeSampledBitmap
import com.ilfforever.fujisync.ui.components.BlurredImagePlaceholder
import com.ilfforever.fujisync.ui.components.ImageLoadState
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import android.content.Context
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
internal fun RecipeReferenceImage(
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
            if (isEditing && referenceImageUris.size > 1) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Hold and drag to rearrange",
                    fontFamily = SansFamily,
                    fontSize = 11.sp,
                    color = TextDim,
                )
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

internal fun decodeShareReferenceBitmap(context: Context, uriString: String?): Bitmap? {
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
internal fun ReferenceImageLightbox(
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
