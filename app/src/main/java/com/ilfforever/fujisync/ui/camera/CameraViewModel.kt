package com.ilfforever.fujisync.ui.camera

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.ilfforever.fujisync.BuildConfig
import com.ilfforever.fujisync.R
import com.ilfforever.fujisync.data.local.LocalStore
import com.ilfforever.fujisync.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import com.ilfforever.fujisync.data.usb.CameraHeartbeat
import com.ilfforever.fujisync.data.usb.CameraSessionManager
import com.ilfforever.fujisync.data.usb.CameraUsbMode
import com.ilfforever.fujisync.data.usb.FujiPtpProbe
import com.ilfforever.fujisync.data.usb.FujiPtpProbeResult
import com.ilfforever.fujisync.data.usb.FujiRecipeCamera
import com.ilfforever.fujisync.data.usb.UsbPtpConnection
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.RecipePreset
import com.ilfforever.fujisync.domain.repository.CameraRepository
import com.ilfforever.fujisync.ui.CameraUiState
import com.ilfforever.fujisync.ui.UiTimings
import com.ilfforever.fujisync.ui.WriteToastState
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.SlotBackupMeta
import com.ilfforever.fujisync.ui.model.toPreset
import com.ilfforever.fujisync.ui.model.toUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

sealed class CameraEvent {
    data class RequestUsbPermission(val device: UsbDevice) : CameraEvent()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val usbManager: UsbManager,
    private val repository: CameraRepository,
    private val connectionFactory: UsbPtpConnection,
    private val sessionManager: CameraSessionManager,
    private val localStore: LocalStore,
    private val heartbeat: CameraHeartbeat,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    private val _writeBusy = MutableStateFlow(false)
    val writeBusy: StateFlow<Boolean> = _writeBusy.asStateFlow()

    private val _writeToast = MutableStateFlow<WriteToastState?>(null)
    val writeToast: StateFlow<WriteToastState?> = _writeToast.asStateFlow()

    private val _events = MutableSharedFlow<CameraEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CameraEvent> = _events.asSharedFlow()

    // DEBUG: rearrange flow log
    private val _rearrangeDebugLog = MutableStateFlow("")
    val rearrangeDebugLog: StateFlow<String> = _rearrangeDebugLog.asStateFlow()
    private fun rdbg(msg: String) {
        if (!BuildConfig.DEBUG) return
        val ts = System.currentTimeMillis() % 100000
        _rearrangeDebugLog.value += "[$ts] $msg\n"
    }

    private var heartbeatJob: Job? = null

    init {
        refreshDevices()
        loadSlotBackups()
    }

    // ── USB / connection ──────────────────────────────────────────────

    fun refreshDevices() {
        val devices = repository.scanUsb()
        if (devices.none { it.mode == CameraUsbMode.Ptp }) {
            stopHeartbeat()
            _state.update { it.copy(connected = false) }
        }
    }

    fun onReconnect() {
        val state = _state.value
        if (state.connected) {
            val ptpDevice = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
            if (ptpDevice == null) {
                _state.update {
                    it.copy(
                        connected = false,
                        cameraModel = "",
                        firmware = "",
                        battery = "",
                        slots = emptyList(),
                        readingSlotIndex = -1,
                        scanError = appContext.getString(R.string.error_camera_not_found),
                    )
                }
                return
            }
            if (!usbManager.hasPermission(ptpDevice.device)) {
                _events.tryEmit(CameraEvent.RequestUsbPermission(ptpDevice.device))
                return
            }
            _state.update {
                it.copy(readingSlots = true, readingSlotIndex = -1, slots = emptyList(), scanError = null, isRearrangeValidation = false)
            }
            readAllSlots(ptpDevice.device)
            return
        }

        _state.update { it.copy(scanning = true, scanError = null) }
        viewModelScope.launch {
            val devices = withContext(ioDispatcher) { repository.scanUsb() }
            val ptpDevice = devices.firstOrNull { it.mode == CameraUsbMode.Ptp }
            if (ptpDevice == null) {
                val fujiMode = devices.firstOrNull()?.mode
                val message = when (fujiMode) {
                    CameraUsbMode.CardReader -> appContext.getString(R.string.error_card_reader_mode)
                    CameraUsbMode.Other -> appContext.getString(R.string.error_no_ptp_interface)
                    CameraUsbMode.NotPlugged, null -> appContext.getString(R.string.error_no_camera_found)
                    CameraUsbMode.Ptp -> appContext.getString(R.string.error_no_camera_found)
                }
                _state.update { it.copy(scanning = false, scanError = message) }
                return@launch
            }
            if (!usbManager.hasPermission(ptpDevice.device)) {
                _state.update { it.copy(scanning = false) }
                _events.tryEmit(CameraEvent.RequestUsbPermission(ptpDevice.device))
                return@launch
            }
            probeDevice(ptpDevice.device)
        }
    }

