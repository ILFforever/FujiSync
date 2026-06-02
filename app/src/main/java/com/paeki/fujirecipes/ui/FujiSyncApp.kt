package com.paeki.fujirecipes.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.paeki.fujirecipes.ui.overlay.BackHandler
import com.paeki.fujirecipes.ui.overlay.OverlayLayer
import com.paeki.fujirecipes.ui.overlay.overlayStackOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
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
import com.paeki.fujirecipes.ui.components.IconCamera
import com.paeki.fujirecipes.ui.components.IconClose
import com.paeki.fujirecipes.ui.components.IconFolder
import com.paeki.fujirecipes.ui.components.IconProfile
import com.paeki.fujirecipes.ui.components.IconSearch
import com.paeki.fujirecipes.ui.discover.DiscoverScreen
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.paeki.fujirecipes.ui.camera.CameraImageTunerScreen
import com.paeki.fujirecipes.ui.dev.ExifBenchScreen
import com.paeki.fujirecipes.ui.dev.WriteDelayBenchScreen
import com.paeki.fujirecipes.ui.dev.WriteDelayBenchViewModel
import com.paeki.fujirecipes.ui.camera.ConnectGuide
import com.paeki.fujirecipes.ui.components.AppHeader
import com.paeki.fujirecipes.ui.components.DeleteConfirmDialog
import com.paeki.fujirecipes.ui.components.DuplicateDialog
import com.paeki.fujirecipes.ui.detail.RecipeDetailScreen
import com.paeki.fujirecipes.ui.editor.RecipeEditorScreen
import com.paeki.fujirecipes.ui.library.LibraryScreen
import com.paeki.fujirecipes.ui.model.AppSettings
import com.paeki.fujirecipes.ui.model.DuplicateDialogState
import com.paeki.fujirecipes.ui.model.SaveAllReport
import com.paeki.fujirecipes.ui.model.LibraryGroupUiModel
import com.paeki.fujirecipes.ui.model.LibraryGroupStyle
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import com.paeki.fujirecipes.ui.model.LibraryRecipeSource
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.model.SlotBackupMeta
import com.paeki.fujirecipes.ui.profile.ProfileScreen
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

enum class AppTab { Camera, Library, Discover, Profile }

/**
 * Typed representation of the currently active screen. Derived from [FujiSyncUiState];
 * source of truth for back-stack behavior and overlay ordering.
 */
sealed class Screen {
    data class Tab(val tab: AppTab) : Screen()
    data class RecipeDetail(val recipe: RecipeUiModel) : Screen()
    /** [recipe] is null when creating a new recipe. */
    data class RecipeEditor(val recipe: RecipeUiModel?) : Screen()
}

val FujiSyncUiState.currentScreen: Screen
    get() = when {
        creatingRecipe || editorRecipe != null -> Screen.RecipeEditor(editorRecipe)
        detailRecipe != null -> Screen.RecipeDetail(detailRecipe)
        else -> Screen.Tab(tab)
    }

data class CameraUiState(
    val connected: Boolean = false,
    val scanning: Boolean = false,
    val scanError: String? = null,
    val readingSlots: Boolean = false,
    val readingSlotIndex: Int = -1,
    val cameraModel: String = "",
    val cameraSerial: String = "",
    val firmware: String = "",
    val battery: String = "",
    val slots: List<RecipeUiModel> = emptyList(),
    val selectedSlotIdx: Int = 0,
    val hasSlotBackup: Boolean = false,
    val slotBackupMeta: SlotBackupMeta? = null,
    val slotBackupSlots: List<RecipeUiModel>? = null,
    val restoringSlots: Boolean = false,
    val restoringSlotIndex: Int = -1,
    val cameraLabels: Map<String, String> = emptyMap(),
    val cameraModels: Map<String, String> = emptyMap(),
    val cameraFirmwares: Map<String, String> = emptyMap(),
    val showImageTuner: Boolean = false,
)

data class LibraryUiState(
    val recipes: List<LibraryRecipeUiModel> = emptyList(),
    val groups: List<LibraryGroupUiModel> = emptyList(),
    val sort: String = "NEWEST",
    val groupStyles: Map<String, LibraryGroupStyle> = emptyMap(),
    val saveConfirmed: Boolean = false,
    val duplicateDialog: DuplicateDialogState? = null,
)

