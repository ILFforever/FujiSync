package com.paeki.fujirecipes.ui

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.paeki.fujirecipes.BuildConfig
import com.paeki.fujirecipes.data.exif.FujiExifReader
import com.paeki.fujirecipes.data.ocr.OcrRecipeParser
import com.paeki.fujirecipes.data.exif.toPreset
import com.paeki.fujirecipes.data.local.LocalStore
import com.paeki.fujirecipes.data.remote.FxwRecipe
import com.paeki.fujirecipes.data.update.AppUpdateRelease
import com.paeki.fujirecipes.data.update.GitHubReleaseUpdater
import com.paeki.fujirecipes.data.update.isRemoteVersionNewer
import com.paeki.fujirecipes.data.usb.CameraHeartbeat
import com.paeki.fujirecipes.data.usb.CameraUsbMode
import com.paeki.fujirecipes.data.usb.FujiPtpProbe
import com.paeki.fujirecipes.data.usb.FujiPtpProbeResult
import com.paeki.fujirecipes.data.usb.FujiRecipeCamera
import com.paeki.fujirecipes.data.usb.UsbPtpConnection
import com.paeki.fujirecipes.domain.model.CameraSlot
import com.paeki.fujirecipes.domain.repository.CameraRepository
import com.paeki.fujirecipes.ui.library.LibraryStateHolder
import com.paeki.fujirecipes.ui.model.AppSettings
import com.paeki.fujirecipes.ui.model.LibraryGroupUiModel
import com.paeki.fujirecipes.ui.model.LibraryRecipeSource
import com.paeki.fujirecipes.ui.model.SaveAllReport
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.model.SlotBackupMeta
import com.paeki.fujirecipes.ui.model.SlotBackupSet
import com.paeki.fujirecipes.ui.model.toPreset
import com.paeki.fujirecipes.ui.model.toUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

sealed class MainViewModelEvent {
    data class RequestUsbPermission(val device: UsbDevice) : MainViewModelEvent()
    object LaunchReferenceImagePicker : MainViewModelEvent()
    object LaunchGroupImagePicker : MainViewModelEvent()
    object LaunchExifImagePicker : MainViewModelEvent()
    object LaunchOcrImagePicker : MainViewModelEvent()
    object LaunchQrImagePicker : MainViewModelEvent()
    data class InstallApk(val uri: Uri) : MainViewModelEvent()
    object OpenInstallPermissionSettings : MainViewModelEvent()
    data class LaunchBackupExport(val fileName: String) : MainViewModelEvent()
    object LaunchBackupImport : MainViewModelEvent()
}

enum class BackupImportMode { Merge, Replace }

private data class ReferenceImageTarget(
    val libraryId: String?,
    val slot: String?,
    val name: String,
    val sim: String,
    val editorDraft: Boolean = false,
)

