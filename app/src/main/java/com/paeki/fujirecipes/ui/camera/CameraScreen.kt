package com.paeki.fujirecipes.ui.camera

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.paeki.fujirecipes.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import com.paeki.fujirecipes.ui.components.IconChevronRight
import com.paeki.fujirecipes.ui.components.IconRefresh
import com.paeki.fujirecipes.ui.components.IconSort
import com.paeki.fujirecipes.ui.components.IconUSB
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.components.DragHandle
import com.paeki.fujirecipes.ui.components.MetaRow
import com.paeki.fujirecipes.ui.components.Pill
import com.paeki.fujirecipes.ui.components.PrimaryCTA
import com.paeki.fujirecipes.ui.components.PropRow
import com.paeki.fujirecipes.ui.components.SectionLabel
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.GoldFaint
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelHigh
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

private val PEEK_HEIGHT = 170.dp
private val CAMERA_CARD_HEIGHT = 166.dp

private data class CameraImageTuning(
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

// ── Connect guide (disconnected state) ────────────────────────────
@Composable
fun ConnectGuide(onSimulateConnect: () -> Unit) {
    val steps = listOf(
        Triple(1, "On your camera", "Menu → Connection Setting → USB Mode"),
        Triple(2, "Plug in a USB-C data cable", "Phone to camera, directly — avoid USB hubs."),
        Triple(3, "Accept the USB prompt", "Tap OK on the permission dialog when it appears."),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Spacer(Modifier.height(38.dp))
        Box(
            modifier = Modifier
                .size(78.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(20.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IconUSB, contentDescription = null, tint = Gold, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "CONNECT YOUR CAMERA",
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 1.2.sp,
            color = TextPrimary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp),
        )
        steps.forEachIndexed { index, (n, heading, body) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Border,
                        shape = RoundedCornerShape(0.dp),
                    )
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GoldFaint)
                        .border(1.dp, Gold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = n.toString(),
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Gold,
                    )
                }
                Column {
                    Text(
                        text = heading,
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.5.sp,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = body,
                        fontFamily = SansFamily,
                        fontSize = 13.5.sp,
                        color = TextMuted,
                        lineHeight = 20.sp,
                    )
                    if (n == 1) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, Gold, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = "USB RAW CONV.",
                                fontFamily = MonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                color = Gold,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Trouble connecting?",
            fontFamily = SansFamily,
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .clickable(onClick = onSimulateConnect)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = "SIMULATE CONNECT →",
                fontFamily = MonoFamily,
                fontSize = 10.5.sp,
                letterSpacing = 1.4.sp,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Camera connected (slot board + bottom sheet) ──────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraConnected(
    cameraModel: String,
    firmware: String,
    battery: String,
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
    onOpenDetail: () -> Unit,
    onWrite: () -> Unit,
    writeBusy: Boolean,
    cameraNames: List<String> = listOf("My Camera", "My Camera", "My Camera"),
    onOpenCameraDetail: (Int, CameraCardUiModel) -> Unit = { _, _ -> },
    onSheetExpandedChange: (Boolean) -> Unit = {},
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
    val recipe = slots.getOrNull(selectedSlotIdx) ?: return
    val debugTunings = remember { mutableStateMapOf<String, CameraImageTuning>() }
    var showImageDebug by remember { mutableStateOf(false) }
    val cameras = remember(cameraModel, firmware, battery) {
        listOf(
            CameraCardUiModel(cameraModel, firmware, battery, connected = true, usbId = "04CB:0128", lastSync = "Today, 9:14 AM"),
            CameraCardUiModel("X-T5", "2.20", "—", connected = false, usbId = "04CB:0131", lastSync = "May 24, 2:33 PM"),
            CameraCardUiModel("X100VI", "2.01", "—", connected = false, usbId = "04CB:0145", lastSync = "May 21, 11:05 AM"),
        )
    }
    val pagerState = rememberPagerState(pageCount = { cameras.size })

    LaunchedEffect(isExpanded) {
        onSheetExpandedChange(isExpanded)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = PEEK_HEIGHT,
        sheetContainerColor = PanelLow,
        containerColor = Bg,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 0.dp,
        sheetDragHandle = {
            Box {
                DragHandle()
            }
        },
        sheetContent = {
            SheetContent(
                recipe = recipe,
                isExpanded = isExpanded,
                onOpenDetail = onOpenDetail,
                onWrite = onWrite,
                writeBusy = writeBusy,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp)),
            ) { page ->
                val cam = cameras[page]
                CameraCard(
                    camera = cam,
                    label = cameraNames.getOrNull(page) ?: "My Camera",
                    imageTuning = debugTunings[cam.model] ?: cameraImageTuning(cam.model),
                    onClick = { onOpenCameraDetail(page, cam) },
                )
            }

            // Action buttons
            ActionButtons()

            CustomSlotSection(
                slots = slots,
                selectedSlotIdx = selectedSlotIdx,
                onSelectSlot = onSelectSlot,
            )

            val visibleCamera = cameras[pagerState.currentPage]
            ImageDebugScaler(
                model = visibleCamera.model,
                tuning = debugTunings[visibleCamera.model] ?: cameraImageTuning(visibleCamera.model),
                expanded = showImageDebug,
                onToggle = { showImageDebug = !showImageDebug },
                onChange = { debugTunings[visibleCamera.model] = it },
            )
        }
    }
}

// Camera image resource for each known model
private fun cameraDrawable(model: String): Int? = when {
    model.contains("X-H2", ignoreCase = true) || model.contains("XH2", ignoreCase = true) -> R.drawable.camera_xh2
    model.contains("X-T5", ignoreCase = true) || model.contains("XT5", ignoreCase = true) -> R.drawable.camera_xt5
    model.contains("X100VI", ignoreCase = true) -> R.drawable.camera_x100vi
    else -> null
}

private fun cameraImageTuning(model: String) = when {
    model.contains("X-H2", ignoreCase = true) || model.contains("XH2", ignoreCase = true) ->
        CameraImageTuning(width = 317.dp, height = 229.dp, offsetX = 47.dp, offsetY = 3.dp)
    model.contains("X-T5", ignoreCase = true) || model.contains("XT5", ignoreCase = true) ->
        CameraImageTuning(width = 414.dp, height = 394.dp, offsetX = 91.dp, offsetY = (-77).dp)
    model.contains("X100VI", ignoreCase = true) ->
        CameraImageTuning(width = 420.dp, height = 325.dp, offsetX = 115.dp, offsetY = (-61).dp)
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
    // overflow: hidden is the clip on the Box — camera image bleeds off the right edge
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
            // Prototype: position absolute, top:0, right:-30, width:260px, height:auto
            // Mask: transparent at left → fully opaque at 35% → opaque to right
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
                    .graphicsLayer {
                        // Offscreen compositing so DstIn mask only affects this image
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        // DstIn: gradient alpha masks the image
                        // transparent @ 0% → opaque @ 35% (image fades in from the right)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.35f to Color.Black,
                                1f to Color.Black,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }

        // Text on top (drawn after image = higher z-order in Box)
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 15.dp),
        ) {
            SectionLabel(text = label)
            Spacer(Modifier.height(8.dp))
            Text(
                text = camera.model,
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                letterSpacing = (-0.5).sp,
                color = TextPrimary,
                lineHeight = 44.sp,
            )
            Spacer(Modifier.height(15.dp))
            MetaRow("Firmware", camera.firmware)
            Spacer(Modifier.height(8.dp))
            MetaRow("Battery", camera.battery)
        }
    }
}

