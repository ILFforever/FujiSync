package com.ilfforever.fujisync.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.DeleteConfirmDialog
import com.ilfforever.fujisync.ui.components.FilmSimLabel
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.components.IconCopy
import com.ilfforever.fujisync.ui.components.IconGlobe
import com.ilfforever.fujisync.ui.components.IconEdit
import com.ilfforever.fujisync.ui.components.IconMore
import com.ilfforever.fujisync.ui.components.IconQrCode
import com.ilfforever.fujisync.ui.components.IconStar
import com.ilfforever.fujisync.ui.components.IconStarFilled
import com.ilfforever.fujisync.ui.components.IconTrash
import com.ilfforever.fujisync.ui.components.Pill
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.components.PropRow
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.components.recipePropertyRows
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.sourceCameraDisplayName
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

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
                                    icon: ImageVector,
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
                        SourceCreditSection(visibleRecipe)
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
private fun SourceCreditSection(recipe: RecipeUiModel) {
    val url = recipe.sourceUrl ?: return
    val label = recipe.sourceLabel ?: "Source"
    val creator = if (recipe.sourceLabel == "Fuji X Weekly") "Ritchie Roesch" else null
    val uriHandler = LocalUriHandler.current
    Spacer(Modifier.height(8.dp))
    SectionLabel(text = "Credit", modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri(url) }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = TextDim,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(IconGlobe, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                Text(
                    text = "OPEN ORIGINAL",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.7.sp,
                    color = Gold,
                )
            }
        }
    }
    if (creator != null) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(14.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Created and maintained by $creator",
                fontFamily = SansFamily,
                fontSize = 12.sp,
                color = TextMuted,
            )
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
