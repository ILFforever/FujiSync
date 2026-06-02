package com.paeki.fujirecipes.ui.camera

import com.paeki.fujirecipes.ui.overlay.OverlayLayer
import com.paeki.fujirecipes.ui.overlay.BackHandler
import com.paeki.fujirecipes.ui.overlay.overlayStackOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.R
import com.paeki.fujirecipes.ui.components.DragHandle
import com.paeki.fujirecipes.ui.components.FilmSimBadgeImage
import com.paeki.fujirecipes.ui.components.IconArrowDown
import com.paeki.fujirecipes.ui.components.IconRefresh
import com.paeki.fujirecipes.ui.components.IconSort
import com.paeki.fujirecipes.ui.components.MetaRow
import com.paeki.fujirecipes.ui.components.SectionLabel
import com.paeki.fujirecipes.ui.model.LibraryRecipeSource
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.model.SaveAllReport
import com.paeki.fujirecipes.ui.model.SlotBackupMeta
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelHigh
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary
import kotlinx.coroutines.launch

internal val PEEK_HEIGHT = 165.dp
internal val DRAG_HANDLE_HEIGHT = 24.dp
private val CAMERA_CARD_HEIGHT = 166.dp

internal data class CameraImageTuning(
    val width: Dp,
    val height: Dp,
    val offsetX: Dp,
    val offsetY: Dp,
)

data class CameraCardUiModel(
    val model: String,
    val firmware: String,
    val battery: String,
    val connected: Boolean,
    val usbId: String,
    val lastSync: String,
)