// ── Action buttons (Backup / Restore) ────────────────────────────
@Composable
private fun ActionButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .background(PanelLow),
    ) {
        ActionButton(
            icon = IconSort,
            label = "Backup Settings",
            modifier = Modifier.weight(1f),
        )
        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(44.dp)
                .background(Border),
        )
        ActionButton(
            icon = IconRefresh,
            label = "Restore Previous",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            letterSpacing = 1.2.sp,
            color = TextMuted,
        )
    }
}

@Composable
private fun CustomSlotSection(
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(top = 12.dp, bottom = 12.dp),
    ) {
        SectionLabel(
            text = "Custom Slots",
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Spacer(Modifier.height(10.dp))
        CompactSlotGrid(
            slots = slots,
            selectedSlotIdx = selectedSlotIdx,
            onSelectSlot = onSelectSlot,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun CompactSlotGrid(
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstRow = slots.take(4)
    val secondRow = slots.drop(4).take(3)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            firstRow.forEachIndexed { idx, slot ->
                CompactSlotChip(
                    recipe = slot,
                    active = idx == selectedSlotIdx,
                    onClick = { onSelectSlot(idx) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            secondRow.forEachIndexed { localIdx, slot ->
                val idx = localIdx + 4
                CompactSlotChip(
                    recipe = slot,
                    active = idx == selectedSlotIdx,
                    onClick = { onSelectSlot(idx) },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(4 - secondRow.size) {
                Spacer(Modifier.weight(1f))
            }
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
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) PanelHigh else PanelLow)
            .border(1.dp, if (active) Gold else Border, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Text(
            text = recipe.slot,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 0.4.sp,
            color = Gold,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = recipe.name,
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.3.sp,
            color = if (active) TextPrimary else TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ImageDebugScaler(
    model: String,
    tuning: CameraImageTuning,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CameraImageTuning) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "IMAGE DEBUG",
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                letterSpacing = 1.8.sp,
                color = Gold,
            )
            Text(
                text = "${model.uppercase()}  ${tuning.width.value.toInt()}x${tuning.height.value.toInt()}  ${tuning.offsetX.value.toInt()},${tuning.offsetY.value.toInt()}",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                color = TextMuted,
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                DebugSlider(
                    label = "Width",
                    value = tuning.width.value,
                    range = 180f..460f,
                    onValueChange = { onChange(tuning.copy(width = it.dp)) },
                )
                DebugSlider(
                    label = "Height",
                    value = tuning.height.value,
                    range = 180f..430f,
                    onValueChange = { onChange(tuning.copy(height = it.dp)) },
                )
                DebugSlider(
                    label = "Offset X",
                    value = tuning.offsetX.value,
                    range = -80f..180f,
                    onValueChange = { onChange(tuning.copy(offsetX = it.dp)) },
                )
                DebugSlider(
                    label = "Offset Y",
                    value = tuning.offsetY.value,
                    range = -100f..100f,
                    onValueChange = { onChange(tuning.copy(offsetY = it.dp)) },
                )
            }
        }
    }
}

@Composable
private fun DebugSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                fontFamily = SansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = TextDim,
            )
            Text(
                text = value.toInt().toString(),
                fontFamily = MonoFamily,
                fontSize = 10.5.sp,
                color = TextMuted,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(32.dp),
        )
    }
}

// ── Bottom sheet content ──────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SheetContent(
    recipe: RecipeUiModel,
    isExpanded: Boolean,
    onOpenDetail: () -> Unit,
    onWrite: () -> Unit,
    writeBusy: Boolean,
) {
    val recipeScrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(top = 6.dp, start = 20.dp, end = 20.dp, bottom = 0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = recipe.sim.uppercase(),
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.8.sp,
                        color = TextMuted,
                    )
                    Text(
                        text = recipe.name,
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 0.1.sp,
                        color = TextPrimary,
                    )
                }
                Row(
                    modifier = Modifier
                        .clickable(onClick = onOpenDetail)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "EDIT",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.8.sp,
                        color = Gold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        IconChevronRight,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                recipe.pills.forEach { Pill(text = it, large = true) }
            }
        }

        if (isExpanded) {
            // Full recipe when expanded
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(recipeScrollState)
                    .padding(bottom = 16.dp),
            ) {
                PropSectionBlock(label = "Effects", data = recipe.effects)
                PropSectionBlock(label = "Tone", data = recipe.tone)
                PropSectionBlock(label = "White Balance", data = recipe.wb)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                PrimaryCTA(
                    label = if (writeBusy) "Saving…" else "Save to Library",
                    onClick = onWrite,
                    busy = writeBusy,
                )
            }
        } else {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "↑  PULL UP FOR FULL RECIPE",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.8.sp,
                color = TextMuted,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SlotRow(
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            slots.forEachIndexed { idx, slot ->
                SheetSlotChip(
                    recipe = slot,
                    active = idx == selectedSlotIdx,
                    onClick = { onSelectSlot(idx) },
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(48.dp)
                .height(44.dp)
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        1f to PanelLow,
                    ),
                ),
        )
        }
}

@Composable
private fun SlotGrid(
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.chunked(2).forEachIndexed { rowIdx, rowSlots ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSlots.forEachIndexed { colIdx, slot ->
                    val idx = rowIdx * 2 + colIdx
                    SheetSlotChip(
                        recipe = slot,
                        active = idx == selectedSlotIdx,
                        onClick = { onSelectSlot(idx) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowSlots.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SheetSlotChip(
    recipe: RecipeUiModel,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) PanelHigh else PanelLow)
            .border(1.dp, if (active) Gold else Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 11.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = recipe.slot,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.4.sp,
            color = Gold,
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = recipe.name,
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            letterSpacing = 0.5.sp,
            color = if (active) TextPrimary else TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Prop section block ────────────────────────────────────────────
@Composable
private fun PropSectionBlock(label: String, data: Map<String, String>) {
    if (data.isEmpty()) return
    val entries = data.entries.toList()
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 0.dp)) {
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
                PropRow(label = k, value = v, isLast = i == entries.lastIndex)
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
        Spacer(Modifier.height(14.dp))
    }
}