    fun onUsbPermissionResult(device: UsbDevice, granted: Boolean) {
        if (granted) {
            probeDevice(device)
        } else {
            _state.update { it.copy(scanning = false, scanError = appContext.getString(R.string.error_usb_permission_denied)) }
            _writeBusy.value = false
        }
    }

    fun probeDevice(device: UsbDevice) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                runCatching { FujiPtpProbe(connectionFactory).probe(device) }
                    .getOrElse { FujiPtpProbeResult.NotReady(reason = it.message ?: appContext.getString(R.string.error_probe_failed)) }
            }
            when (result) {
                is FujiPtpProbeResult.Ready -> {
                    val model = result.deviceInfo.model.ifBlank { "Fujifilm" }
                    val serial = result.bestSerial
                    val fw = result.deviceInfo.deviceVersion.ifBlank { "—" }
                    _state.update { state ->
                        val updatedLabels = if (serial.isNotBlank() && serial !in state.cameraLabels) {
                            state.cameraLabels + (serial to model)
                        } else state.cameraLabels
                        val updatedModels = if (serial.isNotBlank()) {
                            state.cameraModels + (serial to model)
                        } else state.cameraModels
                        val updatedFirmwares = if (serial.isNotBlank() && fw != "—") {
                            state.cameraFirmwares + (serial to fw)
                        } else state.cameraFirmwares
                        state.copy(
                            connected = true,
                            scanning = false,
                            readingSlots = true,
                            cameraModel = model,
                            cameraSerial = serial,
                            battery = result.batteryPercent?.let { "$it%" } ?: "—",
                            firmware = fw,
                            slots = emptyList(),
                            cameraLabels = updatedLabels,
                            cameraModels = updatedModels,
                            cameraFirmwares = updatedFirmwares,
                            isRearrangeValidation = false,
                        )
                    }
                    if (serial.isNotBlank()) persistCameraMeta()
                    startHeartbeat(device)
                    readAllSlots(device)
                }
                is FujiPtpProbeResult.NotReady -> {
                    _state.update { it.copy(connected = false, scanning = false, scanError = result.reason) }
                }
            }
        }
    }

    private fun readAllSlots(device: UsbDevice) {
        viewModelScope.launch {
            var failCount = 0
            val result = sessionManager.withRawSession(device) { conn ->
                val readSlots = mutableListOf<RecipeUiModel>()
                for ((idx, slot) in CameraSlot.entries.withIndex()) {
                    _state.update { it.copy(readingSlotIndex = idx) }
                    val preset = runCatching { FujiRecipeCamera(conn).readPreset(slot) }.getOrNull()
                    val recipe = if (preset != null) {
                        preset.toUiModel()
                    } else {
                        failCount++
                        RecipeUiModel(slot = slot.label, name = "READ FAILED", sim = "—", pills = emptyList())
                    }
                    readSlots.add(recipe)
                    _state.update { it.copy(slots = readSlots.toList()) }
                }
                readSlots.toList()
            }

            _state.update {
                it.copy(
                    readingSlots = false,
                    readingSlotIndex = -1,
                    isRestoringValidation = false,
                    scanError = if (failCount > 0) appContext.getString(R.string.error_slots_could_not_read, failCount) else null,
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
                    _state.update { it.copy(slots = presets.map { p -> p.toUiModel() }) }
                }
            }
            heartbeat.alive.drop(1).collect { alive ->
                if (!alive && _state.value.connected) {
                    _state.update { it.copy(connected = false, scanError = appContext.getString(R.string.error_camera_disconnected)) }
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

    fun handleWrite(recipe: RecipeUiModel) {
        val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
        _writeBusy.value = true

        if (selected == null || !_state.value.connected) {
            _writeBusy.value = false
            return
        }
        if (!usbManager.hasPermission(selected.device)) {
            _events.tryEmit(CameraEvent.RequestUsbPermission(selected.device))
            return
        }

        val targetSlotLabel = recipe.slot.ifEmpty { "C1" }
        val targetSlot = CameraSlot.entries.firstOrNull { it.label == targetSlotLabel } ?: CameraSlot.C1

        viewModelScope.launch {
            val writeResult = sessionManager.withSession(selected.device, _state.value.writeDelayMs) { camera, _ ->
                camera.writePreset(recipe.toPreset(targetSlot))
            }
            if (writeResult.isFailure) {
                _writeBusy.value = false
                _state.update { it.copy(scanError = writeResult.exceptionOrNull()?.message ?: appContext.getString(R.string.error_write_failed)) }
                return@launch
            }
            val updatedRecipe = recipe.copy(slot = targetSlot.label)
            _state.update { it.copy(slots = it.slots.replaceSlot(targetSlot, updatedRecipe)) }
            _writeBusy.value = false
            _writeToast.value = WriteToastState(slot = targetSlotLabel, name = recipe.name)
            delay(UiTimings.TOAST_DISMISS_MS)
            _writeToast.value = null
        }
    }

    fun handleWriteToSlot(recipe: RecipeUiModel, targetSlotLabel: String) {
        val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
        _writeBusy.value = true

        if (selected == null || !_state.value.connected) {
            _writeBusy.value = false
            return
        }
        if (!usbManager.hasPermission(selected.device)) {
            _events.tryEmit(CameraEvent.RequestUsbPermission(selected.device))
            return
        }

        val slot = CameraSlot.entries.firstOrNull { it.label == targetSlotLabel } ?: CameraSlot.C1

        viewModelScope.launch {
            val writeResult = sessionManager.withSession(selected.device, _state.value.writeDelayMs) { camera, _ ->
                camera.writePreset(recipe.toPreset(slot))
            }
            if (writeResult.isFailure) {
                _writeBusy.value = false
                _state.update { it.copy(scanError = writeResult.exceptionOrNull()?.message ?: appContext.getString(R.string.error_write_failed)) }
                return@launch
            }
            val updatedRecipe = recipe.copy(slot = slot.label, libraryId = null)
            _state.update { it.copy(slots = it.slots.replaceSlot(slot, updatedRecipe)) }
            _writeBusy.value = false
            _writeToast.value = WriteToastState(slot = targetSlotLabel, name = recipe.name)
            delay(UiTimings.TOAST_DISMISS_MS)
            _writeToast.value = null
        }
    }

    fun dismissRearrangeValidation() {
        _state.update { it.copy(isRearrangeValidation = false) }
    }

    fun handleRearrangeCameraSlots(nextSlots: List<RecipeUiModel>) {
        _rearrangeDebugLog.value = ""
        rdbg("START writes=${nextSlots.size} slots")
        val state = _state.value
        val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
        val ordered = CameraSlot.entries.mapIndexedNotNull { index, slot ->
            nextSlots.getOrNull(index)?.copy(slot = slot.label, libraryId = null)
        }
        if (ordered.size != CameraSlot.entries.size) { rdbg("ABORT ordered.size=${ordered.size}"); return }
        val writes = ordered.mapIndexedNotNull { index, recipe ->
            val current = state.slots.getOrNull(index)
            if (current != null && current.sameRecipeIgnoringSlot(recipe)) null else CameraSlot.entries[index] to recipe
        }
        if (writes.isEmpty()) { rdbg("ABORT no changes"); return }
        rdbg("writes needed: ${writes.size} slots: ${writes.map { it.first.label }}")

        if (selected == null || !state.connected) {
            rdbg("ABORT no camera")
            _state.update { it.copy(scanError = appContext.getString(R.string.error_connect_before_rearrange)) }
            return
        }
        if (!usbManager.hasPermission(selected.device)) {
            rdbg("ABORT no permission")
            _events.tryEmit(CameraEvent.RequestUsbPermission(selected.device))
            return
        }

        val previouslySelected = state.slots.getOrNull(state.selectedSlotIdx)
        rdbg("SET writeBusy=true rearrangingSlots=true")
        _writeBusy.value = true
        _state.update {
            it.copy(
                scanError = null,
                rearrangingSlots = true,
                rearrangingSlotIndex = -1,
                rearrangingWriteIndex = -1,
                rearrangingWriteTotal = writes.size,
            )
        }

        viewModelScope.launch {
            rdbg("LAUNCH acquiring usbMutex")
            val writeResult = sessionManager.withSession(selected.device, state.writeDelayMs) { camera, _ ->
                rdbg("LOCKED usbMutex")
                writes.mapIndexedNotNull { index, (slot, recipe) ->
                    rdbg("WRITE ${slot.label} ($index/${writes.size})")
                    _state.update {
                        it.copy(rearrangingSlotIndex = CameraSlot.entries.indexOf(slot), rearrangingWriteIndex = index)
                    }
                    val result = camera.writePreset(recipe.toPreset(slot))
                    if (result.isOk) null else slot.label
                }
            }
            rdbg("RELEASED usbMutex")

            writeResult.fold(
                onSuccess = { failed ->
                    val selectedIdx = previouslySelected
                        ?.let { sel -> ordered.indexOfFirst { it.sameRecipeIgnoringSlot(sel) } }
                        ?.takeIf { it >= 0 }
                        ?: _state.value.selectedSlotIdx.coerceIn(0, ordered.lastIndex)
                    val errorMsg = if (failed.isNotEmpty()) {
                        rdbg("PARTIAL FAIL slots: $failed — proceeding to validation")
                        appContext.getString(R.string.error_write_errors_on, failed.joinToString())
                    } else {
                        rdbg("SUCCESS → validation read")
                        null
                    }
                    _writeBusy.value = false
                    _state.update {
                        it.copy(
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
                        )
                    }
                    rdbg("calling readAllSlots")
                    readAllSlots(selected.device)
                    rdbg("readAllSlots returned")
                },
                onFailure = { error ->
                    rdbg("EXCEPTION: ${error.message}")
                    _writeBusy.value = false
                    _state.update {
                        it.copy(
                            scanError = error.message ?: appContext.getString(R.string.error_rearrange_failed),
                            rearrangingSlots = false,
                            rearrangingSlotIndex = -1,
                            rearrangingWriteIndex = -1,
                            rearrangingWriteTotal = 0,
                        )
                    }
                },
            )
        }
    }

    // ── Slot backup / restore ─────────────────────────────────────────

    fun handleBackupSlots(label: String) {
        val state = _state.value
        val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }

        if (selected == null || !state.connected) {
            _state.update { it.copy(scanError = appContext.getString(R.string.error_connect_before_backup)) }
            return
        }
        if (!usbManager.hasPermission(selected.device)) {
            _events.tryEmit(CameraEvent.RequestUsbPermission(selected.device))
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
            _state.update { it.copy(backingUpSlots = true, backingUpSlotIndex = -1, scanError = null) }

            val readResult = sessionManager.withRawSession(selected.device) { conn ->
                val readSlots = mutableListOf<RecipeUiModel>()
                val failedSlots = mutableListOf<String>()
                for ((idx, slot) in CameraSlot.entries.withIndex()) {
                    _state.update { it.copy(backingUpSlotIndex = idx) }
                    val preset = runCatching { FujiRecipeCamera(conn).readPreset(slot) }.getOrNull()
                    if (preset != null) readSlots += preset.toUiModel()
                    else failedSlots += slot.label
                }
                if (failedSlots.isNotEmpty()) {
                    error(appContext.getString(R.string.error_backup_read_failed, failedSlots.joinToString(", ")))
                }
                readSlots.toList()
            }

            val result = readResult.mapCatching { slots ->
                withContext(ioDispatcher) {
                    localStore.saveSlotBackupSet(meta, slots)
                    slots to localStore.loadSlotBackupSets()
                }
            }
            val remainingMs = 900L - (System.currentTimeMillis() - startedAt)
            if (remainingMs > 0L) delay(remainingMs)

            result.fold(
                onSuccess = { (slots, sets) ->
                    _state.update {
                        it.copy(
                            backingUpSlots = false,
                            backingUpSlotIndex = -1,
                            hasSlotBackup = sets.isNotEmpty(),
                            slotBackupMeta = meta,
                            slotBackupSlots = slots,
                            slotBackupSets = sets,
                            slots = slots,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(backingUpSlots = false, backingUpSlotIndex = -1, scanError = error.message ?: appContext.getString(R.string.error_backup_save_failed))
                    }
                },
            )
        }
    }

    fun handleComposeSet(label: String, slots: List<RecipeUiModel>) {
        val savedAt = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
        val meta = SlotBackupMeta(
            label = label.trim().ifBlank { "Custom · $savedAt" },
            savedAt = savedAt,
            id = "slot-backup-${UUID.randomUUID()}",
        )
        viewModelScope.launch(ioDispatcher) {
            localStore.saveSlotBackupSet(meta, slots)
            val sets = localStore.loadSlotBackupSets()
            _state.update {
                it.copy(
                    hasSlotBackup = sets.isNotEmpty(),
                    slotBackupMeta = meta,
                    slotBackupSlots = slots,
                    slotBackupSets = sets,
                )
            }
        }
    }

    fun handleDeleteSlotBackup() {
        val selectedId = _state.value.slotBackupMeta?.id ?: return
        viewModelScope.launch(ioDispatcher) {
            localStore.deleteSlotBackup(selectedId)
            val sets = localStore.loadSlotBackupSets()
            val next = sets.firstOrNull()
            _state.update {
                it.copy(hasSlotBackup = sets.isNotEmpty(), slotBackupMeta = next?.meta, slotBackupSlots = next?.slots, slotBackupSets = sets)
            }
        }
    }

    fun handleRenameSlotBackup(newLabel: String) {
        val meta = _state.value.slotBackupMeta ?: return
        viewModelScope.launch(ioDispatcher) {
            localStore.renameSlotBackup(meta.id, newLabel)
            val sets = localStore.loadSlotBackupSets()
            val selected = sets.firstOrNull { it.meta.id == meta.id }
            _state.update { it.copy(slotBackupMeta = selected?.meta, slotBackupSlots = selected?.slots, slotBackupSets = sets) }
        }
    }

    fun handleSelectSlotBackup(id: String) {
        val selected = _state.value.slotBackupSets.firstOrNull { it.meta.id == id } ?: return
        _state.update { it.copy(slotBackupMeta = selected.meta, slotBackupSlots = selected.slots) }
    }

    fun handleRestoreSlots() {
        viewModelScope.launch {
            val backup = _state.value.slotBackupSlots
            if (backup.isNullOrEmpty()) return@launch

            val selected = repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
            if (selected == null || !_state.value.connected) {
                _state.update { it.copy(scanError = appContext.getString(R.string.error_connect_before_restore)) }
                return@launch
            }
            if (!usbManager.hasPermission(selected.device)) {
                _events.tryEmit(CameraEvent.RequestUsbPermission(selected.device))
                return@launch
            }

            _writeBusy.value = true
            _state.update { it.copy(scanError = null, restoringSlots = true, restoringSlotIndex = -1) }

            val orderedBackup = CameraSlot.entries.mapNotNull { slot ->
                backup.firstOrNull { it.slot == slot.label }?.copy(slot = slot.label)
            }

            val writeResult = sessionManager.withSession(selected.device, _state.value.writeDelayMs) { camera, _ ->
                orderedBackup.mapIndexedNotNull { index, recipe ->
                    _state.update { it.copy(restoringSlotIndex = index) }
                    val slot = CameraSlot.entries.first { it.label == recipe.slot }
                    val result = camera.writePreset(recipe.toPreset(slot))
                    if (result.isOk) null else slot.label
                }
            }

            writeResult.fold(
                onSuccess = { failed ->
                    _writeBusy.value = false
                    _state.update {
                        it.copy(
                            scanError = failed.takeIf { f -> f.isNotEmpty() }
                                ?.let { f -> appContext.getString(R.string.error_restore_write_errors, f.joinToString()) },
                            restoringSlots = false,
                            restoringSlotIndex = -1,
                            readingSlots = true,
                            readingSlotIndex = -1,
                            slots = emptyList(),
                            isRestoringValidation = true,
                        )
                    }
                    readAllSlots(selected.device)
                },
                onFailure = { error ->
                    _writeBusy.value = false
                    _state.update { it.copy(scanError = error.message ?: appContext.getString(R.string.error_restore_failed), restoringSlots = false, restoringSlotIndex = -1) }
                },
            )
        }
    }

    // ── State accessors ───────────────────────────────────────────────

    fun setSelectedSlotIdx(idx: Int) = _state.update { it.copy(selectedSlotIdx = idx) }
    fun setShowImageTuner(show: Boolean) = _state.update { it.copy(showImageTuner = show) }
    fun clearScanError() = _state.update { it.copy(scanError = null) }

    /** Called externally when property write delay setting changes. */
    fun setWriteDelayMs(ms: Long) { _state.update { it.copy(writeDelayMs = ms) } }

    // ── Camera labels / meta ──────────────────────────────────────────

    fun renameCameraLabel(serial: String, label: String) {
        if (serial.isBlank()) return
        val trimmed = label.trim().ifBlank { "My Camera" }
        _state.update { it.copy(cameraLabels = it.cameraLabels + (serial to trimmed)) }
        persistCameraMeta()
    }

    fun deleteCamera(serial: String) {
        if (serial.isBlank()) return
        _state.update { it.copy(cameraLabels = it.cameraLabels - serial, cameraModels = it.cameraModels - serial, cameraFirmwares = it.cameraFirmwares - serial) }
        persistCameraMeta()
    }

    fun resetCameraLabel(serial: String) {
        if (serial.isBlank()) return
        val model = _state.value.cameraModels[serial] ?: return
        _state.update { it.copy(cameraLabels = it.cameraLabels + (serial to model)) }
        persistCameraMeta()
    }

    fun addMockCamera() {
        val mockModels = listOf("X-H2", "X-T5", "X-S20", "X-T50", "X-M5", "X-E5", "X100VI", "X-Pro3", "X-T30 III")
        val existingCount = _state.value.cameraLabels.size
        val model = mockModels[existingCount % mockModels.size]
        val serial = "MOCK-${UUID.randomUUID().toString().take(8).uppercase()}"
        val firmware = "%.2f".format(1 + (existingCount % 5) * 0.1)
        _state.update {
            it.copy(
                cameraLabels = it.cameraLabels + (serial to model),
                cameraModels = it.cameraModels + (serial to model),
                cameraFirmwares = it.cameraFirmwares + (serial to firmware),
            )
        }
        persistCameraMeta()
    }

    fun exploreDemo() {
        _state.update {
            it.copy(
                connected = true,
                scanning = false,
                scanError = null,
                cameraModel = "X-H2S",
                firmware = "3.10",
                battery = "87%",
                slots = com.ilfforever.fujisync.ui.model.SampleData.slots,
                readingSlots = false,
                readingSlotIndex = -1,
            )
        }
    }

    // ── Persistence ───────────────────────────────────────────────────

    fun reloadPersistedData() = loadSlotBackups()

    private fun loadSlotBackups() {
        viewModelScope.launch {
            val backupSets = withContext(ioDispatcher) { localStore.loadSlotBackupSets() }
            val cameraLabels = withContext(ioDispatcher) { localStore.loadCameraLabels() }
            val cameraModels = withContext(ioDispatcher) { localStore.loadCameraModels() }
            val cameraFirmwares = withContext(ioDispatcher) { localStore.loadCameraFirmwares() }
            val selected = backupSets.firstOrNull()
            _state.update {
                it.copy(
                    hasSlotBackup = backupSets.isNotEmpty(),
                    slotBackupMeta = selected?.meta,
                    slotBackupSlots = selected?.slots,
                    slotBackupSets = backupSets,
                    cameraLabels = cameraLabels,
                    cameraModels = cameraModels,
                    cameraFirmwares = cameraFirmwares,
                )
            }
        }
    }

    private fun persistCameraMeta() {
        val state = _state.value
        viewModelScope.launch(ioDispatcher) {
            localStore.saveCameraLabels(state.cameraLabels)
            localStore.saveCameraModels(state.cameraModels)
            localStore.saveCameraFirmwares(state.cameraFirmwares)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun List<RecipeUiModel>.replaceSlot(slot: CameraSlot, recipe: RecipeUiModel): List<RecipeUiModel> {
        val slotIndex = indexOfFirst { it.slot == slot.label }
        return if (slotIndex >= 0) mapIndexed { index, existing -> if (index == slotIndex) recipe else existing }
        else this + recipe
    }

    private fun RecipeUiModel.sameRecipeIgnoringSlot(other: RecipeUiModel): Boolean =
        copy(slot = "") == other.copy(slot = "")
}