// ── Camera connected (slot board + bottom sheet) ──────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraConnected(
    cameraModel: String,
    firmware: String,
    battery: String,
    readingSlots: Boolean = false,
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
    onOpenDetail: () -> Unit,
    onSaveToLibrary: (LibraryRecipeSource) -> Unit,
    writeBusy: Boolean,
    librarySaveConfirmed: Boolean,
    hasSlotBackup: Boolean = false,
    slotBackupMeta: SlotBackupMeta? = null,
    slotBackupSlots: List<RecipeUiModel>? = null,
    onBackupSlots: (String) -> Unit = {},
    onRestoreSlots: () -> Unit = {},
    onDeleteSlotBackup: () -> Unit = {},
    onRenameSlotBackup: (String) -> Unit = {},
    cameraSerial: String = "",
    cameraNames: List<String> = listOf("My Camera"),
    onOpenCameraDetail: (Int, CameraCardUiModel) -> Unit = { _, _ -> },
    onSheetRevealProgressChange: (Float) -> Unit = {},
    onSaveAllToLibrary: (LibraryRecipeSource) -> Unit = {},
    saveAllSlotsConfirmed: Boolean = false,
    saveAllReport: SaveAllReport? = null,
    onSaveAllReportDismiss: () -> Unit = {},
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
    if (readingSlots && slots.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "READING C1–C7…", fontFamily = MonoFamily, fontSize = 11.sp, letterSpacing = 2.sp, color = TextDim)
        }
        return
    }
    val recipe = slots.getOrNull(selectedSlotIdx) ?: return
    var showBackupSheet by remember { mutableStateOf(false) }
    var showRestoreSheet by remember { mutableStateOf(false) }
    var showSaveAllSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cameras = remember(cameraModel, firmware, battery, cameraSerial) {
        listOf(CameraCardUiModel(cameraModel, firmware, battery, connected = true, usbId = cameraSerial, lastSync = ""))
    }
    val pagerState = rememberPagerState(pageCount = { cameras.size })

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val sheetRevealProgress = runCatching {
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val peekHeightPx = with(density) { PEEK_HEIGHT.toPx() }
            val partialOffsetPx = (screenHeightPx - peekHeightPx).coerceAtLeast(1f)
            ((partialOffsetPx - scaffoldState.bottomSheetState.requireOffset()) / partialOffsetPx).coerceIn(0f, 1f)
        }.getOrElse { if (isExpanded) 1f else 0f }

        LaunchedEffect(sheetRevealProgress) { onSheetRevealProgressChange(sheetRevealProgress) }

        overlayStackOf(
            OverlayLayer(isExpanded) { scope.launch { runCatching { scaffoldState.bottomSheetState.partialExpand() } } },
            OverlayLayer(showBackupSheet) { showBackupSheet = false },
            OverlayLayer(showRestoreSheet) { showRestoreSheet = false },
            OverlayLayer(showSaveAllSheet) { showSaveAllSheet = false },
            OverlayLayer(saveAllReport != null) { onSaveAllReportDismiss() },
        ).BackHandler()

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = PEEK_HEIGHT,
            sheetContainerColor = PanelLow,
            containerColor = Bg,
            sheetTonalElevation = 0.dp,
            sheetShadowElevation = 0.dp,
            sheetDragHandle = { Box { DragHandle() } },
            sheetContent = {
                val selectedCamera = cameras[pagerState.currentPage]
                SheetContent(
                    recipe = recipe,
                    sheetRevealProgress = sheetRevealProgress,
                    onOpenDetail = onOpenDetail,
                    onSaveToLibrary = {
                        onSaveToLibrary(
                            LibraryRecipeSource(
                                cameraName = cameraNames.getOrNull(pagerState.currentPage) ?: "My Camera",
                                cameraModel = selectedCamera.model,
                                usbId = selectedCamera.usbId,
                            )
                        )
                    },
                    writeBusy = writeBusy,
                    librarySaveConfirmed = librarySaveConfirmed,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
                    ) { page ->
                        val cam = cameras[page]
                        CameraCard(
                            camera = cam,
                            label = cameraNames.getOrNull(page) ?: "My Camera",
                            imageTuning = cameraImageTuning(cam.model),
                            onClick = { onOpenCameraDetail(page, cam) },
                        )
                    }

                    ActionButtons(
                        hasSlotBackup = hasSlotBackup,
                        onBackupSlots = { showBackupSheet = true },
                        onRestoreSlots = { showRestoreSheet = true },
                    )

                    CustomSlotSection(
                        slots = slots,
                        selectedSlotIdx = selectedSlotIdx,
                        onSelectSlot = onSelectSlot,
                        onSaveAllToLibrary = { showSaveAllSheet = true },
                        saveAllSlotsConfirmed = saveAllSlotsConfirmed,
                    )
                }
            }
        }

        if (showBackupSheet) {
            BackupSheet(onDismiss = { showBackupSheet = false }, onConfirm = { label -> onBackupSlots(label) })
        }

        if (showRestoreSheet) {
            RestoreSheet(
                meta = slotBackupMeta,
                backupSlots = slotBackupSlots,
                onDismiss = { showRestoreSheet = false },
                onConfirm = { onRestoreSlots() },
                onDelete = onDeleteSlotBackup,
                onRename = onRenameSlotBackup,
            )
        }

        if (saveAllReport != null) {
            SaveAllReportSheet(report = saveAllReport, onDismiss = onSaveAllReportDismiss)
        }

        if (showSaveAllSheet) {
            val selectedCamera = cameras[pagerState.currentPage]
            SaveAllSheet(
                slots = slots,
                onDismiss = { showSaveAllSheet = false },
                onConfirm = {
                    onSaveAllToLibrary(
                        LibraryRecipeSource(
                            cameraName = cameraNames.getOrNull(pagerState.currentPage) ?: "My Camera",
                            cameraModel = selectedCamera.model,
                            usbId = selectedCamera.usbId,
                        )
                    )
                },
            )
        }
    }
}