data class FujiSyncUiState(
    val tab: AppTab = AppTab.Camera,
    val detailRecipe: RecipeUiModel? = null,
    val editorRecipe: RecipeUiModel? = null,
    val creatingRecipe: Boolean = false,
    val editorReferenceImageUris: List<String> = emptyList(),
    val writeBusy: Boolean = false,
    val writeToast: WriteToastState? = null,
    val camera: CameraUiState = CameraUiState(),
    val library: LibraryUiState = LibraryUiState(),
    val settings: AppSettings = AppSettings(),
    val exifImportLoading: Boolean = false,
    val exifImportError: String? = null,
    val ocrImportLoading: Boolean = false,
    val ocrImportError: String? = null,
    val saveAllSlotsConfirmed: Boolean = false,
    val saveAllReport: SaveAllReport? = null,
)

data class WriteToastState(val slot: String, val name: String, val savedToLibrary: Boolean = false)

@Composable
fun FujiSyncApp(
    state: FujiSyncUiState,
    onReconnect: () -> Unit,
    onTabChange: (AppTab) -> Unit,
    onSelectSlot: (Int) -> Unit,
    onOpenDetail: (RecipeUiModel) -> Unit,
    onCloseDetail: () -> Unit,
    onOpenRecipeCreator: () -> Unit,
    onOpenRecipeEditor: (RecipeUiModel) -> Unit,
    onCloseRecipeEditor: () -> Unit,
    onSaveRecipeDraft: (RecipeUiModel) -> Unit,
    onWrite: () -> Unit,
    onSaveToLibrary: (LibraryRecipeSource) -> Unit,
    onAddReferenceImage: (RecipeUiModel) -> Unit,
    onRemoveReferenceImage: (RecipeUiModel, String) -> Unit,
    onAddEditorReferenceImage: () -> Unit,
    onRemoveEditorReferenceImage: (String) -> Unit,
    onDeleteLibraryRecipes: (Set<String>) -> Unit,
    onCloneLibraryRecipe: (RecipeUiModel) -> Unit,
    onAddLibraryGroupImage: (String) -> Unit,
    onOpenLibraryItem: (LibraryRecipeUiModel) -> Unit,
    onOpenCameraImageTuner: () -> Unit,
    onCloseCameraImageTuner: () -> Unit,
    onLoadSampleLibrary: () -> Unit,
    onDuplicateSaveAsNew: () -> Unit,
    onDuplicateUpdateExisting: (String) -> Unit,
    onDuplicateDismiss: () -> Unit,
    onExploreDemo: () -> Unit,
    onBackupSlots: (String) -> Unit,
    onRestoreSlots: () -> Unit,
    onDeleteSlotBackup: () -> Unit,
    onRenameSlotBackup: (String) -> Unit,
    onRenameCameraLabel: (String, String) -> Unit,
    onDeleteCamera: (String) -> Unit = {},
    onResetCameraLabel: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit,
    onToggleLibraryShowImages: () -> Unit,
    onWriteLibraryRecipeToSlot: (String) -> Unit = {},
    onImportFromPhoto: () -> Unit = {},
    onExifImportErrorDismiss: () -> Unit = {},
    onImportFromScreenshot: () -> Unit = {},
    onOcrImportErrorDismiss: () -> Unit = {},
    onAddMockCamera: () -> Unit = {},
    onSaveAllToLibrary: (LibraryRecipeSource) -> Unit = {},
    onSaveAllReportDismiss: () -> Unit = {},
) {
    var showExifBench by remember { mutableStateOf(false) }
    var showWriteDelayBench by remember { mutableStateOf(false) }
    var cameraSheetRevealProgress by rememberSaveable { mutableStateOf(0f) }
    val cameraLabel = state.camera.cameraLabels[state.camera.cameraSerial] ?: "My Camera"
    var cameraDetail by remember { mutableStateOf<Pair<Int, CameraCardUiModel>?>(null) }
    var showReadingOverlay by remember { mutableStateOf(false) }
    var editorDirty by remember { mutableStateOf(false) }
    var showDiscardEditorDialog by remember { mutableStateOf(false) }
    var pendingEditorTab by remember { mutableStateOf<AppTab?>(null) }
    val editorOpen = state.creatingRecipe || state.editorRecipe != null
    var showImportFromPhotoGuide by remember { mutableStateOf(false) }
    var showImportFromScreenshotGuide by remember { mutableStateOf(false) }
    LaunchedEffect(state.editorRecipe) {
        if (showImportFromPhotoGuide && state.editorRecipe != null) {
            showImportFromPhotoGuide = false
        }
        if (showImportFromScreenshotGuide && state.editorRecipe != null) {
            showImportFromScreenshotGuide = false
        }
    }

    fun discardEditorAndContinue() {
        showDiscardEditorDialog = false
        editorDirty = false
        onCloseRecipeEditor()
        pendingEditorTab?.let(onTabChange)
        pendingEditorTab = null
    }

    fun requestEditorClose(nextTab: AppTab? = null) {
        if (!editorOpen) {
            nextTab?.let(onTabChange)
            return
        }
        if (editorDirty) {
            pendingEditorTab = nextTab
            showDiscardEditorDialog = true
        } else {
            editorDirty = false
            onCloseRecipeEditor()
            nextTab?.let(onTabChange)
        }
    }

    LaunchedEffect(state.camera.readingSlots) {
        if (state.camera.readingSlots) showReadingOverlay = true
    }

    LaunchedEffect(editorOpen) {
        if (!editorOpen) {
            editorDirty = false
            showDiscardEditorDialog = false
            pendingEditorTab = null
        }
    }

    overlayStackOf(
        OverlayLayer(state.detailRecipe != null) { onCloseDetail() },
        OverlayLayer(showImportFromPhotoGuide) { showImportFromPhotoGuide = false },
        OverlayLayer(showImportFromScreenshotGuide) { showImportFromScreenshotGuide = false },
        OverlayLayer(state.exifImportError != null) { onExifImportErrorDismiss() },
        OverlayLayer(state.exifImportLoading) { },  // blocks back during import, no dismiss action
        OverlayLayer(state.ocrImportError != null) { onOcrImportErrorDismiss() },
        OverlayLayer(state.ocrImportLoading) { },
        OverlayLayer(state.camera.restoringSlots) { },  // blocks back while writing the set to camera
        OverlayLayer(showExifBench) { showExifBench = false },
        OverlayLayer(showWriteDelayBench) { showWriteDelayBench = false },
        OverlayLayer(state.camera.showImageTuner) { onCloseCameraImageTuner() },
        OverlayLayer(showReadingOverlay) { showReadingOverlay = false },
        OverlayLayer(cameraDetail != null) { cameraDetail = null },
        OverlayLayer(editorOpen) { requestEditorClose() },
        OverlayLayer(state.library.duplicateDialog != null) { onDuplicateDismiss() },
    ).BackHandler()

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.tab == AppTab.Camera) {
                AppHeader(
                    connected = state.camera.connected,
                    cameraModel = state.camera.cameraModel,
                    sheetRevealProgress = cameraSheetRevealProgress,
                    onReconnect = onReconnect,
                    showConnectionStatus = state.camera.connected,
                    showDisconnectedStatus = false,
                    showReconnectButton = state.camera.connected,
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (state.tab) {
                    AppTab.Camera -> {
                        if (state.camera.connected) {
                            CameraConnected(
                                cameraModel = state.camera.cameraModel,
                                firmware = state.camera.firmware,
                                battery = state.camera.battery,
                                readingSlots = state.camera.readingSlots,
                                slots = state.camera.slots,
                                selectedSlotIdx = state.camera.selectedSlotIdx,
                                onSelectSlot = onSelectSlot,
                                onOpenDetail = {
                                    state.camera.slots.getOrNull(state.camera.selectedSlotIdx)?.let(onOpenDetail)
                                },
                                onSaveToLibrary = onSaveToLibrary,
                                writeBusy = state.writeBusy,
                                librarySaveConfirmed = state.library.saveConfirmed,
                                hasSlotBackup = state.camera.hasSlotBackup,
                                slotBackupMeta = state.camera.slotBackupMeta,
                                slotBackupSlots = state.camera.slotBackupSlots,
                                onBackupSlots = onBackupSlots,
                                onRestoreSlots = onRestoreSlots,
                                onDeleteSlotBackup = onDeleteSlotBackup,
                                onRenameSlotBackup = onRenameSlotBackup,
                                cameraSerial = state.camera.cameraSerial,
                                cameraNames = listOf(cameraLabel),
                                onOpenCameraDetail = { idx, cam -> cameraDetail = idx to cam },
                                onSheetRevealProgressChange = { cameraSheetRevealProgress = it },
                                onSaveAllToLibrary = onSaveAllToLibrary,
                                saveAllSlotsConfirmed = state.saveAllSlotsConfirmed,
                                saveAllReport = state.saveAllReport,
                                onSaveAllReportDismiss = onSaveAllReportDismiss,
                            )
                        } else {
                            ConnectGuide(
                            scanning = state.camera.scanning,
                            scanError = state.camera.scanError,
                            onSimulateConnect = onReconnect,
                        )
                        }
                    }
                    AppTab.Library -> LibraryScreen(
                        showImages = state.settings.showLibraryImages,
                        scrollToTopSignal = state.library.saveConfirmed,
                        onOpenItem = onOpenLibraryItem,
                        onCreateRecipe = onOpenRecipeCreator,
                        onAddGroupImage = onAddLibraryGroupImage,
                    )
                    AppTab.Discover -> DiscoverScreen()
                    AppTab.Profile -> ProfileScreen(
                        cameraLabels = state.camera.cameraLabels,
                        cameraModels = state.camera.cameraModels,
                        cameraFirmwares = state.camera.cameraFirmwares,
                        activeCameraSerial = state.camera.cameraSerial,
                        activeCameraModel = state.camera.cameraModel,
                        onRenameCameraLabel = onRenameCameraLabel,
                        onDeleteCamera = onDeleteCamera,
                        onResetCameraLabel = onResetCameraLabel,
                        settings = state.settings,
                        onToggleLibraryShowImages = onToggleLibraryShowImages,
                        onOpenCameraImageTuner = onOpenCameraImageTuner,
                        onLoadSampleLibrary = onLoadSampleLibrary,
                        onExploreDemo = onExploreDemo,
                        onOpenExifBench = { showExifBench = true },
                        onOpenWriteDelayBench = { showWriteDelayBench = true },
                        onImportFromPhoto = { showImportFromPhotoGuide = true },
                        onImportFromScreenshot = { showImportFromScreenshotGuide = true },
                        onAddMockCamera = onAddMockCamera,
                    )
                }

                AppOverlays(
                    state = state,
                    cameraLabel = cameraLabel,
                    cameraDetail = cameraDetail,
                    showExifBench = showExifBench,
                    showWriteDelayBench = showWriteDelayBench,
                    showImportFromPhotoGuide = showImportFromPhotoGuide,
                    showReadingOverlay = showReadingOverlay,
                    showDiscardEditorDialog = showDiscardEditorDialog,
                    onCloseDetail = onCloseDetail,
                    onWrite = onWrite,
                    onAddReferenceImage = onAddReferenceImage,
                    onRemoveReferenceImage = onRemoveReferenceImage,
                    onToggleFavorite = onToggleFavorite,
                    onCloneLibraryRecipe = onCloneLibraryRecipe,
                    onDeleteLibraryRecipes = onDeleteLibraryRecipes,
                    onWriteLibraryRecipeToSlot = onWriteLibraryRecipeToSlot,
                    onOpenRecipeEditor = onOpenRecipeEditor,
                    onCloseCameraImageTuner = onCloseCameraImageTuner,
                    onImportFromPhoto = onImportFromPhoto,
                    onAddEditorReferenceImage = onAddEditorReferenceImage,
                    onRemoveEditorReferenceImage = onRemoveEditorReferenceImage,
                    onSaveRecipeDraft = onSaveRecipeDraft,
                    requestEditorClose = ::requestEditorClose,
                    onEditorDirtyChange = { editorDirty = it },
                    onDiscardConfirm = ::discardEditorAndContinue,
                    onDiscardDismiss = { showDiscardEditorDialog = false; pendingEditorTab = null },
                    onReadingOverlayDismiss = { showReadingOverlay = false },
                    onCameraDetailClose = { cameraDetail = null },
                    onCameraRename = { nextName -> onRenameCameraLabel(state.camera.cameraSerial, nextName) },
                    onDuplicateSaveAsNew = onDuplicateSaveAsNew,
                    onDuplicateUpdateExisting = onDuplicateUpdateExisting,
                    onDuplicateDismiss = onDuplicateDismiss,
                    onExifBenchClose = { showExifBench = false },
                    onWriteDelayBenchClose = { showWriteDelayBench = false },
                    onImportFromPhotoGuideClose = { showImportFromPhotoGuide = false },
                    showImportFromScreenshotGuide = showImportFromScreenshotGuide,
                    onImportFromScreenshotGuideClose = { showImportFromScreenshotGuide = false },
                    onImportFromScreenshot = onImportFromScreenshot,
                    onExifImportErrorDismiss = onExifImportErrorDismiss,
                    onExifImportRetry = onImportFromPhoto,
                    onOcrImportErrorDismiss = onOcrImportErrorDismiss,
                    onOcrImportRetry = onImportFromScreenshot,
                )
            }

            val isCharging = rememberIsPhoneCharging()
            var chargingBannerDismissed by remember { mutableStateOf(false) }
            AnimatedVisibility(
                visible = state.tab == AppTab.Camera && isCharging && !chargingBannerDismissed,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                ChargingWarningBanner(onDismiss = { chargingBannerDismissed = true })
            }

            AppTabBar(
                tab = state.tab,
                onTabChange = { tab ->
                    if (showImportFromPhotoGuide) {
                        showImportFromPhotoGuide = false
                        onTabChange(tab)
                    } else if (showImportFromScreenshotGuide) {
                        showImportFromScreenshotGuide = false
                        onTabChange(tab)
                    } else if (editorOpen && tab != state.tab) {
                        requestEditorClose(tab)
                    } else {
                        onTabChange(tab)
                    }
                },
            )
        }
    }
}

