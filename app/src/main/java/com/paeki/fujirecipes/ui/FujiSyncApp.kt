package com.paeki.fujirecipes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paeki.fujirecipes.ui.camera.CameraCardUiModel
import com.paeki.fujirecipes.ui.camera.CameraConnected
import com.paeki.fujirecipes.ui.camera.ConnectGuide
import com.paeki.fujirecipes.ui.components.AppHeader
import com.paeki.fujirecipes.ui.discover.DiscoverScreen
import com.paeki.fujirecipes.ui.haptics.FujiHaptics
import com.paeki.fujirecipes.ui.library.LibraryScreen
import com.paeki.fujirecipes.ui.model.LibraryRecipeSource
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.overlay.BackHandler
import com.paeki.fujirecipes.ui.overlay.OverlayLayer
import com.paeki.fujirecipes.ui.overlay.overlayStackOf
import com.paeki.fujirecipes.ui.profile.ProfileScreen
import com.paeki.fujirecipes.ui.theme.Bg

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
    onSmartRefChoosePhoto: () -> Unit = {},
    onSmartRefConfirm: () -> Unit = {},
    onSmartRefDismiss: () -> Unit = {},
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
    var showSmartRefGuide by remember { mutableStateOf(false) }
    LaunchedEffect(state.editorRecipe) {
        if (showImportFromPhotoGuide && state.editorRecipe != null) {
            showImportFromPhotoGuide = false
        }
        if (showImportFromScreenshotGuide && state.editorRecipe != null) {
            showImportFromScreenshotGuide = false
        }
    }
    LaunchedEffect(state.smartRefResult, state.smartRefLoading) {
        if (showSmartRefGuide && (state.smartRefResult != null || state.smartRefLoading)) {
            showSmartRefGuide = false
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
        OverlayLayer(showSmartRefGuide) { showSmartRefGuide = false },
        OverlayLayer(state.smartRefLoading) { },
        OverlayLayer(state.smartRefError != null) { onSmartRefDismiss() },
        OverlayLayer(state.smartRefResult != null) { onSmartRefDismiss() },
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
                        onSmartRef = { showSmartRefGuide = true },
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
                    showSmartRefGuide = showSmartRefGuide,
                    onSmartRefGuideClose = { showSmartRefGuide = false },
                    onSmartRefChoosePhoto = onSmartRefChoosePhoto,
                    onSmartRefConfirm = onSmartRefConfirm,
                    onSmartRefDismiss = onSmartRefDismiss,
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
