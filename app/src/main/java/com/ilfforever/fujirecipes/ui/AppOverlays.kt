package com.ilfforever.fujirecipes.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujirecipes.ui.camera.CameraCardUiModel
import com.ilfforever.fujirecipes.ui.camera.CameraImageTunerScreen
import com.ilfforever.fujirecipes.ui.components.DeleteConfirmDialog
import com.ilfforever.fujirecipes.ui.components.DuplicateDialog
import com.ilfforever.fujirecipes.ui.detail.RecipeDetailScreen
import com.ilfforever.fujirecipes.ui.dev.DrPriorityBenchScreen
import com.ilfforever.fujirecipes.ui.dev.DrPriorityBenchViewModel
import com.ilfforever.fujirecipes.ui.dev.ExifBenchScreen
import com.ilfforever.fujirecipes.ui.dev.FxwSearchBenchScreen
import com.ilfforever.fujirecipes.ui.dev.HapticBenchScreen
import com.ilfforever.fujirecipes.ui.dev.NameBenchScreen
import com.ilfforever.fujirecipes.ui.dev.NameBenchViewModel
import com.ilfforever.fujirecipes.ui.dev.PtpLogScreen
import com.ilfforever.fujirecipes.ui.dev.ReadSlotsBenchScreen
import com.ilfforever.fujirecipes.ui.dev.ReadSlotsBenchViewModel
import com.ilfforever.fujirecipes.ui.dev.WriteDelayBenchScreen
import com.ilfforever.fujirecipes.ui.dev.WriteDelayBenchViewModel
import com.ilfforever.fujirecipes.ui.editor.RecipeEditorScreen
import com.ilfforever.fujirecipes.ui.model.RecipeUiModel
import com.ilfforever.fujirecipes.ui.qr.QrScannerScreen
import com.ilfforever.fujirecipes.ui.theme.Bg
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelLow

@Composable
internal fun BoxScope.AppOverlays(
    state: FujiSyncUiState,
    cameraLabel: String,
    cameraDetail: Pair<Int, CameraCardUiModel>?,
    showExifBench: Boolean,
    showFxwSearchBench: Boolean,
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
    onReorderReferenceImages: (RecipeUiModel, Int, Int) -> Unit,
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
    onFxwSearchBenchClose: () -> Unit,
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
    showSmartRefGuide: Boolean,
    onSmartRefGuideClose: () -> Unit,
    onSmartRefChoosePhoto: () -> Unit,
    onSmartRefConfirm: () -> Unit,
    onSmartRefDismiss: () -> Unit,
    onSmartRefConfirmAndContinue: () -> Unit,
    onSmartRefDismissAndContinue: () -> Unit,
    onSmartRefCreateNew: () -> Unit,
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
            onReorderReferenceImages = onReorderReferenceImages,
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
            maxReferenceImages = state.settings.maxReferenceImages,
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
                .background(com.ilfforever.fujirecipes.ui.theme.PanelHigh)
                .border(1.dp, Gold, RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "✓", color = Gold, fontSize = 16.sp)
            Text(
                text = msg,
                fontFamily = com.ilfforever.fujirecipes.ui.theme.SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = com.ilfforever.fujirecipes.ui.theme.TextPrimary,
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

    if (showFxwSearchBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            FxwSearchBenchScreen(onClose = onFxwSearchBenchClose)
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
            com.ilfforever.fujirecipes.ui.ScanTileGuide(onClose = onScanTileGuideClose)
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
        visible = showSmartRefGuide,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
        exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
               slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            SmartReferenceGuide(
                onClose = onSmartRefGuideClose,
                onChoosePhoto = onSmartRefChoosePhoto,
            )
        }
    }

    if (state.smartRefLoading) {
        ExifImportLoadingScreen(eyebrow = "READING PHOTO", subtitle = "Matching to your library")
    }

    state.smartRefError?.let { error ->
        ExifImportErrorScreen(
            title = "NO MATCH FOUND",
            message = error,
            onDismiss = onSmartRefDismiss,
            onRetry = onSmartRefChoosePhoto,
            onCreateNew = if (state.smartRefPendingUri != null) onSmartRefCreateNew else null,
        )
    }

    state.smartRefResult?.let { result ->
        SmartRefResultSheet(
            result = result,
            onConfirm = onSmartRefConfirm,
            onDismiss = onSmartRefDismiss,
            onScanAnother = if (result.isAlreadyRef) onSmartRefDismissAndContinue else onSmartRefConfirmAndContinue,
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
            maxReferenceImages = state.settings.maxReferenceImages,
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
            com.ilfforever.fujirecipes.ui.profile.SlotReadingAnimationMockup(
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