@Composable
private fun BoxScope.AppOverlays(
    state: FujiSyncUiState,
    cameraLabel: String,
    cameraDetail: Pair<Int, CameraCardUiModel>?,
    showExifBench: Boolean,
    showWriteDelayBench: Boolean,
    showImportFromPhotoGuide: Boolean,
    showReadingOverlay: Boolean,
    showDiscardEditorDialog: Boolean,
    onCloseDetail: () -> Unit,
    onWrite: () -> Unit,
    onAddReferenceImage: (RecipeUiModel) -> Unit,
    onRemoveReferenceImage: (RecipeUiModel, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCloneLibraryRecipe: (RecipeUiModel) -> Unit,
    onDeleteLibraryRecipes: (Set<String>) -> Unit,
    onWriteLibraryRecipeToSlot: (String) -> Unit,
    onOpenRecipeEditor: (RecipeUiModel) -> Unit,
    onCloseCameraImageTuner: () -> Unit,
    onImportFromPhoto: () -> Unit,
    onAddEditorReferenceImage: () -> Unit,
    onRemoveEditorReferenceImage: (String) -> Unit,
    onSaveRecipeDraft: (RecipeUiModel) -> Unit,
    requestEditorClose: () -> Unit,
    onEditorDirtyChange: (Boolean) -> Unit,
    onDiscardConfirm: () -> Unit,
    onDiscardDismiss: () -> Unit,
    onReadingOverlayDismiss: () -> Unit,
    onCameraDetailClose: () -> Unit,
    onCameraRename: (String) -> Unit,
    onDuplicateSaveAsNew: () -> Unit,
    onDuplicateUpdateExisting: (String) -> Unit,
    onDuplicateDismiss: () -> Unit,
    onExifBenchClose: () -> Unit,
    onWriteDelayBenchClose: () -> Unit,
    onImportFromPhotoGuideClose: () -> Unit,
    showImportFromScreenshotGuide: Boolean,
    onImportFromScreenshotGuideClose: () -> Unit,
    onImportFromScreenshot: () -> Unit,
    onExifImportErrorDismiss: () -> Unit,
    onExifImportRetry: () -> Unit,
    onOcrImportErrorDismiss: () -> Unit,
    onOcrImportRetry: () -> Unit,
) {
    val writeDelayBenchVm: WriteDelayBenchViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    Box(modifier = Modifier.fillMaxSize()) {
        RecipeDetailScreen(
            recipe = state.detailRecipe,
            connected = state.camera.connected,
            onClose = onCloseDetail,
            onWrite = onWrite,
            onAddReferenceImage = onAddReferenceImage,
            onRemoveReferenceImage = onRemoveReferenceImage,
            onToggleFavorite = { recipe -> recipe.libraryId?.let { onToggleFavorite(it) } },
            onEdit = onOpenRecipeEditor,
            onClone = onCloneLibraryRecipe,
            onDelete = { recipe -> recipe.libraryId?.let { onDeleteLibraryRecipes(setOf(it)) } },
            writeBusy = state.writeBusy,
            cameraModel = state.camera.cameraModel,
            cameraName = cameraLabel,
            cameraSlots = state.camera.slots,
            onWriteToSlot = onWriteLibraryRecipeToSlot,
        )
    }

    state.writeToast?.let { toast ->
        WriteToast(
            slot = toast.slot,
            name = toast.name,
            savedToLibrary = toast.savedToLibrary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }

    if (state.camera.showImageTuner) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            CameraImageTunerScreen(onClose = onCloseCameraImageTuner)
        }
    }

    if (showExifBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            ExifBenchScreen(onClose = onExifBenchClose)
        }
    }

    if (showWriteDelayBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            WriteDelayBenchScreen(viewModel = writeDelayBenchVm, onClose = onWriteDelayBenchClose)
        }
    }

    AnimatedVisibility(
        visible = showImportFromPhotoGuide,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
        exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
               slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            ImportFromPhotoGuide(
                onClose = onImportFromPhotoGuideClose,
                onChoosePhoto = onImportFromPhoto,
            )
        }
    }

    AnimatedVisibility(
        visible = showImportFromScreenshotGuide,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
        exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
               slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            ImportFromScreenshotGuide(
                onClose = onImportFromScreenshotGuideClose,
                onChooseScreenshot = onImportFromScreenshot,
            )
        }
    }

    if (state.exifImportLoading) {
        ExifImportLoadingScreen()
    }

    if (state.camera.restoringSlots) {
        RestoreSetLoadingScreen(currentSlotIndex = state.camera.restoringSlotIndex)
    }

    state.exifImportError?.let { error ->
        ExifImportErrorScreen(
            message = error,
            onDismiss = onExifImportErrorDismiss,
            onRetry = onExifImportRetry,
        )
    }

    if (state.ocrImportLoading) {
        ExifImportLoadingScreen(eyebrow = "READING IMAGE", subtitle = "Scanning for recipe settings")
    }

    state.ocrImportError?.let { error ->
        ExifImportErrorScreen(
            message = error,
            onDismiss = onOcrImportErrorDismiss,
            onRetry = onOcrImportRetry,
        )
    }

    AnimatedVisibility(
        visible = state.creatingRecipe || state.editorRecipe != null,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 12 },
        exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) +
               slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { it / 12 },
    ) {
        RecipeEditorScreen(
            initialRecipe = state.editorRecipe,
            referenceImageUris = state.editorReferenceImageUris,
            cameraModel = state.camera.cameraModel,
            onClose = requestEditorClose,
            onDirtyChange = onEditorDirtyChange,
            onAddReferenceImage = onAddEditorReferenceImage,
            onRemoveReferenceImage = onRemoveEditorReferenceImage,
            onSave = onSaveRecipeDraft,
        )
    }

    if (showDiscardEditorDialog) {
        DeleteConfirmDialog(
            title = "Discard changes?",
            body = "This recipe draft has unsaved changes. You can keep editing or discard the draft.",
            confirmLabel = "Discard",
            eyebrow = "DISCARD",
            onConfirm = onDiscardConfirm,
            onDismiss = onDiscardDismiss,
        )
    }

    if (showReadingOverlay) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.paeki.fujirecipes.ui.profile.SlotReadingAnimationMockup(
                currentSlotIndex = state.camera.readingSlotIndex,
                loadedSlots = state.camera.slots,
                isDone = !state.camera.readingSlots,
                onDismissed = onReadingOverlayDismiss,
            )
        }
    }

    cameraDetail?.let { (_, cam) ->
        CameraDetailModal(
            camera = cam,
            name = cameraLabel,
            onRename = onCameraRename,
            onClose = onCameraDetailClose,
        )
    }

    state.library.duplicateDialog?.let { dialog ->
        DuplicateDialog(
            dialog = dialog,
            onSaveAsNew = onDuplicateSaveAsNew,
            onUpdateExisting = { onDuplicateUpdateExisting(dialog.topMatch.libraryRecipe.id) },
            onDismiss = onDuplicateDismiss,
        )
    }
}