// ── Camera image resource per model ──────────────────────────────
private fun cameraDrawable(model: String): Int? = when {
    model.contains("X-T30 III", ignoreCase = true) || model.contains("XT30III", ignoreCase = true) -> R.drawable.camera_xt30iii
    model.contains("X-T50", ignoreCase = true) || model.contains("XT50", ignoreCase = true) -> R.drawable.camera_xt50
    model.contains("X-H2", ignoreCase = true) || model.contains("XH2", ignoreCase = true) -> R.drawable.camera_xh2
    model.contains("X-T5", ignoreCase = true) || model.contains("XT5", ignoreCase = true) -> R.drawable.camera_xt5
    model.contains("X-S20", ignoreCase = true) || model.contains("XS20", ignoreCase = true) -> R.drawable.camera_xs20
    model.contains("X-M5", ignoreCase = true) || model.contains("XM5", ignoreCase = true) -> R.drawable.camera_xm5
    model.contains("X-E5", ignoreCase = true) || model.contains("XE5", ignoreCase = true) -> R.drawable.camera_xe5
    model.contains("X100VI", ignoreCase = true) -> R.drawable.camera_x100vi
    model.contains("X-Pro3", ignoreCase = true) || model.contains("XPro3", ignoreCase = true) -> R.drawable.camera_xpro3
    else -> null
}

internal fun cameraImageTuning(model: String) = when {
    model.contains("X-T30 III", ignoreCase = true) || model.contains("XT30III", ignoreCase = true) ->
        CameraImageTuning(width = 460.dp, height = 430.dp, offsetX = 160.dp, offsetY = (-150).dp)
    model.contains("X-T50", ignoreCase = true) || model.contains("XT50", ignoreCase = true) ->
        CameraImageTuning(width = 460.dp, height = 387.dp, offsetX = 145.dp, offsetY = (-120).dp)
    model.contains("X-H2", ignoreCase = true) || model.contains("XH2", ignoreCase = true) ->
        CameraImageTuning(width = 317.dp, height = 229.dp, offsetX = 47.dp, offsetY = 3.dp)
    model.contains("X-T5", ignoreCase = true) || model.contains("XT5", ignoreCase = true) ->
        CameraImageTuning(width = 414.dp, height = 394.dp, offsetX = 91.dp, offsetY = (-77).dp)
    model.contains("X-S20", ignoreCase = true) || model.contains("XS20", ignoreCase = true) ->
        CameraImageTuning(width = 423.dp, height = 410.dp, offsetX = 159.dp, offsetY = (-87).dp)
    model.contains("X-M5", ignoreCase = true) || model.contains("XM5", ignoreCase = true) ->
        CameraImageTuning(width = 360.dp, height = 360.dp, offsetX = 111.dp, offsetY = (-56).dp)
    model.contains("X-E5", ignoreCase = true) || model.contains("XE5", ignoreCase = true) ->
        CameraImageTuning(width = 430.dp, height = 273.dp, offsetX = 157.dp, offsetY = (-1).dp)
    model.contains("X100VI", ignoreCase = true) ->
        CameraImageTuning(width = 420.dp, height = 325.dp, offsetX = 115.dp, offsetY = (-61).dp)
    model.contains("X-Pro3", ignoreCase = true) || model.contains("XPro3", ignoreCase = true) ->
        CameraImageTuning(width = 430.dp, height = 229.dp, offsetX = 140.dp, offsetY = 14.dp)
    else ->
        CameraImageTuning(width = 317.dp, height = 229.dp, offsetX = 47.dp, offsetY = 3.dp)
}