internal object UiTimings {
    const val WRITE_ANIMATION_MS = 1400L
    const val TOAST_DISMISS_MS = 2400L
    const val SAVE_CONFIRMATION_MS = 1600L
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val usbManager: UsbManager,
    private val repository: CameraRepository,
    private val connectionFactory: UsbPtpConnection,
    private val localStore: LocalStore,
    private val heartbeat: CameraHeartbeat,
    private val libraryHolder: LibraryStateHolder,
    private val releaseUpdater: GitHubReleaseUpdater,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FujiSyncUiState())
    val uiState: StateFlow<FujiSyncUiState> = _uiState.asStateFlow()

    // DEBUG: rearrange flow log
    private val _rearrangeDebugLog = MutableStateFlow("")
    val rearrangeDebugLog: StateFlow<String> = _rearrangeDebugLog.asStateFlow()
    private fun rdbg(msg: String) {
        val ts = System.currentTimeMillis() % 100000
        _rearrangeDebugLog.value += "[$ts] $msg\n"
    }

    private val _events = MutableSharedFlow<MainViewModelEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<MainViewModelEvent> = _events.asSharedFlow()
    private var latestRelease: AppUpdateRelease? = null
    private var downloadedUpdateUri: Uri? = null
    private var pendingBackupImportMode: BackupImportMode = BackupImportMode.Merge

    private var pendingReferenceTarget: ReferenceImageTarget? = null
    private var pendingGroupImageTarget: String? = null
    private var heartbeatJob: Job? = null

    init {
        refreshDevices()
        loadPersistedState()
        observeLibraryState()
    }

    private fun observeLibraryState() {
        viewModelScope.launch {
            libraryHolder.state.collect { libraryState ->
                _uiState.update { appState ->
                    val refreshedDetail = appState.detailRecipe?.refreshFromLibrary(libraryState.recipes, libraryState.groups)
                    appState.copy(library = libraryState, detailRecipe = refreshedDetail)
                }
            }
        }
    }

    // ── USB / connection ──────────────────────────────────────────────

    fun refreshDevices() {
        val devices = repository.scanUsb()
        if (devices.none { it.mode == CameraUsbMode.Ptp }) {
            stopHeartbeat()
            _uiState.update { it.copy(camera = it.camera.copy(connected = false)) }
        }
    }

    fun onReconnect() {
        val state = _uiState.value
        if (state.camera.connected) {
            val ptpDevice = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
            if (ptpDevice == null) {
                _uiState.update {
                    it.copy(
                        camera = it.camera.copy(
                            connected = false,
                            cameraModel = "",
                            firmware = "",
                            battery = "",
                            slots = emptyList(),
                            readingSlotIndex = -1,
                            scanError = "Camera not found. Reconnect the USB cable.",
                        ),
                    )
                }
                return
            }
            if (!usbManager.hasPermission(ptpDevice.device)) {
                _events.tryEmit(MainViewModelEvent.RequestUsbPermission(ptpDevice.device))
                return
            }
            _uiState.update {
                it.copy(camera = it.camera.copy(readingSlots = true, readingSlotIndex = -1, slots = emptyList(), scanError = null, isRearrangeValidation = false))
            }
            readAllSlots(ptpDevice.device)
            return
        }

        _uiState.update { it.copy(camera = it.camera.copy(scanning = true, scanError = null)) }
        viewModelScope.launch {
            val devices = withContext(Dispatchers.IO) { repository.scanUsb() }
            val ptpDevice = devices.firstOrNull { it.mode == CameraUsbMode.Ptp }
            if (ptpDevice == null) {
                val fujiMode = devices.firstOrNull()?.mode
                val message = when (fujiMode) {
                    CameraUsbMode.CardReader -> "Camera is connected in card reader mode. Set USB mode to USB RAW CONV. / Backup Restore, then reconnect."
                    CameraUsbMode.Other -> "Fujifilm camera found, but it is not exposing a PTP recipe interface. Set USB mode to USB RAW CONV. / Backup Restore."
                    CameraUsbMode.NotPlugged, null -> "No camera found. Make sure USB mode is set to USB RAW CONV."
                    CameraUsbMode.Ptp -> "No camera found. Make sure USB mode is set to USB RAW CONV."
                }
                _uiState.update { it.copy(camera = it.camera.copy(scanning = false, scanError = message)) }
                return@launch
            }
            if (!usbManager.hasPermission(ptpDevice.device)) {
                _uiState.update { it.copy(camera = it.camera.copy(scanning = false)) }
                _events.tryEmit(MainViewModelEvent.RequestUsbPermission(ptpDevice.device))
                return@launch
            }
            probeDevice(ptpDevice.device)
        }
    }

    fun onUsbPermissionResult(device: UsbDevice, granted: Boolean) {
        if (granted) {
            probeDevice(device)
        } else {
            _uiState.update { it.copy(camera = it.camera.copy(scanning = false, scanError = "USB permission denied."), writeBusy = false) }
        }
    }

    fun probeDevice(device: UsbDevice) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { FujiPtpProbe(connectionFactory).probe(device) }
                    .getOrElse { FujiPtpProbeResult.NotReady(reason = it.message ?: "Probe failed.") }
            }
            when (result) {
                is FujiPtpProbeResult.Ready -> {
                    val model = result.deviceInfo.model.ifBlank { "Fujifilm" }
                    val serial = result.bestSerial
                    val fw = result.deviceInfo.deviceVersion.ifBlank { "—" }
                    _uiState.update { state ->
                        val updatedLabels = if (serial.isNotBlank() && serial !in state.camera.cameraLabels) {
                            state.camera.cameraLabels + (serial to model)
                        } else {
                            state.camera.cameraLabels
                        }
                        val updatedModels = if (serial.isNotBlank()) {
                            state.camera.cameraModels + (serial to model)
                        } else {
                            state.camera.cameraModels
                        }
                        val updatedFirmwares = if (serial.isNotBlank() && fw != "—") {
                            state.camera.cameraFirmwares + (serial to fw)
                        } else {
                            state.camera.cameraFirmwares
                        }
                        state.copy(
                            camera = state.camera.copy(
                                connected = true,
                                scanning = false,
                                readingSlots = true,
                                cameraModel = model,
                                cameraSerial = serial,
                                battery = result.batteryPercent?.let { pct -> "$pct%" } ?: "—",
                                firmware = fw,
                                slots = emptyList(),
                                cameraLabels = updatedLabels,
                                cameraModels = updatedModels,
                                cameraFirmwares = updatedFirmwares,
                                isRearrangeValidation = false,
                            ),
                        )
                    }
                    if (serial.isNotBlank()) {
                        persistCameraLabels()
                        persistCameraModels()
                        persistCameraFirmwares()
                    }
                    startHeartbeat(device)
                    readAllSlots(device)
                }
                is FujiPtpProbeResult.NotReady -> {
                    _uiState.update { it.copy(camera = it.camera.copy(connected = false, scanning = false, scanError = result.reason)) }
                }
            }
        }
    }

    private fun readAllSlots(device: UsbDevice) {
        viewModelScope.launch {
            var failCount = 0

            heartbeat.usbMutex.withLock {
                val conn = withContext(Dispatchers.IO) {
                    runCatching {
                        val c = connectionFactory.open(device) ?: return@runCatching null
                        if (!c.openSession()) { c.close(); return@runCatching null }
                        c
                    }.getOrNull()
                }

                if (conn == null) {
                    _uiState.update {
                        it.copy(
                            camera = it.camera.copy(
                                readingSlots = false,
                                readingSlotIndex = -1,
                                scanError = "Could not open camera for reading.",
                            ),
                        )
                    }
                    return@withLock
                }

                conn.use {
                    for ((idx, slot) in CameraSlot.entries.withIndex()) {
                        _uiState.update { it.copy(camera = it.camera.copy(readingSlotIndex = idx)) }
                        val preset = withContext(Dispatchers.IO) {
                            runCatching { FujiRecipeCamera(conn).readPreset(slot) }.getOrNull()
                        }
                        val recipe = if (preset != null) {
                            preset.toUiModel()
                        } else {
                            failCount++
                            RecipeUiModel(slot = slot.label, name = "READ FAILED", sim = "—", pills = emptyList())
                        }
                        _uiState.update { it.copy(camera = it.camera.copy(slots = it.camera.slots + recipe)) }
                    }
                    withContext(Dispatchers.IO) { runCatching { conn.closeSession() } }
                }
            }

            _uiState.update {
                it.copy(
                    camera = it.camera.copy(
                        readingSlots = false,
                        readingSlotIndex = -1,
                        isRestoringValidation = false,
                        scanError = if (failCount > 0) "$failCount slot${if (failCount > 1) "s" else ""} could not be read." else null,
                    ),
                )
            }
        }
    }

    private fun startHeartbeat(device: UsbDevice) {
        stopHeartbeat()
        heartbeat.reset()
        heartbeatJob = viewModelScope.launch {
            launch { heartbeat.monitor(device) }
            launch {
                heartbeat.slots.collect { presets ->
                    if (presets.isEmpty()) return@collect
                    _uiState.update { state ->
                        state.copy(
                            camera = state.camera.copy(
                                slots = presets.map { it.toUiModel() },
                            ),
                        )
                    }
                }
            }
            heartbeat.alive.drop(1).collect { alive ->
                if (!alive && _uiState.value.camera.connected) {
                    _uiState.update {
                        it.copy(
                            camera = it.camera.copy(
                                connected = false,
                                scanError = "Camera disconnected. Reconnect the USB cable.",
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        heartbeat.reset()
    }

    // ── Write ─────────────────────────────────────────────────────────

    fun handleWrite() {
        val state = _uiState.value
        val recipe = state.detailRecipe ?: state.camera.slots.getOrNull(state.camera.selectedSlotIdx) ?: return
        val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }

        _uiState.update { it.copy(writeBusy = true) }

        if (selected == null || !state.camera.connected) {
            _uiState.update { it.copy(writeBusy = false) }
            return
        }

        if (!usbManager.hasPermission(selected.device)) {
            _events.tryEmit(MainViewModelEvent.RequestUsbPermission(selected.device))
            return
        }

        val targetSlotLabel = recipe.slot.ifEmpty { "C1" }
        val targetSlot = CameraSlot.entries.firstOrNull { it.label == targetSlotLabel } ?: CameraSlot.C1

        viewModelScope.launch {
            val writeResult = heartbeat.usbMutex.withLock {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val connection = connectionFactory.open(selected.device)
                            ?: error("Unable to open the camera's PTP USB interface.")
                        connection.use {
                            check(connection.openSession()) { "OpenSession rejected." }
                            try {
                                FujiRecipeCamera(connection, _uiState.value.settings.propertyWriteDelayMs).writePreset(recipe.toPreset(targetSlot))
                            } finally {
                                connection.closeSession()
                            }
                        }
                    }
                }
            }
            if (writeResult.isFailure) {
                _uiState.update {
                    it.copy(
                        writeBusy = false,
                        camera = it.camera.copy(scanError = writeResult.exceptionOrNull()?.message ?: "Write failed."),
                    )
                }
                return@launch
            }
            val updatedRecipe = recipe.copy(slot = targetSlot.label)
            _uiState.update {
                it.copy(
                    writeBusy    = false,
                    detailRecipe = null,
                    camera       = it.camera.copy(slots = it.camera.slots.replaceSlot(targetSlot, updatedRecipe)),
                    writeToast   = WriteToastState(slot = targetSlotLabel, name = recipe.name),
                )
            }
            delay(UiTimings.TOAST_DISMISS_MS)
            _uiState.update { it.copy(writeToast = null) }
        }
    }

    fun handleWriteToSlot(targetSlot: String) {
        val state = _uiState.value
        val recipe = state.detailRecipe ?: return
        val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }

        _uiState.update { it.copy(writeBusy = true) }

        if (selected == null || !state.camera.connected) {
            _uiState.update { it.copy(writeBusy = false) }
            return
        }

        if (!usbManager.hasPermission(selected.device)) {
            _events.tryEmit(MainViewModelEvent.RequestUsbPermission(selected.device))
            return
        }

        val slot = CameraSlot.entries.firstOrNull { it.label == targetSlot } ?: CameraSlot.C1

        viewModelScope.launch {
            val writeResult = heartbeat.usbMutex.withLock {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val connection = connectionFactory.open(selected.device)
                            ?: error("Unable to open the camera's PTP USB interface.")
                        connection.use {
                            check(connection.openSession()) { "OpenSession rejected." }
                            try {
                                FujiRecipeCamera(connection, _uiState.value.settings.propertyWriteDelayMs).writePreset(recipe.toPreset(slot))
                            } finally {
                                connection.closeSession()
                            }
                        }
                    }
                }
            }
            if (writeResult.isFailure) {
                _uiState.update {
                    it.copy(
                        writeBusy = false,
                        camera = it.camera.copy(scanError = writeResult.exceptionOrNull()?.message ?: "Write failed."),
                    )
                }
                return@launch
            }
            val updatedRecipe = recipe.copy(slot = slot.label, libraryId = null)
            _uiState.update {
                it.copy(
                    writeBusy    = false,
                    detailRecipe = null,
                    camera       = it.camera.copy(slots = it.camera.slots.replaceSlot(slot, updatedRecipe)),
                    writeToast   = WriteToastState(slot = targetSlot, name = recipe.name),
                )
            }
            delay(UiTimings.TOAST_DISMISS_MS)
            _uiState.update { it.copy(writeToast = null) }
        }
    }

    /** Clears the rearrange validation panel once the user dismisses it. */
    fun dismissRearrangeValidation() {
        _uiState.update { it.copy(camera = it.camera.copy(isRearrangeValidation = false)) }
    }

    fun handleRearrangeCameraSlots(nextSlots: List<RecipeUiModel>) {
        _rearrangeDebugLog.value = "" // reset on new rearrange
        rdbg("START writes=${nextSlots.size} slots")
        val state = _uiState.value
        val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
        val ordered = CameraSlot.entries.mapIndexedNotNull { index, slot ->
            nextSlots.getOrNull(index)?.copy(slot = slot.label, libraryId = null)
        }
        if (ordered.size != CameraSlot.entries.size) { rdbg("ABORT ordered.size=${ordered.size}"); return }
        val writes = ordered.mapIndexedNotNull { index, recipe ->
            val current = state.camera.slots.getOrNull(index)
            if (current != null && current.sameRecipeIgnoringSlot(recipe)) null else CameraSlot.entries[index] to recipe
        }
        if (writes.isEmpty()) { rdbg("ABORT no changes"); return }
        rdbg("writes needed: ${writes.size} slots: ${writes.map { it.first.label }}")

        if (selected == null || !state.camera.connected) {
            rdbg("ABORT no camera")
            _uiState.update {
                it.copy(camera = it.camera.copy(scanError = "Connect camera before rearranging recipes."))
            }
            return
        }

        if (!usbManager.hasPermission(selected.device)) {
            rdbg("ABORT no permission")
            _events.tryEmit(MainViewModelEvent.RequestUsbPermission(selected.device))
            return
        }

        val previouslySelected = state.camera.slots.getOrNull(state.camera.selectedSlotIdx)
        rdbg("SET writeBusy=true rearrangingSlots=true")
        _uiState.update {
            it.copy(
                writeBusy = true,
                camera = it.camera.copy(
                    scanError = null,
                    rearrangingSlots = true,
                    rearrangingSlotIndex = -1,
                    rearrangingWriteIndex = -1,
                    rearrangingWriteTotal = writes.size,
                ),
            )
        }

        viewModelScope.launch {
            rdbg("LAUNCH acquiring usbMutex")
            val writeResult = heartbeat.usbMutex.withLock {
                rdbg("LOCKED usbMutex")
                withContext(Dispatchers.IO) {
                    runCatching {
                        val connection = connectionFactory.open(selected.device)
                            ?: error("Unable to open the camera's PTP USB interface.")
                        connection.use {
                            check(connection.openSession()) { "OpenSession rejected." }
                            try {
                                val camera = FujiRecipeCamera(connection, _uiState.value.settings.propertyWriteDelayMs)
                                writes.mapIndexedNotNull { index, (slot, recipe) ->
                                    rdbg("WRITE ${slot.label} ($index/${writes.size})")
                                    _uiState.update {
                                        it.copy(
                                            camera = it.camera.copy(
                                                rearrangingSlotIndex = CameraSlot.entries.indexOf(slot),
                                                rearrangingWriteIndex = index,
                                            ),
                                        )
                                    }
                                    val result = camera.writePreset(recipe.toPreset(slot))
                                    if (result.isOk) null else slot.label
                                }
                            } finally {
                                connection.closeSession()
                            }
                        }
                    }
                }
            }
            rdbg("RELEASED usbMutex")

            writeResult.fold(
                onSuccess = { failed ->
                    val selectedIdx = previouslySelected
                        ?.let { selectedRecipe -> ordered.indexOfFirst { it.sameRecipeIgnoringSlot(selectedRecipe) } }
                        ?.takeIf { it >= 0 }
                        ?: _uiState.value.camera.selectedSlotIdx.coerceIn(0, ordered.lastIndex)
                    val errorMsg = if (failed.isNotEmpty()) {
                        rdbg("PARTIAL FAIL slots: $failed — proceeding to validation")
                        failed.joinToString(prefix = "Write errors on ", postfix = ". Validating what landed.")
                    } else {
                        rdbg("SUCCESS → SET writeBusy=false isRearrangeValidation=true readingSlots=true")
                        null
                    }
                    _uiState.update {
                        it.copy(
                            writeBusy = false,
                            camera = it.camera.copy(
                                slots = emptyList(),
                                selectedSlotIdx = selectedIdx,
                                scanError = errorMsg,
                                rearrangingSlots = false,
                                rearrangingSlotIndex = -1,
                                rearrangingWriteIndex = -1,
                                rearrangingWriteTotal = 0,
                                readingSlots = true,
                                readingSlotIndex = -1,
                                isRearrangeValidation = true,
                            ),
                        )
                    }
                    rdbg("calling readAllSlots")
                    readAllSlots(selected.device)
                    rdbg("readAllSlots returned")
                },
                onFailure = { error ->
                    rdbg("EXCEPTION: ${error.message}")
                    _uiState.update {
                        it.copy(
                            writeBusy = false,
                            camera = it.camera.copy(
                                scanError = error.message ?: "Rearrange failed.",
                                rearrangingSlots = false,
                                rearrangingSlotIndex = -1,
                                rearrangingWriteIndex = -1,
                                rearrangingWriteTotal = 0,
                            ),
                        )
                    }
                },
            )
        }
    }

    // ── Library save / duplicate (delegates to LibraryStateHolder) ────

    fun handleSaveToLibrary(source: LibraryRecipeSource) {
        val state = _uiState.value
        val recipe = state.detailRecipe ?: state.camera.slots.getOrNull(state.camera.selectedSlotIdx) ?: return
        libraryHolder.addRecipe(recipe, source)
    }

    fun handleSaveAllSlotsToLibrary(source: LibraryRecipeSource) {
        val slots = _uiState.value.camera.slots.filter { it.name != "READ FAILED" && it.sim.isNotBlank() }
        if (slots.isEmpty()) return
        val report = libraryHolder.addRecipesBatch(slots, source)
        if (report.hasSkipped) {
            _uiState.update { it.copy(saveAllSlotsConfirmed = true, saveAllReport = report) }
        } else {
            _uiState.update { it.copy(saveAllSlotsConfirmed = true, writeToast = WriteToastState(slot = "", name = "All ${report.saved} saved to library", savedToLibrary = true)) }
            viewModelScope.launch {
                delay(UiTimings.SAVE_CONFIRMATION_MS)
                _uiState.update { it.copy(saveAllSlotsConfirmed = false, writeToast = null) }
            }
        }
    }

    fun handleSaveAllReportDismiss() {
        _uiState.update { it.copy(saveAllReport = null, saveAllSlotsConfirmed = false) }
    }

    fun handleDuplicateSaveAsNew() {
        val editorWasOpen = _uiState.value.creatingRecipe || _uiState.value.editorRecipe != null
        val savedName = libraryHolder.state.value.duplicateDialog?.incomingRecipe?.name
        libraryHolder.handleDuplicateSaveAsNew()
        closeEditorAfterDuplicateSave(editorWasOpen, savedName)
    }

    fun handleDuplicateUpdateExisting(libraryId: String) {
        val editorWasOpen = _uiState.value.creatingRecipe || _uiState.value.editorRecipe != null
        val savedName = libraryHolder.state.value.duplicateDialog?.incomingRecipe?.name
        libraryHolder.handleDuplicateUpdateExisting(libraryId)
        closeEditorAfterDuplicateSave(editorWasOpen, savedName)
    }

    fun handleDuplicateDismiss() = libraryHolder.dismissDuplicate()

    private fun closeEditorAfterDuplicateSave(editorWasOpen: Boolean, savedName: String?) {
        if (!editorWasOpen) return
        _uiState.update {
            it.copy(
                tab = AppTab.Library,
                editorRecipe = null,
                creatingRecipe = false,
                editorReferenceImageUris = emptyList(),
                writeToast = WriteToastState(slot = "", name = savedName.orEmpty(), savedToLibrary = true),
            )
        }
        viewModelScope.launch {
            delay(UiTimings.SAVE_CONFIRMATION_MS)
            _uiState.update { it.copy(writeToast = null) }
        }
    }

    // ── EXIF import ───────────────────────────────────────────────────

    fun handleLaunchExifImport() {
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchExifImagePicker) }
    }

    fun handleExifImportResult(uri: android.net.Uri) {
        _uiState.update { it.copy(exifImportLoading = true, exifImportError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            val recipe = runCatching { FujiExifReader.readRecipe(appContext, uri) }
            delay(minLoadMs(started))
            when {
                recipe.isFailure -> _uiState.update {
                    it.copy(
                        exifImportLoading = false,
                        exifImportError = "Couldn't read this file. Make sure it's an unedited JPEG from a Fujifilm camera.",
                    )
                }
                recipe.getOrNull() == null -> _uiState.update {
                    it.copy(
                        exifImportLoading = false,
                        exifImportError = "No Fujifilm recipe found in this file. Use an original JPEG shot on an X-series camera.",
                    )
                }
                else -> {
                    val exif = recipe.getOrNull() ?: return@launch
                    val localImageUri = runCatching { localStore.copyReferenceImage(uri) }.getOrNull()
                    val imageName = imageDisplayName(uri)
                    val uiModel = exif.toPreset("New Recipe").toUiModel().copy(
                        slot = "",
                        description = "Scanned from $imageName",
                        referenceImageUris = listOfNotNull(localImageUri),
                    )
                    _uiState.update {
                        it.copy(
                            exifImportLoading = false,
                            exifImportError = null,
                            creatingRecipe = false,
                            editorRecipe = uiModel,
                            editorReferenceImageUris = uiModel.referenceImageUris,
                        )
                    }
                }
            }
        }
    }

    fun handleExifImportDismiss() {
        _uiState.update { it.copy(exifImportLoading = false, exifImportError = null) }
    }

    // ── OCR / screenshot import ───────────────────────────────────────

    fun handleLaunchOcrImport() {
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchOcrImagePicker) }
    }

    fun handleOcrImportResult(uri: android.net.Uri) {
        _uiState.update { it.copy(ocrImportLoading = true, ocrImportError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            var capturedRaw: String? = null
            val result = runCatching {
                val input = OcrRecipeParser.recognizeText(appContext, uri)
                capturedRaw = input.mlText.text
                OcrRecipeParser.parse(input)
            }
            delay(minLoadMs(started))
            when {
                result.isFailure -> _uiState.update {
                    it.copy(
                        ocrImportLoading = false,
                        ocrImportError = "Couldn't read this image. Make sure it's a clear screenshot.",
                        ocrRawText = capturedRaw,
                        ocrParseResult = null,
                    )
                }
                result.getOrNull() == null -> _uiState.update {
                    it.copy(
                        ocrImportLoading = false,
                        ocrImportError = "No recipe settings found in this screenshot. Try a clearer image with visible parameter labels.",
                        ocrRawText = capturedRaw,
                        ocrParseResult = null,
                    )
                }
                else -> {
                    val parsed = result.getOrNull()!!
                    val imageName = imageDisplayName(uri)
                    val description = buildString {
                        append("Scanned from $imageName")
                        if (parsed.unmatchedFields.isNotEmpty()) {
                            append("\nCouldn't read: ${parsed.unmatchedFields.joinToString(", ")} — please verify.")
                        }
                    }
                    val name = parsed.suggestedName.ifBlank { "OCR Import" }
                    // Round-trip through toPreset → toUiModel to compute correct pills.
                    val preliminary = RecipeUiModel(
                        slot    = "C1",
                        name    = name,
                        sim     = parsed.sim,
                        pills   = emptyList(),
                        effects = parsed.effects,
                        tone    = parsed.tone,
                        wb      = parsed.wb,
                    )
                    val uiModel = preliminary
                        .toPreset(com.paeki.fujirecipes.domain.model.CameraSlot.C1)
                        .toUiModel()
                        .copy(slot = "", name = name, description = description)
                    _uiState.update {
                        it.copy(
                            ocrImportLoading  = false,
                            ocrImportError    = null,
                            ocrRawText        = capturedRaw,
                            ocrParseResult    = parsed,
                            creatingRecipe    = false,
                            editorRecipe      = uiModel,
                            editorReferenceImageUris = emptyList(),
                        )
                    }
                }
            }
        }
    }

    fun handleOcrImportDismiss() {
        _uiState.update { it.copy(ocrImportLoading = false, ocrImportError = null, ocrRawText = null, ocrParseResult = null) }
    }

    // ── QR import ─────────────────────────────────────────────────────

    fun handleLaunchQrImageImport() {
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchQrImagePicker) }
    }

    fun handleQrImportResult(uri: Uri) {
        _uiState.update { it.copy(qrImportLoading = true, qrImportError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            val recipe = runCatching { com.paeki.fujirecipes.data.qr.RecipeQr.decodeBitmap(appContext, uri) }.getOrNull()
            delay(minLoadMs(started, minMs = 700))
            if (recipe == null) {
                _uiState.update {
                    it.copy(
                        qrImportLoading = false,
                        qrImportError = "No FujiSync recipe QR found. Make sure the QR code is sharp and fully visible.",
                    )
                }
            } else {
                handleQrImportRecipe(recipe)
            }
        }
    }

    fun handleQrImportRecipe(recipe: RecipeUiModel) {
        _uiState.update {
            it.copy(
                tab = AppTab.Library,
                qrImportLoading = false,
                qrImportError = null,
                creatingRecipe = false,
                editorRecipe = recipe.copy(libraryId = null, slot = "", referenceImageUris = emptyList()),
                editorReferenceImageUris = emptyList(),
            )
        }
    }

    fun handleQrImportDismiss() {
        _uiState.update { it.copy(qrImportLoading = false, qrImportError = null) }
    }

    fun loadCaptureLog() {
        val log = com.paeki.fujirecipes.capture.CaptureDiag.read(appContext)
        _uiState.update { it.copy(captureLog = log.ifBlank { "(log is empty)" }) }
    }

    fun clearCaptureLog() {
        com.paeki.fujirecipes.capture.CaptureDiag.clear(appContext)
        _uiState.update { it.copy(captureLog = null) }
    }

    private fun minLoadMs(startedAt: Long, minMs: Long = 1400): Long =
        (minMs - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0L)

    private fun imageDisplayName(uri: Uri): String {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    cursor.getString(index)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.trim()?.takeIf { it.isNotBlank() } ?: "image"
    }

    // ── Reference images ──────────────────────────────────────────────

    fun handleAddReferenceImage(recipe: RecipeUiModel) {
        pendingReferenceTarget = ReferenceImageTarget(
            libraryId = recipe.libraryId,
            slot = recipe.slot.takeIf { it.isNotBlank() },
            name = recipe.name,
            sim = recipe.sim,
        )
        _events.tryEmit(MainViewModelEvent.LaunchReferenceImagePicker)
    }

    fun handleAddEditorReferenceImage() {
        pendingReferenceTarget = ReferenceImageTarget(libraryId = null, slot = null, name = "", sim = "", editorDraft = true)
        _events.tryEmit(MainViewModelEvent.LaunchReferenceImagePicker)
    }

    fun handleRemoveEditorReferenceImage(uriString: String) {
        _uiState.update { state -> state.copy(editorReferenceImageUris = state.editorReferenceImageUris - uriString) }
    }

    fun applyReferenceImages(uris: List<Uri>) {
        val target = pendingReferenceTarget ?: return
        pendingReferenceTarget = null
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val localUris = withContext(Dispatchers.IO) {
                uris.mapNotNull { localStore.copyReferenceImage(it) }
            }
            if (localUris.isEmpty()) return@launch
            if (target.editorDraft) {
                _uiState.update { state ->
                    state.copy(editorReferenceImageUris = state.editorReferenceImageUris.appendReferenceImages(localUris))
                }
            } else {
                if (target.libraryId != null) {
                    libraryHolder.applyReferenceImages(target.libraryId, localUris)
                } else if (target.slot != null) {
                    _uiState.update { state ->
                        state.copy(
                            camera = state.camera.copy(
                                slots = state.camera.slots.map { r ->
                                    if (r.slot == target.slot)
                                        r.copy(referenceImageUris = r.referenceImageUris.appendReferenceImages(localUris))
                                    else r
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    fun handleRemoveReferenceImage(recipe: RecipeUiModel, uriString: String) {
        if (recipe.libraryId != null) {
            libraryHolder.removeReferenceImage(recipe.libraryId, uriString)
        } else if (recipe.slot.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    camera = state.camera.copy(
                        slots = state.camera.slots.map { r ->
                            if (r.slot == recipe.slot) r.copy(referenceImageUris = r.referenceImageUris - uriString) else r
                        },
                    ),
                    detailRecipe = state.detailRecipe?.let { r ->
                        if (r.slot == recipe.slot) r.copy(referenceImageUris = r.referenceImageUris - uriString) else r
                    },
                )
            }
            viewModelScope.launch(Dispatchers.IO) { localStore.deleteReferenceImageFile(uriString) }
        }
    }

    fun handleAddLibraryGroupImage(groupId: String) {
        pendingGroupImageTarget = groupId
        _events.tryEmit(MainViewModelEvent.LaunchGroupImagePicker)
    }

    fun applyGroupImage(uri: Uri) {
        val groupId = pendingGroupImageTarget ?: return
        pendingGroupImageTarget = null
        libraryHolder.setGroupImage(groupId, uri)
    }

    // ── Navigation / UI state ─────────────────────────────────────────

    fun setTab(tab: AppTab) = _uiState.update {
        val base = if (tab != AppTab.Library) it.copy(tab = tab, detailRecipe = null) else it.copy(tab = tab)
        if (tab != AppTab.Camera) base.copy(camera = base.camera.copy(scanError = null)) else base
    }

    fun setSelectedSlotIdx(idx: Int) = _uiState.update { it.copy(camera = it.camera.copy(selectedSlotIdx = idx)) }

    fun openDetail(recipe: RecipeUiModel) = _uiState.update { it.copy(detailRecipe = recipe) }

    fun closeDetail() = _uiState.update { it.copy(detailRecipe = null) }

    fun openRecipeCreator() = _uiState.update {
        it.copy(creatingRecipe = true, editorRecipe = null, editorReferenceImageUris = emptyList())
    }

    fun openRecipeEditor(recipe: RecipeUiModel) = _uiState.update {
        it.copy(creatingRecipe = false, editorRecipe = recipe, editorReferenceImageUris = recipe.referenceImageUris)
    }

    fun closeRecipeEditor() = _uiState.update {
        it.copy(creatingRecipe = false, editorRecipe = null, editorReferenceImageUris = emptyList(), ocrRawText = null, ocrParseResult = null)
    }

    fun saveRecipeDraft(recipe: RecipeUiModel) {
        val savedAt = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
        when {
            recipe.libraryId != null -> {
                libraryHolder.updateRecipe(recipe)
                _uiState.update { it.copy(
                    detailRecipe = if (it.detailRecipe?.libraryId == recipe.libraryId) recipe else it.detailRecipe,
                    editorRecipe = null,
                    creatingRecipe = false,
                    editorReferenceImageUris = emptyList(),
                ) }
            }
            recipe.slot.isNotBlank() -> {
                _uiState.update {
                    it.copy(
                        camera = it.camera.copy(
                            slots = it.camera.slots.map { slotRecipe ->
                                if (slotRecipe.slot == recipe.slot) recipe else slotRecipe
                            },
                        ),
                        detailRecipe = if (it.detailRecipe?.slot == recipe.slot) recipe else it.detailRecipe,
                        editorRecipe = null,
                        creatingRecipe = false,
                        editorReferenceImageUris = emptyList(),
                    )
                }
            }
            else -> {
                val saved = recipe.copy(saved = savedAt)
                if (!libraryHolder.addRecipeDraft(saved)) return
                _uiState.update {
                    it.copy(
                        tab = AppTab.Library,
                        editorRecipe = null,
                        creatingRecipe = false,
                        editorReferenceImageUris = emptyList(),
                        writeToast = WriteToastState(slot = "", name = recipe.name, savedToLibrary = true),
                    )
                }
                viewModelScope.launch {
                    delay(UiTimings.SAVE_CONFIRMATION_MS)
                    _uiState.update { it.copy(writeToast = null) }
                }
            }
        }
    }

    fun openLibraryItem(recipe: LibraryRecipeUiModel) {
        val normalizedRecipe = with(libraryHolder) { recipe.normalizedLibraryRecipe() }
        val groups = _uiState.value.library.groups
        _uiState.update {
            it.copy(
                detailRecipe = RecipeUiModel(
                    libraryId = normalizedRecipe.id,
                    slot = "",
                    name = normalizedRecipe.name,
                    sim = normalizedRecipe.sim,
                    pills = normalizedRecipe.pills,
                    description = normalizedRecipe.description,
                    effects = normalizedRecipe.effects,
                    tone = normalizedRecipe.tone,
                    wb = normalizedRecipe.wb,
                    saved = normalizedRecipe.saved,
                    sourceCameraName = normalizedRecipe.sourceCameraName,
                    sourceCameraModel = normalizedRecipe.sourceCameraModel,
                    sourceUsbId = normalizedRecipe.sourceUsbId,
                    referenceImageUris = normalizedRecipe.referenceImageUris,
                    groupIds = normalizedRecipe.groupIds,
                    group = groupLabel(normalizedRecipe.groupIds, groups),
                    favorite = normalizedRecipe.favorite,
                    isoMin = normalizedRecipe.isoMin,
                    isoMax = normalizedRecipe.isoMax,
                    exposureCompMin = normalizedRecipe.exposureCompMin,
                    exposureCompMax = normalizedRecipe.exposureCompMax,
                    sensorGens = normalizedRecipe.sensorGens,
                )
            )
        }
    }

    fun handleToggleLibraryShowImages() {
        _uiState.update { it.copy(settings = it.settings.copy(showLibraryImages = !it.settings.showLibraryImages)) }
        persistSettings()
    }

    fun handleToggleReferenceImageBlur() {
        _uiState.update { it.copy(settings = it.settings.copy(showReferenceImageBlur = !it.settings.showReferenceImageBlur)) }
        persistSettings()
    }

    fun handleToggleHaptics() {
        _uiState.update { it.copy(settings = it.settings.copy(hapticsEnabled = !it.settings.hapticsEnabled)) }
        persistSettings()
    }

    fun handleSetPropertyWriteDelay(ms: Long) {
        _uiState.update { it.copy(settings = it.settings.copy(propertyWriteDelayMs = ms.coerceIn(0L, 300L))) }
        persistSettings()
    }

    fun handleLaunchBackupExport() {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.US))
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchBackupExport("fujisync-backup-$stamp.json")) }
    }

    fun handleLaunchBackupImport(mode: BackupImportMode) {
        pendingBackupImportMode = mode
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchBackupImport) }
    }

    fun handleBackupExportDestination(uri: Uri?) {
        uri ?: return
        _uiState.update { it.copy(backup = it.backup.copy(exporting = true, message = null, error = null)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val state = _uiState.value
                    val json = localStore.backupJson(
                        settings = state.settings,
                        cameraLabels = state.camera.cameraLabels,
                        cameraModels = state.camera.cameraModels,
                        cameraFirmwares = state.camera.cameraFirmwares,
                        libraryData = libraryHolder.exportData(),
                    )
                    appContext.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open export file.")
                }
            }
            _uiState.update {
                it.copy(
                    backup = it.backup.copy(
                        exporting = false,
                        message = if (result.isSuccess) "Backup exported." else null,
                        error = result.exceptionOrNull()?.message,
                    )
                )
            }
        }
    }

    fun handleBackupImportResult(uri: Uri?) {
        uri ?: return
        _uiState.update { it.copy(backup = it.backup.copy(importing = true, message = null, error = null)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val content = appContext.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } ?: error("Could not open backup file.")
                    localStore.parseBackupJson(content)
                }
            }
            result
                .onSuccess { backup ->
                    val mode = pendingBackupImportMode
                    val mergeReport = if (mode == BackupImportMode.Merge) {
                        libraryHolder.mergeData(backup.library)
                    } else {
                        libraryHolder.replaceAll(backup.library)
                        null
                    }
                    _uiState.update {
                        val nextLabels = when (mode) {
                            BackupImportMode.Merge -> it.camera.cameraLabels + backup.cameraLabels
                            BackupImportMode.Replace -> backup.cameraLabels
                        }
                        val nextModels = when (mode) {
                            BackupImportMode.Merge -> it.camera.cameraModels + backup.cameraModels
                            BackupImportMode.Replace -> backup.cameraModels
                        }
                        val nextFirmwares = when (mode) {
                            BackupImportMode.Merge -> it.camera.cameraFirmwares + backup.cameraFirmwares
                            BackupImportMode.Replace -> backup.cameraFirmwares
                        }
                        it.copy(
                            settings = backup.settings,
                            camera = it.camera.copy(
                                cameraLabels = nextLabels,
                                cameraModels = nextModels,
                                cameraFirmwares = nextFirmwares,
                            ),
                            backup = it.backup.copy(
                                importing = false,
                                message = when (mode) {
                                    BackupImportMode.Merge -> {
                                        val report = mergeReport
                                        if (report == null) "Backup merged." else
                                            "Backup merged. Added ${report.recipesImported} recipes and ${report.groupsImported} groups. Skipped ${report.recipesSkipped} duplicate recipes."
                                    }
                                    BackupImportMode.Replace -> "Backup imported. Current data was replaced."
                                },
                                error = null,
                            ),
                        )
                    }
                    val importedState = _uiState.value
                    viewModelScope.launch(Dispatchers.IO) {
                        localStore.saveSettings(importedState.settings)
                        localStore.saveCameraLabels(importedState.camera.cameraLabels)
                        localStore.saveCameraModels(importedState.camera.cameraModels)
                        localStore.saveCameraFirmwares(importedState.camera.cameraFirmwares)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            backup = it.backup.copy(
                                importing = false,
                                message = null,
                                error = error.message ?: "Could not import backup.",
                            )
                        )
                    }
                }
        }
    }

    fun handleBackupMessageDismiss() {
        _uiState.update { it.copy(backup = it.backup.copy(message = null, error = null)) }
    }

    fun handleCheckForUpdates() {
        if (_uiState.value.update.checking || _uiState.value.update.downloading) return
        _uiState.update {
            it.copy(
                update = it.update.copy(
                    checking = true,
                    installPermissionRequired = false,
                    message = null,
                    error = null,
                )
            )
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { releaseUpdater.fetchLatestRelease() }
            result
                .onSuccess { release ->
                    latestRelease = release
                    downloadedUpdateUri = null
                    val newer = isRemoteVersionNewer(BuildConfig.VERSION_NAME, release.versionName)
                    _uiState.update {
                        it.copy(
                            update = it.update.copy(
                                checking = false,
                                latestVersion = release.versionName,
                                releaseName = release.releaseName,
                                assetName = release.assetName,
                                updateAvailable = newer,
                                downloaded = false,
                                message = if (newer) "Update ${release.versionName} is available." else "FujiSync is up to date.",
                                error = null,
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            update = it.update.copy(
                                checking = false,
                                message = null,
                                error = error.message ?: "Could not check GitHub releases.",
                            )
                        )
                    }
                }
        }
    }

    fun handleInstallUpdate() {
        if (_uiState.value.update.checking || _uiState.value.update.downloading) return
        val cachedUri = downloadedUpdateUri
        if (cachedUri != null) {
            launchPackageInstaller(cachedUri)
            return
        }

        val release = latestRelease
        if (release == null) {
            handleCheckForUpdates()
            return
        }

        _uiState.update {
            it.copy(update = it.update.copy(downloading = true, message = "Downloading ${release.versionName}...", error = null))
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { releaseUpdater.download(release) }
            result
                .onSuccess { downloaded ->
                    downloadedUpdateUri = downloaded.uri
                    _uiState.update {
                        it.copy(
                            update = it.update.copy(
                                downloading = false,
                                downloaded = true,
                                installPermissionRequired = false,
                                message = "Download ready.",
                                error = null,
                            )
                        )
                    }
                    launchPackageInstaller(downloaded.uri)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            update = it.update.copy(
                                downloading = false,
                                message = null,
                                error = error.message ?: "Could not download the update APK.",
                            )
                        )
                    }
                }
        }
    }

    private fun launchPackageInstaller(uri: Uri) {
        if (!appContext.packageManager.canRequestPackageInstalls()) {
            _uiState.update {
                it.copy(
                    update = it.update.copy(
                        installPermissionRequired = true,
                        message = "Allow FujiSync to install unknown apps, then tap Install update again.",
                        error = null,
                    )
                )
            }
            _events.tryEmit(MainViewModelEvent.OpenInstallPermissionSettings)
            return
        }
        _events.tryEmit(MainViewModelEvent.InstallApk(uri))
    }

    fun setShowCameraImageTuner(show: Boolean) = _uiState.update { it.copy(camera = it.camera.copy(showImageTuner = show)) }

    fun handleToggleFavoriteById(recipeId: String) = libraryHolder.toggleFavorite(recipeId)

    fun handleDeleteLibraryRecipes(ids: Set<String>) = libraryHolder.deleteRecipes(ids)

    fun handleCloneLibraryRecipe(recipe: RecipeUiModel) {
        val clone = libraryHolder.cloneRecipe(recipe) ?: return
        openLibraryItem(clone)
    }

    fun handleLoadSampleLibrary() = libraryHolder.loadSampleLibrary()

    // ── Profile actions ───────────────────────────────────────────────

    fun handleExploreDemo() {
        _uiState.update {
            it.copy(
                camera = it.camera.copy(
                    connected = true,
                    scanning = false,
                    scanError = null,
                    cameraModel = "X-H2S",
                    firmware = "3.10",
                    battery = "87%",
                    slots = com.paeki.fujirecipes.ui.model.SampleData.slots,
                    readingSlots = false,
                    readingSlotIndex = -1,
                ),
            )
        }
    }

    fun handleAddMockCamera() {
        val mockModels = listOf("X-H2", "X-T5", "X-S20", "X-T50", "X-M5", "X-E5", "X100VI", "X-Pro3", "X-T30 III")
        val existingCount = _uiState.value.camera.cameraLabels.size
        val model = mockModels[existingCount % mockModels.size]
        val serial = "MOCK-${UUID.randomUUID().toString().take(8).uppercase()}"
        val firmware = "%.2f".format(1 + (existingCount % 5) * 0.1)
        _uiState.update { state ->
            state.copy(
                camera = state.camera.copy(
                    cameraLabels = state.camera.cameraLabels + (serial to model),
                    cameraModels = state.camera.cameraModels + (serial to model),
                    cameraFirmwares = state.camera.cameraFirmwares + (serial to firmware),
                ),
            )
        }
        persistCameraLabels()
        persistCameraModels()
        persistCameraFirmwares()
    }

    fun handleRenameCameraLabel(serial: String, label: String) {
        if (serial.isBlank()) return
        val trimmed = label.trim().ifBlank { "My Camera" }
        val oldName = _uiState.value.camera.cameraLabels[serial]
        _uiState.update { it.copy(camera = it.camera.copy(cameraLabels = it.camera.cameraLabels + (serial to trimmed))) }
        persistCameraLabels()
        if (oldName != null) libraryHolder.renameCameraSource(oldName, trimmed)
    }

    fun handleDeleteCamera(serial: String) {
        if (serial.isBlank()) return
        _uiState.update { state ->
            state.copy(
                camera = state.camera.copy(
                    cameraLabels = state.camera.cameraLabels - serial,
                    cameraModels = state.camera.cameraModels - serial,
                    cameraFirmwares = state.camera.cameraFirmwares - serial,
                )
            )
        }
        persistCameraLabels()
        persistCameraModels()
        persistCameraFirmwares()
    }

    fun handleResetCameraLabel(serial: String) {
        if (serial.isBlank()) return
        val model = _uiState.value.camera.cameraModels[serial] ?: return
        _uiState.update { it.copy(camera = it.camera.copy(cameraLabels = it.camera.cameraLabels + (serial to model))) }
        persistCameraLabels()
    }

    fun handleBackupSlots(label: String) {
        val state = _uiState.value
        val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }

        if (selected == null || !state.camera.connected) {
            _uiState.update {
                it.copy(camera = it.camera.copy(scanError = "Connect a camera before backing up slots."))
            }
            return
        }

        if (!usbManager.hasPermission(selected.device)) {
            _events.tryEmit(MainViewModelEvent.RequestUsbPermission(selected.device))
            return
        }

        val savedAt = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
        val meta = SlotBackupMeta(
            label = label.trim().ifBlank { "C1–C7 · $savedAt" },
            savedAt = savedAt,
            id = "slot-backup-${UUID.randomUUID()}",
        )
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    camera = it.camera.copy(
                        backingUpSlots = true,
                        backingUpSlotIndex = -1,
                        scanError = null,
                    )
                )
            }

            val readResult = heartbeat.usbMutex.withLock {
                val conn = withContext(Dispatchers.IO) {
                    runCatching {
                        val c = connectionFactory.open(selected.device) ?: return@runCatching null
                        if (!c.openSession()) { c.close(); return@runCatching null }
                        c
                    }.getOrNull()
                }

                if (conn == null) {
                    Result.failure(IllegalStateException("Could not open camera for backup."))
                } else {
                    conn.use {
                        val readSlots = mutableListOf<RecipeUiModel>()
                        val failedSlots = mutableListOf<String>()
                        for ((idx, slot) in CameraSlot.entries.withIndex()) {
                            _uiState.update { current ->
                                current.copy(camera = current.camera.copy(backingUpSlotIndex = idx))
                            }
                            val preset = withContext(Dispatchers.IO) {
                                runCatching { FujiRecipeCamera(conn).readPreset(slot) }.getOrNull()
                            }
                            if (preset != null) {
                                readSlots += preset.toUiModel()
                            } else {
                                failedSlots += slot.label
                            }
                        }
                        withContext(Dispatchers.IO) { runCatching { conn.closeSession() } }

                        if (failedSlots.isEmpty()) {
                            Result.success(readSlots.toList())
                        } else {
                            Result.failure(
                                IllegalStateException(
                                    "Could not back up ${failedSlots.joinToString(", ")}. Reconnect and try again.",
                                ),
                            )
                        }
                    }
                }
            }

            val result = readResult.mapCatching { slots ->
                withContext(Dispatchers.IO) {
                    localStore.saveSlotBackupSet(meta, slots)
                    slots to localStore.loadSlotBackupSets()
                }
            }
            val remainingAnimationMs = 900L - (System.currentTimeMillis() - startedAt)
            if (remainingAnimationMs > 0L) delay(remainingAnimationMs)

            result
                .onSuccess { (slots, sets) ->
                    _uiState.update {
                        it.copy(
                            camera = it.camera.copy(
                                backingUpSlots = false,
                                backingUpSlotIndex = -1,
                                hasSlotBackup = sets.isNotEmpty(),
                                slotBackupMeta = meta,
                                slotBackupSlots = slots,
                                slotBackupSets = sets,
                                slots = slots,
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            camera = it.camera.copy(
                                backingUpSlots = false,
                                backingUpSlotIndex = -1,
                                scanError = error.message ?: "Could not save backup set.",
                            )
                        )
                    }
                }
        }
    }

    fun handleDeleteSlotBackup() {
        val selectedId = _uiState.value.camera.slotBackupMeta?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            localStore.deleteSlotBackup(selectedId)
            val sets = localStore.loadSlotBackupSets()
            val next = sets.firstOrNull()
            _uiState.update {
                it.copy(
                    camera = it.camera.copy(
                        hasSlotBackup = sets.isNotEmpty(),
                        slotBackupMeta = next?.meta,
                        slotBackupSlots = next?.slots,
                        slotBackupSets = sets,
                    )
                )
            }
        }
    }

    fun handleRenameSlotBackup(newLabel: String) {
        val meta = _uiState.value.camera.slotBackupMeta ?: return
        viewModelScope.launch(Dispatchers.IO) {
            localStore.renameSlotBackup(meta.id, newLabel)
            val sets = localStore.loadSlotBackupSets()
            val selected = sets.firstOrNull { it.meta.id == meta.id }
            _uiState.update { state ->
                state.copy(
                    camera = state.camera.copy(
                        slotBackupMeta = selected?.meta,
                        slotBackupSlots = selected?.slots,
                        slotBackupSets = sets,
                    )
                )
            }
        }
    }

    fun handleSelectSlotBackup(id: String) {
        val selected = _uiState.value.camera.slotBackupSets.firstOrNull { it.meta.id == id } ?: return
        _uiState.update { state ->
            state.copy(camera = state.camera.copy(slotBackupMeta = selected.meta, slotBackupSlots = selected.slots))
        }
    }

    fun handleRestoreSlots() {
        viewModelScope.launch {
            val backup = _uiState.value.camera.slotBackupSlots
            if (backup.isNullOrEmpty()) return@launch

            val state = _uiState.value
            val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
            if (selected == null || !state.camera.connected) {
                _uiState.update {
                    it.copy(camera = it.camera.copy(scanError = "Connect camera before restoring a set."))
                }
                return@launch
            }

            if (!usbManager.hasPermission(selected.device)) {
                _events.tryEmit(MainViewModelEvent.RequestUsbPermission(selected.device))
                return@launch
            }

            _uiState.update {
                it.copy(
                    writeBusy = true,
                    camera = it.camera.copy(
                        scanError = null,
                        restoringSlots = true,
                        restoringSlotIndex = -1,
                    ),
                )
            }

            val backupBySlot = backup.associateBy { it.slot }
            val orderedBackup = CameraSlot.entries.mapNotNull { slot ->
                backupBySlot[slot.label]?.copy(slot = slot.label)
            }
            val failedSlots = heartbeat.usbMutex.withLock {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val connection = connectionFactory.open(selected.device)
                            ?: error("Unable to open the camera's PTP USB interface.")
                        connection.use {
                            check(connection.openSession()) { "OpenSession rejected." }
                            try {
                                val camera = FujiRecipeCamera(connection, _uiState.value.settings.propertyWriteDelayMs)
                                orderedBackup.mapIndexedNotNull { index, recipe ->
                                    _uiState.update {
                                        it.copy(camera = it.camera.copy(restoringSlotIndex = index))
                                    }
                                    val slot = CameraSlot.entries.first { it.label == recipe.slot }
                                    val result = camera.writePreset(recipe.toPreset(slot))
                                    if (result.isOk) null else slot.label
                                }
                            } finally {
                                connection.closeSession()
                            }
                        }
                    }
                }
            }

            failedSlots.fold(
                onSuccess = { failed ->
                    // Re-read all slots from camera after the restore pass to validate what actually landed.
                    _uiState.update {
                        it.copy(
                            writeBusy = false,
                            camera = it.camera.copy(
                                connected = true,
                                scanError = failed
                                    .takeIf { it.isNotEmpty() }
                                    ?.joinToString(prefix = "Restore reported write errors for ", postfix = ". Validating camera slots now."),
                                restoringSlots = false,
                                restoringSlotIndex = -1,
                                readingSlots = true,
                                readingSlotIndex = -1,
                                slots = emptyList(),
                                isRestoringValidation = true,
                            ),
                            tab = AppTab.Camera,
                        )
                    }
                    readAllSlots(selected.device)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            writeBusy = false,
                            camera = it.camera.copy(
                                scanError = error.message ?: "Restore failed.",
                                restoringSlots = false,
                                restoringSlotIndex = -1,
                            ),
                        )
                    }
                },
            )
        }
    }

    // ── Discover → Library ────────────────────────────────────────────

    suspend fun handleSaveFromDiscover(recipe: FxwRecipe, name: String, includePhotos: Boolean = true) {
        libraryHolder.saveFromDiscover(recipe, name, includePhotos)
    }

    // ── Persistence ───────────────────────────────────────────────────

    private fun loadPersistedState() {
        libraryHolder.load()
        viewModelScope.launch {
            val backupSets = withContext(Dispatchers.IO) { localStore.loadSlotBackupSets() }
            val selectedBackup = backupSets.firstOrNull()
            val cameraLabels = withContext(Dispatchers.IO) { localStore.loadCameraLabels() }
            val cameraModels = withContext(Dispatchers.IO) { localStore.loadCameraModels() }
            val cameraFirmwares = withContext(Dispatchers.IO) { localStore.loadCameraFirmwares() }
            val settings = withContext(Dispatchers.IO) { localStore.loadSettings() }
            _uiState.update {
                it.copy(
                    camera = it.camera.copy(
                        hasSlotBackup = backupSets.isNotEmpty(),
                        slotBackupMeta = selectedBackup?.meta,
                        slotBackupSlots = selectedBackup?.slots,
                        slotBackupSets = backupSets,
                        cameraLabels = cameraLabels,
                        cameraModels = cameraModels,
                        cameraFirmwares = cameraFirmwares,
                    ),
                    settings = settings,
                )
            }
        }
    }

    private fun persistCameraLabels() {
        val labels = _uiState.value.camera.cameraLabels
        viewModelScope.launch(Dispatchers.IO) { localStore.saveCameraLabels(labels) }
    }

    private fun persistCameraModels() {
        val models = _uiState.value.camera.cameraModels
        viewModelScope.launch(Dispatchers.IO) { localStore.saveCameraModels(models) }
    }

    private fun persistCameraFirmwares() {
        val firmwares = _uiState.value.camera.cameraFirmwares
        viewModelScope.launch(Dispatchers.IO) { localStore.saveCameraFirmwares(firmwares) }
    }

    private fun persistSettings() {
        val settings = _uiState.value.settings
        viewModelScope.launch(Dispatchers.IO) { localStore.saveSettings(settings) }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun List<RecipeUiModel>.replaceSlot(slot: CameraSlot, recipe: RecipeUiModel): List<RecipeUiModel> {
        val slotIndex = indexOfFirst { it.slot == slot.label }
        return if (slotIndex >= 0) {
            mapIndexed { index, existing -> if (index == slotIndex) recipe else existing }
        } else {
            this + recipe
        }
    }

    private fun RecipeUiModel.sameRecipeIgnoringSlot(other: RecipeUiModel): Boolean =
        copy(slot = "") == other.copy(slot = "")

    private fun List<String>.appendReferenceImages(uris: List<String>): List<String> =
        (this + uris).distinct().take(20)

    private fun RecipeUiModel.refreshFromLibrary(
        recipes: List<LibraryRecipeUiModel>,
        groups: List<LibraryGroupUiModel>,
    ): RecipeUiModel? {
        if (libraryId == null) return this
        val updated = recipes.firstOrNull { it.id == libraryId } ?: return null
        return copy(
            name = updated.name,
            sim = updated.sim,
            pills = updated.pills,
            description = updated.description,
            effects = updated.effects,
            tone = updated.tone,
            wb = updated.wb,
            referenceImageUris = updated.referenceImageUris,
            groupIds = updated.groupIds,
            group = groupLabel(updated.groupIds, groups),
            favorite = updated.favorite,
        )
    }

    private fun groupLabel(groupIds: List<String>, groups: List<LibraryGroupUiModel>): String? {
        if (groupIds.isEmpty()) return null
        val namesById = groups.associate { it.id to it.name }
        return groupIds.mapNotNull { namesById[it] }.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }
}
