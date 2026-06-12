package com.paeki.fujirecipes

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
import com.paeki.fujirecipes.capture.CaptureDiag
import com.paeki.fujirecipes.capture.OcrCaptureService
import com.paeki.fujirecipes.ui.FujiSyncApp
import com.paeki.fujirecipes.ui.BackupImportMode
import com.paeki.fujirecipes.ui.MainViewModel
import com.paeki.fujirecipes.ui.MainViewModelEvent
import com.paeki.fujirecipes.ui.SplashScreen
import com.paeki.fujirecipes.ui.theme.FujiRecipesTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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
        viewModel.handleExifImportResult(uri)
    }

    private val shutterCheckPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.handleShutterCheckResult(uri)
    }

    private val ocrImagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.handleOcrImportResult(uri)
    }

    private val qrImagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.handleQrImportResult(uri)
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
            viewModel.onUsbPermissionResult(device, granted)
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
                if (showSplash) {
                    SplashScreen(onComplete = { showSplash = false })
                } else {
                    FujiSyncApp(
                    state = state,
                    onReconnect = viewModel::onReconnect,
                    onTabChange = viewModel::setTab,
                    onSelectSlot = viewModel::setSelectedSlotIdx,
                    onOpenDetail = viewModel::openDetail,
                    onCloseDetail = viewModel::closeDetail,
                    onOpenRecipeCreator = viewModel::openRecipeCreator,
                    onOpenRecipeEditor = viewModel::openRecipeEditor,
                    onCloseRecipeEditor = viewModel::closeRecipeEditor,
                    onSaveRecipeDraft = viewModel::saveRecipeDraft,
                    onWrite = viewModel::handleWrite,
                    onSaveToLibrary = viewModel::handleSaveToLibrary,
                    onAddReferenceImage = viewModel::handleAddReferenceImage,
                    onRemoveReferenceImage = viewModel::handleRemoveReferenceImage,
                    onAddEditorReferenceImage = viewModel::handleAddEditorReferenceImage,
                    onRemoveEditorReferenceImage = viewModel::handleRemoveEditorReferenceImage,
                    onDeleteLibraryRecipes = viewModel::handleDeleteLibraryRecipes,
                    onCloneLibraryRecipe = viewModel::handleCloneLibraryRecipe,
                    onAddLibraryGroupImage = viewModel::handleAddLibraryGroupImage,
                    onOpenLibraryItem = viewModel::openLibraryItem,
                    onOpenCameraImageTuner = { viewModel.setShowCameraImageTuner(true) },
                    onCloseCameraImageTuner = { viewModel.setShowCameraImageTuner(false) },
                    onLoadSampleLibrary = viewModel::handleLoadSampleLibrary,
                    onDuplicateSaveAsNew = viewModel::handleDuplicateSaveAsNew,
                    onDuplicateUpdateExisting = viewModel::handleDuplicateUpdateExisting,
                    onDuplicateDismiss = viewModel::handleDuplicateDismiss,
                    onExploreDemo = viewModel::handleExploreDemo,
                    onBackupSlots = { label -> viewModel.handleBackupSlots(label) },
                    onRestoreSlots = viewModel::handleRestoreSlots,
                    onDeleteSlotBackup = viewModel::handleDeleteSlotBackup,
                    onRenameSlotBackup = viewModel::handleRenameSlotBackup,
                    onSelectSlotBackup = viewModel::handleSelectSlotBackup,
                    onRearrangeCameraSlots = viewModel::handleRearrangeCameraSlots,
                    onRearrangeValidationDismiss = viewModel::dismissRearrangeValidation,
                    rearrangeDebugLog = viewModel.rearrangeDebugLog.collectAsState().value,
                    onRenameCameraLabel = { serial, label -> viewModel.handleRenameCameraLabel(serial, label) },
                    onDeleteCamera = viewModel::handleDeleteCamera,
                    onResetCameraLabel = viewModel::handleResetCameraLabel,
                    onToggleFavorite = viewModel::handleToggleFavoriteById,
                    onToggleLibraryShowImages = viewModel::handleToggleLibraryShowImages,
                    onToggleReferenceImageBlur = viewModel::handleToggleReferenceImageBlur,
                    onToggleFavoritesOnTop = viewModel::handleToggleFavoritesOnTop,
                    onToggleHaptics = viewModel::handleToggleHaptics,
                    onWriteLibraryRecipeToSlot = viewModel::handleWriteToSlot,
                    onImportFromPhoto = viewModel::handleLaunchExifImport,
                    onExifImportErrorDismiss = viewModel::handleExifImportDismiss,
                    onImportFromScreenshot = viewModel::handleLaunchOcrImport,
                    onOcrImportErrorDismiss = viewModel::handleOcrImportDismiss,
                    onQrRecipeDetected = viewModel::handleQrImportRecipe,
                    onImportQrFromImage = viewModel::handleLaunchQrImageImport,
                    onQrImportErrorDismiss = viewModel::handleQrImportDismiss,
                    onAddMockCamera = viewModel::handleAddMockCamera,
                    onSaveAllToLibrary = viewModel::handleSaveAllSlotsToLibrary,
                    onSaveAllReportDismiss = viewModel::handleSaveAllReportDismiss,
                    onLoadCaptureLog = viewModel::loadCaptureLog,
                    onClearCaptureLog = viewModel::clearCaptureLog,
                    onSetPropertyWriteDelay = viewModel::handleSetPropertyWriteDelay,
                    onCheckForUpdates = viewModel::handleCheckForUpdates,
                    onInstallUpdate = viewModel::handleInstallUpdate,
                    onExportBackup = viewModel::handleLaunchBackupExport,
                    onImportBackupMerge = { viewModel.handleLaunchBackupImport(BackupImportMode.Merge) },
                    onImportBackupReplace = { viewModel.handleLaunchBackupImport(BackupImportMode.Replace) },
                    onDismissBackupMessage = viewModel::handleBackupMessageDismiss,
                    onShutterCheck = viewModel::handleLaunchShutterCheck,
                    onShutterCheckDismiss = viewModel::handleShutterCheckDismiss,
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
                    MainViewModelEvent.LaunchReferenceImagePicker -> referenceImagePicker.launch(arrayOf("image/*"))
                    MainViewModelEvent.LaunchGroupImagePicker -> groupImagePicker.launch(arrayOf("image/*"))
                    MainViewModelEvent.LaunchExifImagePicker -> exifImagePicker.launch(arrayOf("image/*"))
                    MainViewModelEvent.LaunchOcrImagePicker  -> ocrImagePicker.launch(arrayOf("image/*"))
                    MainViewModelEvent.LaunchQrImagePicker -> qrImagePicker.launch(arrayOf("image/*"))
                    is MainViewModelEvent.InstallApk -> openApkInstaller(event.uri)
                    MainViewModelEvent.OpenInstallPermissionSettings -> openInstallPermissionSettings()
                    is MainViewModelEvent.LaunchBackupExport -> backupExportPicker.launch(event.fileName)
                    MainViewModelEvent.LaunchBackupImport -> backupImportPicker.launch(arrayOf("application/json", "text/*", "*/*"))
                    MainViewModelEvent.LaunchShutterCheckPicker -> shutterCheckPicker.launch(arrayOf("image/*"))
                }
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
            else -> if (!handleUsbAttachIntent(intent)) viewModel.refreshDevices()
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
        viewModel.handleOcrImportResult(Uri.parse(uriString))
    }

    private fun handleUsbAttachIntent(intent: Intent?): Boolean {
        if (intent?.action != ACTION_USB_DEVICE_ATTACHED) return false
        val device = intent.usbDeviceExtra()
        if (device?.vendorId != FUJI_VENDOR_ID) {
            viewModel.refreshDevices()
            return true
        }
        viewModel.onReconnect()
        return true
    }

    private companion object {
        const val ACTION_USB_PERMISSION = "com.paeki.fujirecipes.USB_PERMISSION"
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