@Composable
private fun BoxScope.CameraDetailModal(
    camera: CameraCardUiModel,
    name: String,
    onRename: (String) -> Unit,
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
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
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
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 14.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(TextDim.copy(alpha = 0.4f)),
            )

            // Header — model ID + custom name + DONE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 16.dp, top = 10.dp, bottom = 20.dp),
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
                        .clickable(onClick = onClose)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                )
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
        TabItem(AppTab.Discover, "DISCOVER", IconSearch),
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
                        .padding(top = 5.dp, bottom = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            t.icon,
                            contentDescription = t.label,
                            tint = if (active) Gold else TextMuted,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = t.label,
                            fontFamily = MonoFamily,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.2.sp,
                            color = if (active) Gold else TextMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) Gold else Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

// ── Charging warning ──────────────────────────────────────────────

@Composable
private fun rememberIsPhoneCharging(): Boolean {
    val context = LocalContext.current
    return produceState(initialValue = context.isCharging()) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: Intent) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        awaitDispose { context.unregisterReceiver(receiver) }
    }.value
}

private fun Context.isCharging(): Boolean {
    val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
}

@Composable
private fun ChargingWarningBanner(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelLow)
            .border(width = 1.dp, color = Gold.copy(alpha = 0.35f), shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "⚡",
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 1.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Phone is charging via USB",
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary,
            )
            Text(
                text = "If the camera is charging your phone instead of syncing, unplug and reconnect the cable — repeat until the Fuji screen goes black.",
                fontFamily = SansFamily,
                fontSize = 12.sp,
                color = TextMuted,
            )
        }
        Icon(
            imageVector = IconClose,
            contentDescription = "Dismiss",
            tint = TextDim,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .padding(1.dp),
        )
    }
}