// ── Camera card ───────────────────────────────────────────────────
@Composable
private fun CameraCard(
    camera: CameraCardUiModel,
    label: String,
    imageTuning: CameraImageTuning,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CAMERA_CARD_HEIGHT)
            .clip(RoundedCornerShape(18.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        val drawableRes = cameraDrawable(camera.model)
        if (drawableRes != null) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .wrapContentSize(unbounded = true, align = Alignment.TopEnd)
                    .requiredWidth(imageTuning.width)
                    .requiredHeight(imageTuning.height)
                    .offset(x = imageTuning.offsetX, y = imageTuning.offsetY)
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
            SectionLabel(text = label)
            Spacer(Modifier.height(8.dp))
            Text(text = camera.model, fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = (-0.5).sp, color = TextPrimary, lineHeight = 44.sp)
            Spacer(Modifier.height(15.dp))
            MetaRow("Firmware", camera.firmware)
            Spacer(Modifier.height(8.dp))
            MetaRow("Battery", camera.battery)
        }
    }
}

// ── Action buttons (Backup / Restore) ─────────────────────────────
@Composable
private fun ActionButtons(
    hasSlotBackup: Boolean = false,
    onBackupSlots: () -> Unit,
    onRestoreSlots: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .background(PanelLow),
    ) {
        ActionButton(icon = IconSort, label = "Backup Sets", onClick = onBackupSlots, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.width(1.dp).height(44.dp).background(Border))
        ActionButton(icon = IconRefresh, label = "Restore Sets", enabled = hasSlotBackup, onClick = onRestoreSlots, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = if (enabled) Gold else TextMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label.uppercase(), fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, letterSpacing = 1.2.sp, color = if (enabled) TextMuted else TextMuted.copy(alpha = 0.4f))
    }
}

// ── Slot board ────────────────────────────────────────────────────
@Composable
private fun CustomSlotSection(
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
    onSaveAllToLibrary: () -> Unit = {},
    saveAllSlotsConfirmed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(top = 13.dp, bottom = 13.dp),
    ) {
        SectionLabel(text = "Custom Slots", modifier = Modifier.padding(horizontal = 14.dp))
        Spacer(Modifier.height(10.dp))
        CompactSlotGrid(
            slots = slots,
            selectedSlotIdx = selectedSlotIdx,
            onSelectSlot = onSelectSlot,
            onSaveAllToLibrary = onSaveAllToLibrary,
            saveAllSlotsConfirmed = saveAllSlotsConfirmed,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun CompactSlotGrid(
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
    onSaveAllToLibrary: () -> Unit = {},
    saveAllSlotsConfirmed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val firstRow = slots.take(4)
    val secondRow = slots.drop(4).take(3)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            firstRow.forEachIndexed { idx, slot ->
                CompactSlotChip(recipe = slot, active = idx == selectedSlotIdx, onClick = { onSelectSlot(idx) }, modifier = Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            secondRow.forEachIndexed { localIdx, slot ->
                val idx = localIdx + 4
                CompactSlotChip(recipe = slot, active = idx == selectedSlotIdx, onClick = { onSelectSlot(idx) }, modifier = Modifier.weight(1f))
            }
            repeat(3 - secondRow.size) { Spacer(Modifier.weight(1f)) }
            SaveAllSlotChip(confirmed = saveAllSlotsConfirmed, onClick = onSaveAllToLibrary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SaveAllSlotChip(
    confirmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = Gold.copy(alpha = 0.36f)
    val labelColor = Gold
    Box(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Gold.copy(alpha = 0.08f))
            .border(1.dp, borderColor, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(6.dp))
                .background(Gold.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = IconArrowDown,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(11.dp),
            )
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = "SAVE",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                letterSpacing = 0.7.sp,
                color = labelColor,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "ALL",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                letterSpacing = 0.7.sp,
                color = labelColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompactSlotChip(
    recipe: RecipeUiModel,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) PanelHigh else PanelLow)
            .border(1.dp, if (active) Gold else Border, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        FilmSimBadgeImage(sim = recipe.sim, size = 24.dp, modifier = Modifier.align(Alignment.TopEnd))
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(text = recipe.slot, fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, letterSpacing = 0.4.sp, color = Gold)
            Spacer(Modifier.height(3.dp))
            Text(text = recipe.name, fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.3.sp, color = if (active) TextPrimary else TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
