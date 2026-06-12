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
import androidx.compose.animation.core.updateTransition
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
import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.paeki.fujirecipes.ui.camera.CameraCardUiModel
import com.paeki.fujirecipes.ui.camera.CameraConnected
import com.paeki.fujirecipes.ui.camera.CameraImageTunerScreen
import com.paeki.fujirecipes.ui.dev.DrPriorityBenchScreen
import com.paeki.fujirecipes.ui.dev.DrPriorityBenchViewModel
import com.paeki.fujirecipes.ui.dev.ExifBenchScreen
import com.paeki.fujirecipes.ui.dev.HapticBenchScreen
import com.paeki.fujirecipes.ui.dev.PtpLogScreen
import com.paeki.fujirecipes.ui.dev.NameBenchScreen
import com.paeki.fujirecipes.ui.dev.NameBenchViewModel
import com.paeki.fujirecipes.ui.dev.ReadSlotsBenchScreen
import com.paeki.fujirecipes.ui.dev.ReadSlotsBenchViewModel
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
import com.paeki.fujirecipes.ui.model.SlotBackupSet
import com.paeki.fujirecipes.ui.profile.ProfileScreen
import com.paeki.fujirecipes.ui.qr.QrScannerScreen
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
import com.paeki.fujirecipes.ui.haptics.FujiHapticEffect
import com.paeki.fujirecipes.ui.haptics.FujiHaptics

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
    val backingUpSlots: Boolean = false,
    val backingUpSlotIndex: Int = -1,
    val hasSlotBackup: Boolean = false,
    val slotBackupMeta: SlotBackupMeta? = null,
    val slotBackupSlots: List<RecipeUiModel>? = null,
    val slotBackupSets: List<SlotBackupSet> = emptyList(),
    val restoringSlots: Boolean = false,
    val restoringSlotIndex: Int = -1,
    val rearrangingSlots: Boolean = false,
    val rearrangingSlotIndex: Int = -1,
    val rearrangingWriteIndex: Int = -1,
    val rearrangingWriteTotal: Int = 0,
    val isRestoringValidation: Boolean = false,
    val isRearrangeValidation: Boolean = false,
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
    val shutterCount: Int? = null,
    val shutterCheckLoading: Boolean = false,
    val shutterCheckError: String? = null,
    val ocrImportLoading: Boolean = false,
    val ocrImportError: String? = null,
    val ocrRawText: String? = null,
    val ocrParseResult: com.paeki.fujirecipes.data.ocr.OcrParseResult? = null,
    val qrImportLoading: Boolean = false,
    val qrImportError: String? = null,
    val saveAllSlotsConfirmed: Boolean = false,
    val saveAllReport: SaveAllReport? = null,
    val captureLog: String? = null,
    val update: UpdateUiState = UpdateUiState(),
    val backup: BackupUiState = BackupUiState(),
    val toastMessage: String? = null,
)

data class WriteToastState(val slot: String, val name: String, val savedToLibrary: Boolean = false)

