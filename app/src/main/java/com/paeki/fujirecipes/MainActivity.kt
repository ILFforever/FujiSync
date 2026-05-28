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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.paeki.fujirecipes.data.usb.CameraUsbMode
import com.paeki.fujirecipes.data.usb.FujiPtpProbe
import com.paeki.fujirecipes.data.usb.FujiPtpProbeResult
import com.paeki.fujirecipes.data.usb.FujiRecipeCamera
import com.paeki.fujirecipes.data.usb.UsbCameraRepository
import com.paeki.fujirecipes.data.usb.UsbCameraScanner
import com.paeki.fujirecipes.data.usb.UsbPtpConnection
import com.paeki.fujirecipes.domain.model.CameraSlot
import com.paeki.fujirecipes.ui.AppTab
import com.paeki.fujirecipes.ui.FujiSyncApp
import com.paeki.fujirecipes.ui.FujiSyncUiState
import com.paeki.fujirecipes.ui.WriteToastState
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.model.SampleData
import com.paeki.fujirecipes.ui.theme.FujiRecipesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var usbManager: UsbManager
    private lateinit var repository: UsbCameraRepository
    private lateinit var connectionFactory: UsbPtpConnection

    private var uiState by mutableStateOf(
        FujiSyncUiState(
            slots = SampleData.slots,
            library = SampleData.library,
        )
    )

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val device = intent.usbDeviceExtra()
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            if (granted && device != null) {
                probeDevice(device)
            } else {
                uiState = uiState.copy(writeBusy = false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        repository = UsbCameraRepository(UsbCameraScanner(usbManager))
        connectionFactory = UsbPtpConnection(usbManager)

        registerUsbPermissionReceiver()
        refreshDevices()

        setContent {
            FujiRecipesTheme {
                FujiSyncApp(
                    state = uiState,
                    onReconnect = ::onReconnect,
                    onTabChange = { tab -> uiState = uiState.copy(tab = tab) },
                    onSelectSlot = { idx -> uiState = uiState.copy(selectedSlotIdx = idx) },
                    onOpenDetail = { recipe -> uiState = uiState.copy(detailRecipe = recipe) },
                    onCloseDetail = { uiState = uiState.copy(detailRecipe = null) },
                    onWrite = ::handleWrite,
                    onToggleLibrarySort = {
                        uiState = uiState.copy(
                            librarySort = if (uiState.librarySort == "NEWEST") "NAME" else "NEWEST"
                        )
                    },
                    onOpenLibraryItem = { recipe ->
                        uiState = uiState.copy(
                            detailRecipe = RecipeUiModel(
                                slot = "",
                                name = recipe.name,
                                sim = recipe.sim,
                                pills = recipe.pills,
                            )
                        )
                    },
                )
            }
        }
    }

    private fun onReconnect() {
        // If already connected via USB, toggle back to disconnected for demo.
        // In real use this triggers a USB scan.
        if (uiState.connected) {
            uiState = uiState.copy(connected = false)
        } else {
            val devices = repository.scanUsb()
            val ptpDevice = devices.firstOrNull { it.mode == CameraUsbMode.Ptp }
            if (ptpDevice != null) {
                // Real camera found — probe it
                if (!usbManager.hasPermission(ptpDevice.device)) {
                    requestUsbPermission(ptpDevice.device)
                    return
                }
                probeDevice(ptpDevice.device)
            } else {
                // No camera — simulate connection with sample data (prototype/demo mode)
                val model = devices.firstOrNull()?.productName?.takeIf { it.isNotBlank() } ?: "X-H2"
                uiState = uiState.copy(
                    connected = true,
                    cameraModel = model,
                    slots = SampleData.slots,
                )
            }
        }
    }

    private fun handleWrite() {
        val recipe = uiState.detailRecipe ?: uiState.slots.getOrNull(uiState.selectedSlotIdx) ?: return
        val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }

        uiState = uiState.copy(writeBusy = true)

        if (selected == null || !uiState.connected) {
            // Demo mode — simulate write
            lifecycleScope.launch {
                delay(1400)
                val slot = recipe.slot.ifEmpty { "C1" }
                uiState = uiState.copy(
                    writeBusy = false,
                    detailRecipe = null,
                    writeToast = WriteToastState(slot = slot, name = recipe.name),
                )
                delay(2400)
                uiState = uiState.copy(writeToast = null)
            }
            return
        }

        if (!usbManager.hasPermission(selected.device)) {
            requestUsbPermission(selected.device)
            return
        }

        lifecycleScope.launch {
            // Real write would go here via FujiRecipeCamera
            delay(1400)
            val slot = recipe.slot.ifEmpty { "C1" }
            uiState = uiState.copy(
                writeBusy = false,
                detailRecipe = null,
                writeToast = WriteToastState(slot = slot, name = recipe.name),
            )
            delay(2400)
            uiState = uiState.copy(writeToast = null)
        }
    }

    private fun probeDevice(device: UsbDevice) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { FujiPtpProbe(connectionFactory).probe(device) }
                    .getOrElse { FujiPtpProbeResult.NotReady(it.message ?: "Probe failed.") }
            }

            when (result) {
                is FujiPtpProbeResult.Ready -> {
                    val model = result.deviceInfo.model.ifBlank { "Fujifilm" }
                    val battery = result.batteryPercent?.let { "$it%" } ?: "—"
                    val firmware = result.deviceInfo.deviceVersion.ifBlank { "—" }
                    uiState = uiState.copy(
                        connected = true,
                        cameraModel = model,
                        battery = battery,
                        firmware = firmware,
                        slots = SampleData.slots, // real slots filled in by readAllSlots()
                    )
                    readAllSlots(device)
                }
                is FujiPtpProbeResult.NotReady -> {
                    uiState = uiState.copy(connected = false)
                }
            }
        }
    }

    private fun readAllSlots(device: UsbDevice) {
        lifecycleScope.launch {
            val updatedSlots = uiState.slots.toMutableList()
            for (slot in CameraSlot.entries) {
                val preset = withContext(Dispatchers.IO) {
                    runCatching {
                        connectionFactory.open(device)?.use { connection ->
                            check(connection.openSession()) { "OpenSession rejected." }
                            try {
                                FujiRecipeCamera(connection).readPreset(slot)
                            } finally {
                                connection.closeSession()
                            }
                        }
                    }.getOrNull()
                }
                if (preset != null) {
                    val idx = slot.ordinal
                    updatedSlots[idx] = updatedSlots[idx].copy(
                        slot = slot.label,
                        name = preset.name.ifBlank { updatedSlots[idx].name },
                    )
                }
            }
            uiState = uiState.copy(slots = updatedSlots)
        }
    }

    private fun refreshDevices() {
        val devices = repository.scanUsb()
        val hasPtp = devices.any { it.mode == CameraUsbMode.Ptp }
        if (!hasPtp) {
            uiState = uiState.copy(connected = false)
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
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
        refreshDevices()
    }

    private companion object {
        const val ACTION_USB_PERMISSION = "com.paeki.fujirecipes.USB_PERMISSION"
    }
}

private fun Intent.usbDeviceExtra(): UsbDevice? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(UsbManager.EXTRA_DEVICE)
    }
