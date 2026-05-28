package com.paeki.fujirecipes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import com.paeki.fujirecipes.ui.components.IconCamera
import com.paeki.fujirecipes.ui.components.IconClose
import com.paeki.fujirecipes.ui.components.IconFolder
import com.paeki.fujirecipes.ui.components.IconProfile
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.camera.CameraCardUiModel
import com.paeki.fujirecipes.ui.camera.CameraConnected
import com.paeki.fujirecipes.ui.camera.ConnectGuide
import com.paeki.fujirecipes.ui.components.AppHeader
import com.paeki.fujirecipes.ui.components.DragHandle
import com.paeki.fujirecipes.ui.detail.RecipeDetailScreen
import com.paeki.fujirecipes.ui.library.LibraryScreen
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.profile.ProfileScreen
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.BorderStrong
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelHigh
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

enum class AppTab { Camera, Library, Profile }

data class FujiSyncUiState(
    val connected: Boolean = false,
    val cameraModel: String = "X-H2",
    val firmware: String = "7.10",
    val battery: String = "87%",
    val tab: AppTab = AppTab.Camera,
    val selectedSlotIdx: Int = 0,
    val slots: List<RecipeUiModel> = emptyList(),
    val library: List<LibraryRecipeUiModel> = emptyList(),
    val librarySort: String = "NEWEST",
    val detailRecipe: RecipeUiModel? = null,
    val writeBusy: Boolean = false,
    val writeToast: WriteToastState? = null,
)

data class WriteToastState(val slot: String, val name: String)

@Composable
fun FujiSyncApp(
    state: FujiSyncUiState,
    onReconnect: () -> Unit,
    onTabChange: (AppTab) -> Unit,
    onSelectSlot: (Int) -> Unit,
    onOpenDetail: (RecipeUiModel) -> Unit,
    onCloseDetail: () -> Unit,
    onWrite: () -> Unit,
    onToggleLibrarySort: () -> Unit,
    onOpenLibraryItem: (LibraryRecipeUiModel) -> Unit,
) {
    var cameraSheetExpanded by rememberSaveable { mutableStateOf(false) }
    val cameraNames = remember { mutableStateListOf("My Camera", "My Camera", "My Camera") }
    var cameraDetail by remember { mutableStateOf<Pair<Int, CameraCardUiModel>?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                connected = state.connected,
                cameraModel = state.cameraModel,
                sheetExpanded = state.tab == AppTab.Camera && cameraSheetExpanded,
                onReconnect = onReconnect,
            )

            Box(modifier = Modifier.weight(1f)) {
                when (state.tab) {
                    AppTab.Camera -> {
                        if (state.connected) {
                            CameraConnected(
                                cameraModel = state.cameraModel,
                                firmware = state.firmware,
                                battery = state.battery,
                                slots = state.slots,
                                selectedSlotIdx = state.selectedSlotIdx,
                                onSelectSlot = onSelectSlot,
                                onOpenDetail = {
                                    state.slots.getOrNull(state.selectedSlotIdx)?.let(onOpenDetail)
                                },
                                onWrite = onWrite,
                                writeBusy = state.writeBusy,
                                cameraNames = cameraNames,
                                onOpenCameraDetail = { idx, cam -> cameraDetail = idx to cam },
                                onSheetExpandedChange = { cameraSheetExpanded = it },
                            )
                        } else {
                            ConnectGuide(onSimulateConnect = onReconnect)
                        }
                    }
                    AppTab.Library -> LibraryScreen(
                        recipes = state.library,
                        sortBy = state.librarySort,
                        onToggleSort = onToggleLibrarySort,
                        onOpenItem = onOpenLibraryItem,
                    )
                    AppTab.Profile -> ProfileScreen(
                        cameraModel = state.cameraModel,
                        connected = state.connected,
                    )
                }

                // Recipe detail overlay (full-screen, on top)
                Box(modifier = Modifier.fillMaxSize()) {
                    RecipeDetailScreen(
                        recipe = state.detailRecipe,
                        connected = state.connected,
                        onClose = onCloseDetail,
                        onWrite = onWrite,
                        writeBusy = state.writeBusy,
                    )
                }

                // Write toast
                state.writeToast?.let { toast ->
                    WriteToast(
                        slot = toast.slot,
                        name = toast.name,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        )
                }

                cameraDetail?.let { (idx, cam) ->
                    CameraDetailModal(
                        camera = cam,
                        name = cameraNames.getOrNull(idx) ?: "My Camera",
                        onRename = { nextName ->
                            if (idx in cameraNames.indices) cameraNames[idx] = nextName
                        },
                        onDelete = { cameraDetail = null },
                        onClose = { cameraDetail = null },
                    )
                }
            }

            AppTabBar(tab = state.tab, onTabChange = onTabChange)
        }
    }
}