data class UpdateUiState(
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val latestVersion: String? = null,
    val releaseName: String? = null,
    val assetName: String? = null,
    val updateAvailable: Boolean = false,
    val downloaded: Boolean = false,
    val installPermissionRequired: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

data class BackupUiState(
    val exporting: Boolean = false,
    val importing: Boolean = false,
    val exportProgress: Float = 0f,
    val exportTotal: Int = 0,
    val message: String? = null,
    val error: String? = null,
)

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
    onSelectSlotBackup: (String) -> Unit = {},
    onRearrangeCameraSlots: (List<RecipeUiModel>) -> Unit = {},
    onRearrangeValidationDismiss: () -> Unit = {},
    rearrangeDebugLog: String = "",
    onRenameCameraLabel: (String, String) -> Unit,
    onDeleteCamera: (String) -> Unit = {},
    onResetCameraLabel: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit,
    onToggleLibraryShowImages: () -> Unit,
    onToggleReferenceImageBlur: () -> Unit = {},
    onToggleFavoritesOnTop: () -> Unit = {},
    onToggleHaptics: () -> Unit = {},
    onWriteLibraryRecipeToSlot: (String) -> Unit = {},
    onImportFromPhoto: () -> Unit = {},
    onExifImportErrorDismiss: () -> Unit = {},
    onImportFromScreenshot: () -> Unit = {},
    onOcrImportErrorDismiss: () -> Unit = {},
    onQrRecipeDetected: (RecipeUiModel) -> Unit = {},
    onImportQrFromImage: () -> Unit = {},
    onQrImportErrorDismiss: () -> Unit = {},
    onAddMockCamera: () -> Unit = {},
    onSaveAllToLibrary: (LibraryRecipeSource) -> Unit = {},
    onSaveAllReportDismiss: () -> Unit = {},
    onLoadCaptureLog: () -> Unit = {},
    onClearCaptureLog: () -> Unit = {},
    onSetPropertyWriteDelay: (Long) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackupMerge: () -> Unit = {},
    onImportBackupReplace: () -> Unit = {},
    onDismissBackupMessage: () -> Unit = {},
    onShutterCheck: () -> Unit = {},
    onShutterCheckDismiss: () -> Unit = {},
) {
    var showExifBench by remember { mutableStateOf(false) }
    var showWriteDelayBench by remember { mutableStateOf(false) }
    var showNameBench by remember { mutableStateOf(false) }
    var showReadSlotsBench by remember { mutableStateOf(false) }
    var showDrPriorityBench by remember { mutableStateOf(false) }
    var showHapticBench by remember { mutableStateOf(false) }
    var showPtpLog by remember { mutableStateOf(false) }
    var showScanTileGuide by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var cameraSheetRevealProgress by rememberSaveable { mutableStateOf(0f) }
    SideEffect { FujiHaptics.enabled = state.settings.hapticsEnabled }

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

    LaunchedEffect(state.camera.readingSlots, state.camera.isRestoringValidation, state.camera.isRearrangeValidation, state.camera.restoringSlots) {
        if (state.camera.readingSlots && !state.camera.restoringSlots && !state.camera.isRestoringValidation && !state.camera.isRearrangeValidation) {
            showReadingOverlay = true
        } else if (state.camera.restoringSlots) {
            showReadingOverlay = false
        }
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
        OverlayLayer(showQrScanner) { showQrScanner = false },
        OverlayLayer(state.shutterCheckError != null) { onShutterCheckDismiss() },
        OverlayLayer(state.shutterCheckLoading) { },
        OverlayLayer(state.shutterCount != null) { onShutterCheckDismiss() },
        OverlayLayer(state.exifImportError != null) { onExifImportErrorDismiss() },
        OverlayLayer(state.exifImportLoading) { },  // blocks back during import, no dismiss action
        OverlayLayer(state.ocrImportError != null) { onOcrImportErrorDismiss() },
        OverlayLayer(state.ocrImportLoading) { },
        OverlayLayer(state.qrImportError != null) { onQrImportErrorDismiss() },
        OverlayLayer(state.qrImportLoading) { },
        OverlayLayer(state.camera.restoringSlots) { },  // blocks back while writing the set to camera
        OverlayLayer(state.camera.rearrangingSlots) { }, // blocks back while rearranging camera slots
        OverlayLayer(showExifBench) { showExifBench = false },
        OverlayLayer(showWriteDelayBench) { showWriteDelayBench = false },
        OverlayLayer(showNameBench) { showNameBench = false },
        OverlayLayer(showReadSlotsBench) { showReadSlotsBench = false },
        OverlayLayer(showDrPriorityBench) { showDrPriorityBench = false },
        OverlayLayer(showHapticBench) { showHapticBench = false },
        OverlayLayer(showPtpLog) { showPtpLog = false },
        OverlayLayer(showScanTileGuide) { showScanTileGuide = false },
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
                                backingUpSlots = state.camera.backingUpSlots,
                                backingUpSlotIndex = state.camera.backingUpSlotIndex,
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
                                slotBackupSets = state.camera.slotBackupSets,
                                restoringSlots = state.camera.restoringSlots,
                                onBackupSlots = onBackupSlots,
                                onRestoreSlots = onRestoreSlots,
                                onDeleteSlotBackup = onDeleteSlotBackup,
                                onRenameSlotBackup = onRenameSlotBackup,
                                onSelectSlotBackup = onSelectSlotBackup,
                                onRearrangeSlots = onRearrangeCameraSlots,
                                onRearrangeValidationDismiss = onRearrangeValidationDismiss,
                                readingSlotIndex = state.camera.readingSlotIndex,
                                isRestoringValidation = state.camera.isRestoringValidation,
                                isRearrangeValidation = state.camera.isRearrangeValidation,
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
                        favoritesOnTop = state.settings.favoritesOnTop,
                        scrollToTopSignal = state.library.saveConfirmed,
                        onOpenItem = onOpenLibraryItem,
                        onCreateRecipe = onOpenRecipeCreator,
                        onAddGroupImage = onAddLibraryGroupImage,
                        onImportFromPhoto = { showImportFromPhotoGuide = true },
                        onImportFromScreenshot = { showImportFromScreenshotGuide = true },
                        onImportFromQr = { showQrScanner = true },
                        onScanTileGuide = { showScanTileGuide = true },
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
                        onToggleReferenceImageBlur = onToggleReferenceImageBlur,
                        onToggleFavoritesOnTop = onToggleFavoritesOnTop,
                        onToggleHaptics = onToggleHaptics,
                        onOpenCameraImageTuner = onOpenCameraImageTuner,
                        onLoadSampleLibrary = onLoadSampleLibrary,
                        onExploreDemo = onExploreDemo,
                        onOpenExifBench = { showExifBench = true },
                        onOpenWriteDelayBench = { showWriteDelayBench = true },
                        onOpenNameBench = { showNameBench = true },
                        onOpenReadSlotsBench = { showReadSlotsBench = true },
                        onOpenDrPriorityBench = { showDrPriorityBench = true },
                        onOpenHapticBench = { showHapticBench = true },
                        onOpenPtpLog = { showPtpLog = true },
                        onAddMockCamera = onAddMockCamera,
                        onShowScanLog = onLoadCaptureLog,
                        onSetPropertyWriteDelay = onSetPropertyWriteDelay,
                        update = state.update,
                        onCheckForUpdates = onCheckForUpdates,
                        onInstallUpdate = onInstallUpdate,
                        backup = state.backup,
                        onExportBackup = onExportBackup,
                        onImportBackupMerge = onImportBackupMerge,
                        onImportBackupReplace = onImportBackupReplace,
                        onDismissBackupMessage = onDismissBackupMessage,
                        onShutterCheck = onShutterCheck,
                    )
                }

                AppOverlays(
                    state = state,
                    cameraLabel = cameraLabel,
                    cameraDetail = cameraDetail,
                    showExifBench = showExifBench,
                    showWriteDelayBench = showWriteDelayBench,
                    showNameBench = showNameBench,
                    showReadSlotsBench = showReadSlotsBench,
                    showDrPriorityBench = showDrPriorityBench,
                    showHapticBench = showHapticBench,
                    showPtpLog = showPtpLog,
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
                    onNameBenchClose = { showNameBench = false },
                    onReadSlotsBenchClose = { showReadSlotsBench = false },
                    onDrPriorityBenchClose = { showDrPriorityBench = false },
                    onHapticBenchClose = { showHapticBench = false },
                    onPtpLogClose = { showPtpLog = false },
                    ptpLogText = rearrangeDebugLog,
                    onImportFromPhotoGuideClose = { showImportFromPhotoGuide = false },
                    showImportFromScreenshotGuide = showImportFromScreenshotGuide,
                    onImportFromScreenshotGuideClose = { showImportFromScreenshotGuide = false },
                    onImportFromScreenshot = onImportFromScreenshot,
                    showScanTileGuide = showScanTileGuide,
                    onScanTileGuideClose = { showScanTileGuide = false },
                    onExifImportErrorDismiss = onExifImportErrorDismiss,
                    onExifImportRetry = onImportFromPhoto,
                    onOcrImportErrorDismiss = onOcrImportErrorDismiss,
                    onOcrImportRetry = onImportFromScreenshot,
                    onImportQrRetry = { showQrScanner = true },
                    onQrImportErrorDismiss = onQrImportErrorDismiss,
                    showQrScanner = showQrScanner,
                    onQrScannerClose = { showQrScanner = false },
                    onQrRecipeDetected = {
                        showQrScanner = false
                        onQrRecipeDetected(it)
                    },
                    onQrScannerOpenImage = {
                        showQrScanner = false
                        onImportQrFromImage()
                    },
                    onShutterCheckDismiss = onShutterCheckDismiss,
                    onShutterCheckRetry = onShutterCheck,
                )
            }

            val isCharging = rememberIsPhoneCharging()
            var chargingBannerDismissed by remember { mutableStateOf(false) }
            LaunchedEffect(isCharging) {
                if (!isCharging) chargingBannerDismissed = false
            }
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

        state.captureLog?.let { log ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color(0xCC000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClearCaptureLog,
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(androidx.compose.ui.graphics.Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        )
                ) {
                    Text("SCAN DIAGNOSTIC LOG",
                        color = androidx.compose.ui.graphics.Color(0xFFC99A4E),
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                        letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Text(log,
                        color = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
                        fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                        fontFamily = com.paeki.fujirecipes.ui.theme.MonoFamily,
                        lineHeight = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("TAP OUTSIDE TO DISMISS · CLEARS LOG",
                        color = androidx.compose.ui.graphics.Color(0xFF555555),
                        fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                        letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
                    )
                }
            }
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
    showNameBench: Boolean,
    showReadSlotsBench: Boolean,
    showDrPriorityBench: Boolean,
    showHapticBench: Boolean,
    showPtpLog: Boolean,
    ptpLogText: String,
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
    onNameBenchClose: () -> Unit,
    onReadSlotsBenchClose: () -> Unit,
    onDrPriorityBenchClose: () -> Unit,
    onHapticBenchClose: () -> Unit,
    onPtpLogClose: () -> Unit,
    onImportFromPhotoGuideClose: () -> Unit,
    showImportFromScreenshotGuide: Boolean,
    onImportFromScreenshotGuideClose: () -> Unit,
    onImportFromScreenshot: () -> Unit,
    showScanTileGuide: Boolean,
    onScanTileGuideClose: () -> Unit,
    onExifImportErrorDismiss: () -> Unit,
    onExifImportRetry: () -> Unit,
    onOcrImportErrorDismiss: () -> Unit,
    onOcrImportRetry: () -> Unit,
    onImportQrRetry: () -> Unit,
    onQrImportErrorDismiss: () -> Unit,
    showQrScanner: Boolean,
    onQrScannerClose: () -> Unit,
    onQrRecipeDetected: (RecipeUiModel) -> Unit,
    onQrScannerOpenImage: () -> Unit,
    onShutterCheckDismiss: () -> Unit,
    onShutterCheckRetry: () -> Unit,
) {
    val context = LocalContext.current
    val writeDelayBenchVm: WriteDelayBenchViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val nameBenchVm: NameBenchViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val readSlotsBenchVm: ReadSlotsBenchViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val drPriorityBenchVm: DrPriorityBenchViewModel = androidx.hilt.navigation.compose.hiltViewModel()

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
            interactionsEnabled = !(state.creatingRecipe || state.editorRecipe != null),
            showReferenceImageBlur = state.settings.showReferenceImageBlur,
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

    state.toastMessage?.let { msg ->
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(com.paeki.fujirecipes.ui.theme.PanelHigh)
                .border(1.dp, Gold, RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "✓", color = Gold, fontSize = 16.sp)
            Text(
                text = msg,
                fontFamily = com.paeki.fujirecipes.ui.theme.SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = com.paeki.fujirecipes.ui.theme.TextPrimary,
            )
        }
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

    if (showNameBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            NameBenchScreen(viewModel = nameBenchVm, onClose = onNameBenchClose)
        }
    }

    if (showReadSlotsBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            ReadSlotsBenchScreen(viewModel = readSlotsBenchVm, onClose = onReadSlotsBenchClose)
        }
    }

    if (showDrPriorityBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            DrPriorityBenchScreen(viewModel = drPriorityBenchVm, onClose = onDrPriorityBenchClose)
        }
    }

    if (showHapticBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            HapticBenchScreen(onClose = onHapticBenchClose)
        }
    }

    if (showPtpLog) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            PtpLogScreen(log = ptpLogText, onClose = onPtpLogClose)
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

    AnimatedVisibility(
        visible = showQrScanner,
        enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 8 },
        exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it / 8 },
    ) {
        QrScannerScreen(
            onClose = onQrScannerClose,
            onDetected = onQrRecipeDetected,
            onOpenImage = onQrScannerOpenImage,
        )
    }

    AnimatedVisibility(
        visible = showScanTileGuide,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
        exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
               slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            com.paeki.fujirecipes.ui.ScanTileGuide(onClose = onScanTileGuideClose)
        }
    }

    if (state.exifImportLoading) {
        ExifImportLoadingScreen()
    }

    if (state.camera.restoringSlots) {
        RestoreSetLoadingScreen(currentSlotIndex = state.camera.restoringSlotIndex)
    }

    if (state.camera.rearrangingSlots) {
        RearrangeSlotsLoadingScreen(
            currentSlotIndex = state.camera.rearrangingSlotIndex,
            writeIndex = state.camera.rearrangingWriteIndex,
            writeTotal = state.camera.rearrangingWriteTotal,
        )
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
        val rawText = state.ocrRawText
        ExifImportErrorScreen(
            message = error,
            onDismiss = onOcrImportErrorDismiss,
            onRetry = onOcrImportRetry,
            onShareRawDump = if (rawText != null) {
                {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "OCR dump")
                        putExtra(Intent.EXTRA_TEXT, formatOcrDump(rawText, state.ocrParseResult))
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            } else null,
        )
    }

    if (state.qrImportLoading) {
        ExifImportLoadingScreen(eyebrow = "READING QR", subtitle = "Importing shared recipe")
    }

    state.qrImportError?.let { error ->
        ExifImportErrorScreen(
            message = error,
            onDismiss = onQrImportErrorDismiss,
            onRetry = onImportQrRetry,
        )
    }

    if (state.shutterCheckLoading) {
        ExifImportLoadingScreen(eyebrow = "READING IMAGE", subtitle = "Checking shutter count")
    }

    state.shutterCheckError?.let { error ->
        ExifImportErrorScreen(
            title = "NO COUNT FOUND",
            message = error,
            onDismiss = onShutterCheckDismiss,
            onRetry = onShutterCheckRetry,
        )
    }

    state.shutterCount?.let { count ->
        ShutterCheckResultDialog(count = count, onDismiss = onShutterCheckDismiss)
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

    val ocrRawText = state.ocrRawText
    if (ocrRawText != null && (state.creatingRecipe || state.editorRecipe != null)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 80.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Text(
                text = "SHARE OCR DUMP",
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                color = Gold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(PanelLow.copy(alpha = 0.92f))
                    .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .clickable {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "OCR dump")
                            putExtra(Intent.EXTRA_TEXT, formatOcrDump(ocrRawText, state.ocrParseResult))
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
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
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    var editing by remember { mutableStateOf(false) }
    var draft by remember(name) { mutableStateOf(name) }
    val modalInteraction = remember { MutableInteractionSource() }

    fun dismissWithMotion() {
        if (!motionEnabled) { onClose(); return }
        scope.launch { visible = false; delay(220); onClose() }
    }

    fun commitRename() {
        val trimmed = draft.trim().ifBlank { "My Camera" }
        onRename(trimmed)
        draft = trimmed
        editing = false
    }

    LaunchedEffect(motionEnabled) { visible = true }
    androidx.activity.compose.BackHandler(enabled = true) { dismissWithMotion() }

    val overlayTransition = updateTransition(targetState = visible, label = "camera-detail-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 160 else 120, easing = FastOutSlowInEasing) },
        label = "camera-detail-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = 0.62f * overlayAlpha))
            .clickable(onClick = ::dismissWithMotion),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 2 },
            exit  = fadeOut(tween(110, easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
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
            // Drag zone — pill + header combined so dragging anywhere in this area dismisses
            var swipeDy by remember { mutableStateOf(0f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { swipeDy = 0f },
                            onDrag = { change, amount -> change.consume(); swipeDy += amount.y },
                            onDragEnd = { if (swipeDy > 60f) dismissWithMotion() else swipeDy = 0f },
                            onDragCancel = { swipeDy = 0f },
                        )
                    },
            ) {
                // Pill — visual only, gesture is on the parent Column
                Box(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(TextDim.copy(alpha = 0.55f)),
                    )
                }

                // Header — model ID + custom name + DONE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 16.dp, bottom = 20.dp),
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
                            .clickable(onClick = ::dismissWithMotion)
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
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
        } // AnimatedVisibility
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
    val context = LocalContext.current
    val view = LocalView.current
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
                        .clickable {
                            if (tab != t.id) {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                            }
                            onTabChange(t.id)
                        }
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
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(currentSlotIndex) {
        if (currentSlotIndex >= 0) FujiHaptics.performStepClick(context, view, step = currentSlotIndex, total = 7)
    }
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

// ── Rearrange slots loading ───────────────────────────────────────

@Composable
private fun RearrangeSlotsLoadingScreen(
    currentSlotIndex: Int,
    writeIndex: Int,
    writeTotal: Int,
) {
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(writeIndex) {
        if (writeIndex >= 0) FujiHaptics.performStepClick(context, view, writeIndex, writeTotal)
    }
    val transition = rememberInfiniteTransition(label = "rearrange-slots")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rearrange-pulse",
    )
    val activeIndex = currentSlotIndex.coerceIn(0, 6)
    val total = writeTotal.coerceAtLeast(1)
    val hasStarted = currentSlotIndex >= 0 && writeIndex >= 0
    val activeWrite = writeIndex.coerceIn(0, total - 1)
    val completedLabel = if (hasStarted) activeWrite + 1 else 0
    val progress = if (hasStarted) ((activeWrite + 1) / total.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg.copy(alpha = 0.74f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(298.dp)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(PanelLow)
                .border(
                    1.dp,
                    Gold.copy(alpha = 0.24f + 0.14f * pulse),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                )
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "REARRANGE RECIPES",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = TextPrimary,
                )
                Text(
                    text = if (hasStarted) "Writing C${activeIndex + 1} to camera" else "Preparing slot writes",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "Keep the USB cable connected until the slot swap finishes.",
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
                        .background(Gold.copy(alpha = 0.82f)),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(7) { index ->
                    val active = hasStarted && index == activeIndex
                    Box(
                        modifier = Modifier
                            .size(if (active) 40.dp else 34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) Gold.copy(alpha = 0.14f + 0.16f * pulse) else PanelHigh)
                            .border(
                                1.dp,
                                if (active) Gold.copy(alpha = 0.62f + 0.22f * pulse) else Border,
                                RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "C${index + 1}",
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
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
                    text = "WRITE PROGRESS",
                    fontFamily = MonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    color = TextDim,
                )
                Text(
                    text = "$completedLabel/$total",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Gold,
                )
            }
        }
    }
}

// ── Shutter check result ──────────────────────────────────────────

@Composable
private fun ShutterCheckResultDialog(count: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SHUTTER COUNT",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 2.4.sp,
                color = Gold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = java.text.NumberFormat.getIntegerInstance().format(count),
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                letterSpacing = (-1).sp,
                color = TextPrimary,
            )
            Text(
                text = "actuations",
                fontFamily = SansFamily,
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Fujifilm X-series mechanical shutters are typically rated for 150,000 actuations or more.",
                fontFamily = SansFamily,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = TextDim,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Gold.copy(alpha = 0.10f))
                    .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Done",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
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
                    val scanLineInset = inset + 8.dp.toPx()
                    val innerTop = scanLineInset
                    val innerBottom = size.height - scanLineInset
                    val innerLeft = scanLineInset
                    val innerRight = size.width - scanLineInset
                    val y = innerTop + (innerBottom - innerTop) * scanProgress
                    val trailH = (innerBottom - innerTop) * 0.4f

                    // Glow trail above scan line, clipped to the inset scan lane.
                    if (y > innerTop) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, gold.copy(alpha = 0.10f)),
                                startY = maxOf(innerTop, y - trailH),
                                endY = y,
                            ),
                            topLeft = Offset(innerLeft, maxOf(innerTop, y - trailH)),
                            size = Size(innerRight - innerLeft, minOf(trailH, y - innerTop)),
                        )
                    }

                    // Scan line — gradient fade at edges, inset from the frame.
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                gold.copy(alpha = 0.55f),
                                gold,
                                gold.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                            startX = innerLeft,
                            endX = innerRight,
                        ),
                        start = Offset(innerLeft, y),
                        end = Offset(innerRight, y),
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
    onShareRawDump: (() -> Unit)? = null,
    title: String = "NO RECIPE FOUND",
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
                    text = title,
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
                if (onShareRawDump != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onShareRawDump)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "SHARE OCR DUMP",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp,
                            color = Gold,
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

private fun formatOcrDump(
    rawText: String?,
    result: com.paeki.fujirecipes.data.ocr.OcrParseResult?,
): String = buildString {
    appendLine("=== PARSE RESULT ===")
    if (result == null) {
        appendLine("No recipe fields found (parse returned null)")
    } else {
        appendLine("Matched: ${result.matchedCount} field(s)")
        appendLine()
        appendLine("Film Simulation: ${result.sim}")
        appendLine()
        appendLine("Effects:")
        result.effects.forEach { (k, v) -> appendLine("  $k: $v") }
        appendLine()
        appendLine("Tone:")
        result.tone.forEach { (k, v) -> appendLine("  $k: $v") }
        appendLine()
        appendLine("White Balance:")
        result.wb.forEach { (k, v) -> appendLine("  $k: $v") }
        if (result.unmatchedFields.isNotEmpty()) {
            appendLine()
            appendLine("Detected but not parsed: ${result.unmatchedFields.joinToString(", ")}")
        }
    }
    appendLine()
    appendLine("=== RAW OCR TEXT ===")
    appendLine(rawText ?: "(none)")
}