// ── Restore set loading ───────────────────────────────────────────

@Composable
private fun RestoreSetLoadingScreen(currentSlotIndex: Int) {
    val transition = rememberInfiniteTransition(label = "restore-set")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "restore-pulse",
    )
    val activeIndex = currentSlotIndex.coerceIn(0, 6)
    val progress = ((activeIndex + 1) / 7f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg.copy(alpha = 0.66f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(318.dp)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(PanelLow)
                .border(
                    1.dp,
                    Gold.copy(alpha = 0.22f + 0.12f * pulse),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                )
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "RESTORE SET",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "Writing C${activeIndex + 1} of C7 to camera",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "Keep the USB cable connected until the set finishes.",
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PanelHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Gold.copy(alpha = 0.8f)),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(7) { index ->
                    val active = index == activeIndex
                    val completed = index < activeIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (active) 38.dp else 32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        active -> Gold.copy(alpha = 0.14f + 0.16f * pulse)
                                        completed -> Gold.copy(alpha = 0.12f)
                                        else -> PanelHigh
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        active -> Gold.copy(alpha = 0.62f + 0.22f * pulse)
                                        completed -> Gold.copy(alpha = 0.32f)
                                        else -> Border
                                    },
                                    RoundedCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "C${index + 1}",
                                fontFamily = MonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (active || completed) Gold else TextDim,
                            )
                        }
                        Text(
                            text = when {
                                completed -> "DONE"
                                active -> "NOW"
                                else -> "WAIT"
                            },
                            fontFamily = MonoFamily,
                            fontSize = 7.sp,
                            letterSpacing = 0.6.sp,
                            color = if (active) Gold else TextDim,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelHigh)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SLOTS RESTORED",
                    fontFamily = MonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    color = TextDim,
                )
                Text(
                    text = "${activeIndex + 1}/7",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Gold,
                )
            }
        }
    }
}

