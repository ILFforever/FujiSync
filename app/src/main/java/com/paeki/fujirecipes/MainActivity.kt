package com.paeki.fujirecipes

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.paeki.fujirecipes.ui.FujiSyncApp
import com.paeki.fujirecipes.ui.MainViewModel
import com.paeki.fujirecipes.ui.MainViewModelEvent
import com.paeki.fujirecipes.ui.SplashScreen
import com.paeki.fujirecipes.ui.theme.FujiRecipesTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val referenceImagePicker = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        uris.forEach { uri ->
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        viewModel.applyReferenceImages(uris)
    }

    private val groupImagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        viewModel.applyGroupImage(uri)
    }

    private val exifImagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.handleExifImportResult(uri)
    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val device = intent.usbDeviceExtra() ?: return
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            viewModel.onUsbPermissionResult(device, granted)
        }
    }

    private var showSplash by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        registerUsbPermissionReceiver()
        collectViewModelEvents()
        handleUsbAttachIntent(intent)

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
                    onRenameCameraLabel = { serial, label -> viewModel.handleRenameCameraLabel(serial, label) },
                    onDeleteCamera = viewModel::handleDeleteCamera,
                    onResetCameraLabel = viewModel::handleResetCameraLabel,
                    onToggleFavorite = viewModel::handleToggleFavoriteById,
                    onToggleLibraryShowImages = viewModel::handleToggleLibraryShowImages,
                    onWriteLibraryRecipeToSlot = viewModel::handleWriteToSlot,
                    onImportFromPhoto = viewModel::handleLaunchExifImport,
                    onExifImportErrorDismiss = viewModel::handleExifImportDismiss,
                    onAddMockCamera = viewModel::handleAddMockCamera,
                    onSaveAllToLibrary = viewModel::handleSaveAllSlotsToLibrary,
                    onSaveAllReportDismiss = viewModel::handleSaveAllReportDismiss,
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
                    MainViewModelEvent.LaunchReferenceImagePicker -> referenceImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    MainViewModelEvent.LaunchGroupImagePicker -> groupImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    MainViewModelEvent.LaunchExifImagePicker -> exifImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.SingleMimeType("image/jpeg")))
                }
            }
        }
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

    override fun onDestroy() {
        unregisterReceiver(usbPermissionReceiver)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!handleUsbAttachIntent(intent)) {
            viewModel.refreshDevices()
        }
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
