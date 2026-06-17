package com.ilfforever.fujisync

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.ilfforever.fujisync.capture.CaptureDiag
import com.ilfforever.fujisync.capture.OcrCaptureService
import com.ilfforever.fujisync.ui.FujiSyncApp
import com.ilfforever.fujisync.ui.BackupImportMode
import com.ilfforever.fujisync.ui.MainViewModel
import com.ilfforever.fujisync.ui.MainViewModelEvent
import com.ilfforever.fujisync.ui.SplashScreen
import com.ilfforever.fujisync.ui.camera.CameraEvent
import com.ilfforever.fujisync.ui.camera.CameraViewModel
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.profile.ImportEvent
import com.ilfforever.fujisync.ui.profile.ImportViewModel
import com.ilfforever.fujisync.ui.theme.FujiRecipesTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val cameraVm: CameraViewModel by viewModels()
    private val importVm: ImportViewModel by viewModels()

    private val referenceImagePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        uris.forEach { uri ->
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        viewModel.applyReferenceImages(uris)
    }

    private val groupImagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        viewModel.applyGroupImage(uri)
    }

    private val exifImagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        importVm.handleExifImportResult(uri)
    }

    private val shutterCheckPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        importVm.handleShutterCheckResult(uri)
    }

    private val smartRefPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        importVm.handleSmartRefResult(uri)
    }

    private val ocrImagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        importVm.handleOcrImportResult(uri)
    }

    private val qrImagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        importVm.handleQrImportResult(uri)
    }

    private val backupExportPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        viewModel.handleBackupExportDestination(uri)
    }

    private val backupImportPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.handleBackupImportResult(uri)
    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val device = intent.usbDeviceExtra() ?: return
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            cameraVm.onUsbPermissionResult(device, granted)
        }
    }

    private val ocrResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != OcrCaptureService.ACTION_OCR_TILE_RESULT) return
            CaptureDiag.log(this@MainActivity, "ocr result broadcast received")
            handleCaptureIntent(intent)
        }
    }

    private var ocrResultReceiverRegistered = false
    private var lastHandledCaptureToken: String? = null
    private var showSplash by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        registerUsbPermissionReceiver()
        registerOcrResultReceiver()
        collectViewModelEvents()
        handleUsbAttachIntent(intent)
        handleCaptureIntent(intent)

        setContent {
            FujiRecipesTheme {
                val state by viewModel.uiState.collectAsState()
                val cameraState by cameraVm.state.collectAsState()
                val cameraWriteBusy by cameraVm.writeBusy.collectAsState()
                val cameraWriteToast by cameraVm.writeToast.collectAsState()
                val importState by importVm.state.collectAsState()
                // Compose combined state: camera and import state from their VMs
                val combinedState = state.copy(
                    camera = cameraState,
                    writeBusy = cameraWriteBusy || state.writeBusy,
                    writeToast = cameraWriteToast ?: state.writeToast,
                    exifImportLoading = importState.exifLoading,
                    exifImportError = importState.exifError,
                    shutterCount = importState.shutterCount,
                    shutterCheckLoading = importState.shutterCheckLoading,
                    shutterCheckError = importState.shutterCheckError,
                    ocrImportLoading = importState.ocrLoading,
                    ocrImportError = importState.ocrError,
                    ocrRawText = importState.ocrRawText,
                    ocrParseResult = importState.ocrParseResult,
                    qrImportLoading = importState.qrLoading,
                    qrImportError = importState.qrError,
                    smartRefLoading = importState.smartRefLoading,
                    smartRefError = importState.smartRefError,
                    smartRefPendingUri = importState.smartRefPendingUri,
                    smartRefPendingRecipe = importState.smartRefPendingRecipe,
                    smartRefResult = importState.smartRefResult,
                )
                if (showSplash) {
                    SplashScreen(onComplete = { showSplash = false })
                } else {
                    FujiSyncApp(
                    state = combinedState,
                    onReconnect = cameraVm::onReconnect,
                    onTabChange = viewModel::setTab,
                    onSelectSlot = cameraVm::setSelectedSlotIdx,
                    onOpenDetail = viewModel::openDetail,
                    onCloseDetail = viewModel::closeDetail,
                    onOpenRecipeCreator = viewModel::openRecipeCreator,
                    onOpenRecipeEditor = viewModel::openRecipeEditor,
                    onCloseRecipeEditor = viewModel::closeRecipeEditor,
                    onSaveRecipeDraft = viewModel::saveRecipeDraft,
                    onWrite = { cameraVm.handleWrite(combinedState.detailRecipe ?: combinedState.camera.slots.getOrNull(combinedState.camera.selectedSlotIdx) ?: return@FujiSyncApp) },
                    onSaveToLibrary = { source -> viewModel.handleSaveToLibrary(source, combinedState.detailRecipe ?: combinedState.camera.slots.getOrNull(combinedState.camera.selectedSlotIdx)) },
                    onAddReferenceImage = viewModel::handleAddReferenceImage,
                    onRemoveReferenceImage = viewModel::handleRemoveReferenceImage,
                    onReorderReferenceImages = viewModel::handleReorderReferenceImages,
                    onAddEditorReferenceImage = viewModel::handleAddEditorReferenceImage,
                    onRemoveEditorReferenceImage = viewModel::handleRemoveEditorReferenceImage,
                    onReorderEditorReferenceImages = viewModel::handleReorderEditorReferenceImages,
                    onDeleteLibraryRecipes = viewModel::handleDeleteLibraryRecipes,
                    onCloneLibraryRecipe = viewModel::handleCloneLibraryRecipe,
                    onAddLibraryGroupImage = viewModel::handleAddLibraryGroupImage,
                    onOpenLibraryItem = viewModel::openLibraryItem,
                    onOpenCameraImageTuner = { cameraVm.setShowImageTuner(true) },
                    onCloseCameraImageTuner = { cameraVm.setShowImageTuner(false) },
                    onLoadSampleLibrary = viewModel::handleLoadSampleLibrary,
                    onDuplicateSaveAsNew = viewModel::handleDuplicateSaveAsNew,
                    onDuplicateUpdateExisting = viewModel::handleDuplicateUpdateExisting,
                    onDuplicateDismiss = viewModel::handleDuplicateDismiss,
                    onExploreDemo = cameraVm::exploreDemo,
                    onBackupSlots = { label -> cameraVm.handleBackupSlots(label) },
                    onComposeSet = { label, slots -> cameraVm.handleComposeSet(label, slots) },
                    onRestoreSlots = cameraVm::handleRestoreSlots,
                    onDeleteSlotBackup = cameraVm::handleDeleteSlotBackup,
                    onRenameSlotBackup = cameraVm::handleRenameSlotBackup,
                    onSelectSlotBackup = cameraVm::handleSelectSlotBackup,
                    onRearrangeCameraSlots = cameraVm::handleRearrangeCameraSlots,
                    onRearrangeValidationDismiss = cameraVm::dismissRearrangeValidation,
                    rearrangeDebugLog = cameraVm.rearrangeDebugLog.collectAsState().value,
                    onRenameCameraLabel = { serial, label -> cameraVm.renameCameraLabel(serial, label) },
                    onDeleteCamera = cameraVm::deleteCamera,
                    onResetCameraLabel = cameraVm::resetCameraLabel,
                    onToggleFavorite = viewModel::handleToggleFavoriteById,
                    onToggleLibraryShowImages = viewModel::handleToggleLibraryShowImages,
                    onToggleCardImageCount = viewModel::handleToggleCardImageCount,
                    onToggleReferenceImageBlur = viewModel::handleToggleReferenceImageBlur,
                    onToggleFavoritesOnTop = viewModel::handleToggleFavoritesOnTop,
                    onToggleHaptics = viewModel::handleToggleHaptics,
                    onSetSmartRefSimilarityPct = viewModel::handleSetSmartRefSimilarityPct,
                    onSetMaxReferenceImages = viewModel::handleSetMaxReferenceImages,
                    onWriteLibraryRecipeToSlot = { slot -> cameraVm.handleWriteToSlot(combinedState.detailRecipe ?: return@FujiSyncApp, slot) },
                    onImportFromPhoto = importVm::launchExifImport,
                    onExifImportErrorDismiss = importVm::dismissExifImport,
                    onImportFromScreenshot = importVm::launchOcrImport,
                    onOcrImportErrorDismiss = importVm::dismissOcrImport,
                    onQrRecipeDetected = importVm::handleQrImportRecipe,
                    onImportQrFromImage = importVm::launchQrImageImport,
                    onQrImportErrorDismiss = importVm::dismissQrImport,
                    onAddMockCamera = cameraVm::addMockCamera,
                    onSaveAllToLibrary = { source -> viewModel.handleSaveAllSlotsToLibrary(source, combinedState.camera.slots) },
                    onSaveAllReportDismiss = viewModel::handleSaveAllReportDismiss,
                    onLoadCaptureLog = viewModel::loadCaptureLog,
                    onClearCaptureLog = viewModel::clearCaptureLog,
                    onSetPropertyWriteDelay = { ms -> viewModel.handleSetPropertyWriteDelay(ms); cameraVm.setWriteDelayMs(ms) },
                    onCheckForUpdates = viewModel::handleCheckForUpdates,
                    onInstallUpdate = viewModel::handleInstallUpdate,
                    onDismissUpdateDialog = viewModel::dismissUpdateDialog,
                    onExportBackup = viewModel::handleLaunchBackupExport,
                    onImportBackupMerge = { viewModel.handleLaunchBackupImport(BackupImportMode.Merge) },
                    onImportBackupReplace = { viewModel.handleLaunchBackupImport(BackupImportMode.Replace) },
                    onDismissBackupMessage = viewModel::handleBackupMessageDismiss,
                    onShutterCheck = importVm::launchShutterCheck,
                    onShutterCheckDismiss = importVm::dismissShutterCheck,
                    onSmartRefChoosePhoto = importVm::launchSmartRef,
                    onSmartRefConfirm = importVm::handleSmartRefConfirm,
                    onSmartRefDismiss = importVm::handleSmartRefDismiss,
                    onSmartRefConfirmAndContinue = importVm::handleSmartRefConfirmAndContinue,
                    onSmartRefDismissAndContinue = importVm::handleSmartRefDismissAndContinue,
                    onSmartRefCreateNew = importVm::handleSmartRefCreateNew,
                    )
                } // else
            }
        }
    }

    private fun collectViewModelEvents() {
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is MainViewModelEvent.RequestUsbPermission -> requestUsbPermission(event.device)
                    MainViewModelEvent.LaunchReferenceImagePicker -> referenceImagePicker.launch(arrayOf("*/*"))
                    MainViewModelEvent.LaunchGroupImagePicker -> groupImagePicker.launch(arrayOf("*/*"))
                    MainViewModelEvent.LaunchExifImagePicker -> exifImagePicker.launch(arrayOf("*/*"))
                    MainViewModelEvent.LaunchOcrImagePicker  -> ocrImagePicker.launch(arrayOf("*/*"))
                    MainViewModelEvent.LaunchQrImagePicker -> qrImagePicker.launch(arrayOf("*/*"))
                    is MainViewModelEvent.InstallApk -> openApkInstaller(event.uri)
                    MainViewModelEvent.OpenInstallPermissionSettings -> openInstallPermissionSettings()
                    is MainViewModelEvent.LaunchBackupExport -> backupExportPicker.launch(event.fileName)
                    MainViewModelEvent.LaunchBackupImport -> backupImportPicker.launch(arrayOf("application/json", "text/*", "*/*"))
                    MainViewModelEvent.LaunchShutterCheckPicker -> shutterCheckPicker.launch(arrayOf("*/*"))
                    MainViewModelEvent.LaunchSmartRefPicker -> smartRefPicker.launch(arrayOf("*/*"))
                    MainViewModelEvent.BackupImported -> cameraVm.reloadPersistedData()
                }
            }
        }
        lifecycleScope.launch {
            cameraVm.events.collect { event ->
                when (event) {
                    is CameraEvent.RequestUsbPermission -> requestUsbPermission(event.device)
                    CameraEvent.ScanFailed -> FujiHaptics.perform(
                        context = this@MainActivity,
                        effect = FujiHapticEffect.WarningPause,
                    )
                }
            }
        }
        lifecycleScope.launch {
            importVm.events.collect { event ->
                when (event) {
                    ImportEvent.LaunchExifImagePicker -> exifImagePicker.launch(arrayOf("image/*"))
                    ImportEvent.LaunchOcrImagePicker -> ocrImagePicker.launch(arrayOf("image/*"))
                    ImportEvent.LaunchQrImagePicker -> qrImagePicker.launch(arrayOf("image/*"))
                    ImportEvent.LaunchShutterCheckPicker -> shutterCheckPicker.launch(arrayOf("image/*"))
                    ImportEvent.LaunchSmartRefPicker -> smartRefPicker.launch(arrayOf("image/*"))
                }
            }
        }
        lifecycleScope.launch {
            importVm.importedRecipe.collect { imported ->
                viewModel.handleImportedRecipe(imported.recipe, imported.referenceImageUris)
            }
        }
    }

    private fun openApkInstaller(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        usbManager.requestPermission(device, pendingIntent)
    }

    private fun registerUsbPermissionReceiver() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(usbPermissionReceiver, filter)
        }
    }

    private fun registerOcrResultReceiver() {
        if (ocrResultReceiverRegistered) return
        val filter = IntentFilter(OcrCaptureService.ACTION_OCR_TILE_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ocrResultReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(ocrResultReceiver, filter)
        }
        ocrResultReceiverRegistered = true
    }

    private fun unregisterOcrResultReceiver() {
        if (!ocrResultReceiverRegistered) return
        unregisterReceiver(ocrResultReceiver)
        ocrResultReceiverRegistered = false
    }

    override fun onDestroy() {
        unregisterOcrResultReceiver()
        unregisterReceiver(usbPermissionReceiver)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        when (intent.action) {
            OcrCaptureService.ACTION_OCR_TILE_RESULT -> handleCaptureIntent(intent)
            else -> if (!handleUsbAttachIntent(intent)) cameraVm.refreshDevices()
        }
    }

    private fun handleCaptureIntent(intent: Intent?) {
        CaptureDiag.log(this, "handleCaptureIntent: action=${intent?.action} extras=${intent?.extras?.keySet()?.joinToString()}")
        if (intent?.action != OcrCaptureService.ACTION_OCR_TILE_RESULT) return
        val uriString = intent.getStringExtra(OcrCaptureService.EXTRA_CAPTURE_URI)
        CaptureDiag.log(this, "uri string: $uriString")
        if (uriString == null) {
            viewModel.loadCaptureLog()
            return
        }
        val token = intent.getStringExtra(OcrCaptureService.EXTRA_CAPTURE_TOKEN) ?: uriString
        if (token == lastHandledCaptureToken) {
            CaptureDiag.log(this, "capture token already handled: $token")
            return
        }
        lastHandledCaptureToken = token
        CaptureDiag.log(this, "calling handleOcrImportResult")
        importVm.handleOcrImportResult(Uri.parse(uriString))
    }

    private fun handleUsbAttachIntent(intent: Intent?): Boolean {
        if (intent?.action != ACTION_USB_DEVICE_ATTACHED) return false
        val device = intent.usbDeviceExtra()
        if (device?.vendorId != FUJI_VENDOR_ID) {
            cameraVm.refreshDevices()
            return true
        }
        cameraVm.onReconnect()
        return true
    }

    private companion object {
        const val ACTION_USB_PERMISSION = "com.ilfforever.fujisync.USB_PERMISSION"
        const val FUJI_VENDOR_ID = 0x04CB
    }
}

private fun Intent.usbDeviceExtra(): UsbDevice? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(UsbManager.EXTRA_DEVICE)
    }