// ── EXIF import loading ───────────────────────────────────────────

@Composable
private fun ExifImportLoadingScreen(
    eyebrow: String = "READING PHOTO",
    subtitle: String = "Extracting recipe data",
) {
    val transition = rememberInfiniteTransition(label = "exif-load")

    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan-y",
    )
    val borderAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "border-pulse",
    )
    val textAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "text-pulse",
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            // ── Scan frame ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PanelLow)
                    .border(1.dp, Gold.copy(alpha = borderAlpha), RoundedCornerShape(18.dp)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gold = Color(0xFFC99A4E)
                    val inset = 11.dp.toPx()
                    val innerTop = inset
                    val innerBottom = size.height - inset
                    val y = innerTop + (innerBottom - innerTop) * scanProgress
                    val trailH = (innerBottom - innerTop) * 0.4f

                    // Glow trail above scan line, clipped to inner rect
                    if (y > innerTop) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, gold.copy(alpha = 0.10f)),
                                startY = maxOf(innerTop, y - trailH),
                                endY = y,
                            ),
                            topLeft = Offset(inset, maxOf(innerTop, y - trailH)),
                            size = Size(size.width - inset * 2, minOf(trailH, y - innerTop)),
                        )
                    }

                    // Scan line — gradient fade at edges, spans inner rect width
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                gold.copy(alpha = 0.55f),
                                gold,
                                gold.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                            startX = inset,
                            endX = size.width - inset,
                        ),
                        start = Offset(inset, y),
                        end = Offset(size.width - inset, y),
                        strokeWidth = 1.5.dp.toPx(),
                    )

                    // Corner brackets — viewfinder aesthetic
                    val arm = 13.dp.toPx()
                    val pad = 11.dp.toPx()
                    val sw = 1.5.dp.toPx()
                    // Top-left
                    drawLine(gold, Offset(pad, pad), Offset(pad + arm, pad), sw)
                    drawLine(gold, Offset(pad, pad), Offset(pad, pad + arm), sw)
                    // Top-right
                    drawLine(gold, Offset(size.width - pad, pad), Offset(size.width - pad - arm, pad), sw)
                    drawLine(gold, Offset(size.width - pad, pad), Offset(size.width - pad, pad + arm), sw)
                    // Bottom-left
                    drawLine(gold, Offset(pad, size.height - pad), Offset(pad + arm, size.height - pad), sw)
                    drawLine(gold, Offset(pad, size.height - pad), Offset(pad, size.height - pad - arm), sw)
                    // Bottom-right
                    drawLine(gold, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - arm, size.height - pad), sw)
                    drawLine(gold, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - arm), sw)
                }
            }

            // ── Labels ─────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = eyebrow,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = TextPrimary.copy(alpha = textAlpha),
                )
                Text(
                    text = subtitle,
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextDim.copy(alpha = textAlpha * 0.7f),
                )
            }
        }
    }
}