@Composable
private fun BoxScope.CameraDetailModal(
    camera: CameraCardUiModel,
    name: String,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(name) { mutableStateOf(name) }
    val modalInteraction = remember { MutableInteractionSource() }
    fun commitRename() {
        val trimmed = draft.trim().ifBlank { "My Camera" }
        onRename(trimmed)
        draft = trimmed
        editing = false
    }

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(PanelHigh)
                .border(1.dp, BorderStrong, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .clickable(
                    interactionSource = modalInteraction,
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState()),
        ) {
            DragHandle()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = camera.model,
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.3.sp,
                        color = Gold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = name,
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(IconClose, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(17.dp))
                }
            }
            DividerLine()
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                Text(
                    text = "CUSTOM LABEL",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.sp,
                    color = TextDim,
                )
                Spacer(Modifier.height(8.dp))
                if (editing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                .background(PanelLow)
                                .border(1.dp, Gold, RoundedCornerShape(10.dp))
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.Enter -> {
                                            commitRename()
                                            true
                                        }
                                        Key.Escape -> {
                                            draft = name
                                            editing = false
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                .padding(horizontal = 13.dp, vertical = 10.dp),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Gold)
                                .clickable {
                                    commitRename()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "SAVE",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                letterSpacing = 1.3.sp,
                                color = Bg,
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PanelLow)
                            .border(1.dp, Border, RoundedCornerShape(10.dp))
                            .clickable { editing = true }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
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
                            text = "RENAME ›",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.8.sp,
                            color = Gold,
                        )
                    }
                }
            }
            DividerLine()
            Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp)) {
                CameraInfoRow("Model", camera.model, showDivider = true)
                CameraInfoRow("Firmware", camera.firmware, showDivider = true)
                CameraInfoRow("Battery", camera.battery, showDivider = true)
                CameraInfoRow("Last Sync", camera.lastSync, showDivider = false)
            }
            Column(
                modifier = Modifier
                    .padding(start = 22.dp, end = 22.dp, top = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("USB ID", fontFamily = SansFamily, fontSize = 12.sp, color = TextMuted)
                    Text(camera.usbId, fontFamily = MonoFamily, fontSize = 12.sp, letterSpacing = 0.7.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Not the camera serial number — this is the USB device identifier assigned by the host.",
                    fontFamily = SansFamily,
                    fontSize = 10.5.sp,
                    lineHeight = 16.sp,
                    color = TextDim,
                )
            }
            Box(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 28.dp)) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0x1AD23737))
                        .border(1.dp, Color(0x40D23737), RoundedCornerShape(11.dp)),
                ) {
                    Text(
                        text = "Remove Camera",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFFD94040),
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraInfoRow(label: String, value: String, showDivider: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = SansFamily, fontSize = 13.sp, letterSpacing = 0.2.sp, color = TextMuted)
        Text(value, fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, color = TextPrimary)
    }
    if (showDivider) DividerLine()
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}

// ── Tab bar ───────────────────────────────────────────────────────
private data class TabItem(val id: AppTab, val label: String, val icon: ImageVector)

@Composable
private fun AppTabBar(tab: AppTab, onTabChange: (AppTab) -> Unit) {
    val tabs = listOf(
        TabItem(AppTab.Camera, "CAMERA", IconCamera),
        TabItem(AppTab.Library, "LIBRARY", IconFolder),
        TabItem(AppTab.Profile, "PROFILE", IconProfile),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        ) {
            tabs.forEach { t ->
                val active = tab == t.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabChange(t.id) }
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(if (active) 24.dp else 0.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Gold),
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            t.icon,
                            contentDescription = t.label,
                            tint = if (active) Gold else TextMuted,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = t.label,
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.6.sp,
                            color = if (active) Gold else TextMuted,
                        )
                    }
                }
            }
        }
    }
}

// ── Write toast ───────────────────────────────────────────────────
@Composable
fun WriteToast(
    slot: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(com.paeki.fujirecipes.ui.theme.PanelHigh)
            .border(1.dp, Gold, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "✓", color = Gold, fontSize = 16.sp)
        Column {
            Text(
                text = "Wrote $name → $slot",
                fontFamily = com.paeki.fujirecipes.ui.theme.SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = com.paeki.fujirecipes.ui.theme.TextPrimary,
            )
            Text(
                text = "RECIPE LIVE ON CAMERA",
                fontFamily = MonoFamily,
                fontSize = 10.5.sp,
                letterSpacing = 1.sp,
                color = TextMuted,
            )
        }
    }
}