// ── EXIF import error ─────────────────────────────────────────────

@Composable
private fun ExifImportErrorScreen(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // ── Icon frame — static, error state ──────────────────
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✕",
                    fontFamily = MonoFamily,
                    fontSize = 28.sp,
                    color = TextDim,
                )
            }

            // ── Labels ────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "NO RECIPE FOUND",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = TextPrimary,
                )
                Text(
                    text = message,
                    fontFamily = SansFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            // ── Actions ───────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                com.paeki.fujirecipes.ui.components.PrimaryCTA(
                    label = "Try Another Photo",
                    onClick = onRetry,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Done",
                        fontFamily = SansFamily,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        fontSize = 15.sp,
                        color = TextMuted,
                    )
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
    savedToLibrary: Boolean = false,
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
                text = if (savedToLibrary) "Saved $name" else "Wrote $name → $slot",
                fontFamily = com.paeki.fujirecipes.ui.theme.SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = com.paeki.fujirecipes.ui.theme.TextPrimary,
            )
            Text(
                text = if (savedToLibrary) "ADDED TO LIBRARY" else "RECIPE LIVE ON CAMERA",
                fontFamily = MonoFamily,
                fontSize = 10.5.sp,
                letterSpacing = 1.sp,
                color = TextMuted,
            )
        }
    }
}
