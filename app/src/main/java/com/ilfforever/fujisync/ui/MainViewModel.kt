package com.ilfforever.fujisync.ui

import android.content.Context
import android.hardware.usb.UsbDevice
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.ilfforever.fujisync.BuildConfig
import com.ilfforever.fujisync.data.local.LocalStore
import com.ilfforever.fujisync.data.remote.FxwRecipe
import com.ilfforever.fujisync.data.update.AppUpdateRelease
import com.ilfforever.fujisync.data.update.GitHubReleaseUpdater
import com.ilfforever.fujisync.data.update.isRemoteVersionNewer
import com.ilfforever.fujisync.ui.library.LibraryStateHolder
import com.ilfforever.fujisync.ui.model.LibraryGroupUiModel
import com.ilfforever.fujisync.ui.model.LibraryRecipeSource
import com.ilfforever.fujisync.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.toUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    object LaunchShutterCheckPicker : MainViewModelEvent()
    object LaunchSmartRefPicker : MainViewModelEvent()
    object BackupImported : MainViewModelEvent()
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
    private val localStore: LocalStore,
    private val libraryHolder: LibraryStateHolder,
    private val releaseUpdater: GitHubReleaseUpdater,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FujiSyncUiState())
    val uiState: StateFlow<FujiSyncUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainViewModelEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<MainViewModelEvent> = _events.asSharedFlow()
    private var latestRelease: AppUpdateRelease? = null
    private var downloadedUpdateUri: Uri? = null
    private var pendingBackupImportMode: BackupImportMode = BackupImportMode.Merge

    private var pendingReferenceTarget: ReferenceImageTarget? = null
    private var pendingGroupImageTarget: String? = null

    init {
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

    // ── USB / connection (delegated to CameraViewModel) ─────────────

    // ── Library save / duplicate (delegates to LibraryStateHolder) ────

    fun handleSaveToLibrary(source: LibraryRecipeSource, recipe: RecipeUiModel?) {
        val target = recipe ?: _uiState.value.detailRecipe ?: return
        libraryHolder.addRecipe(target, source)
    }

    fun handleSaveAllSlotsToLibrary(source: LibraryRecipeSource, slots: List<RecipeUiModel>) {
        val validSlots = slots.filter { it.name != "READ FAILED" && it.sim.isNotBlank() }
        if (validSlots.isEmpty()) return
        val report = libraryHolder.addRecipesBatch(validSlots, source)
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

    // ── Import (delegated to ImportViewModel) ───────────────────────

    fun handleImportedRecipe(recipe: RecipeUiModel, referenceImageUris: List<String> = emptyList()) {
        _uiState.update {
            it.copy(
                creatingRecipe = false,
                editorRecipe = recipe,
                editorReferenceImageUris = referenceImageUris,
            )
        }
    }

    fun handleLaunchExifImport() {
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchExifImagePicker) }
    }

    fun handleExifImportDismiss() {
        _uiState.update { it.copy(exifImportLoading = false, exifImportError = null) }
    }

    // ── Shutter count check ───────────────────────────────────────────

    fun handleLaunchShutterCheck() {
        _uiState.update { it.copy(shutterCheckError = null, shutterCount = null) }
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchShutterCheckPicker) }
    }

    fun handleShutterCheckDismiss() {
        _uiState.update { it.copy(shutterCount = null, shutterCheckLoading = false, shutterCheckError = null) }
    }

    // ── Smart Reference (delegated to ImportViewModel) ────────────────

    fun handleLaunchSmartRef() {
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchSmartRefPicker) }
    }

    // ── OCR / screenshot import (delegated to ImportViewModel) ────────

    fun handleLaunchOcrImport() {
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchOcrImagePicker) }
    }

    fun handleOcrImportDismiss() {
        _uiState.update { it.copy(ocrImportLoading = false, ocrImportError = null, ocrRawText = null, ocrParseResult = null) }
    }

    // ── QR import (delegated to ImportViewModel) ──────────────────────

    fun handleLaunchQrImageImport() {
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchQrImagePicker) }
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
        val log = com.ilfforever.fujisync.capture.CaptureDiag.read(appContext)
        _uiState.update { it.copy(captureLog = log.ifBlank { "(log is empty)" }) }
    }

    fun clearCaptureLog() {
        com.ilfforever.fujisync.capture.CaptureDiag.clear(appContext)
        _uiState.update { it.copy(captureLog = null) }
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

    fun handleReorderReferenceImages(recipe: RecipeUiModel, fromIndex: Int, toIndex: Int) {
        if (recipe.libraryId != null) {
            libraryHolder.reorderReferenceImages(recipe.libraryId, fromIndex, toIndex)
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
                    sourceUrl = normalizedRecipe.sourceUrl,
                    sourceLabel = normalizedRecipe.sourceLabel,
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

    fun handleToggleCardImageCount() {
        _uiState.update { it.copy(settings = it.settings.copy(showCardImageCount = !it.settings.showCardImageCount)) }
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

    fun handleToggleFavoritesOnTop() {
        _uiState.update { it.copy(settings = it.settings.copy(favoritesOnTop = !it.settings.favoritesOnTop)) }
        persistSettings()
    }

    fun handleSetSmartRefSimilarityPct(pct: Int) {
        _uiState.update { it.copy(settings = it.settings.copy(smartRefSimilarityPct = pct.coerceIn(40, 90))) }
        persistSettings()
    }

    fun handleSetMaxReferenceImages(value: Int) {
        val coerced = value.coerceIn(5, 100)
        _uiState.update { it.copy(settings = it.settings.copy(maxReferenceImages = coerced)) }
        libraryHolder.maxReferenceImages = coerced
        persistSettings()
    }

    fun handleSetPropertyWriteDelay(ms: Long) {
        _uiState.update { it.copy(settings = it.settings.copy(propertyWriteDelayMs = ms.coerceIn(0L, 300L))) }
        persistSettings()
    }

    fun handleLaunchBackupExport() {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.US))
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchBackupExport("fujisync-backup-$stamp.zip")) }
    }

    fun handleLaunchBackupImport(mode: BackupImportMode) {
        pendingBackupImportMode = mode
        viewModelScope.launch { _events.emit(MainViewModelEvent.LaunchBackupImport) }
    }

    fun handleBackupExportDestination(uri: Uri?) {
        uri ?: return
        _uiState.update { it.copy(backup = it.backup.copy(exporting = true, exportProgress = 0f, exportTotal = 0, message = null, error = null)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val state = _uiState.value
                    val libraryData = libraryHolder.exportData()
                    val slotBackupSets = localStore.loadSlotBackupSets()
                    appContext.contentResolver.openOutputStream(uri)?.use { output ->
                        localStore.backupZip(
                            outputStream = output,
                            settings = state.settings,
                            cameraLabels = state.camera.cameraLabels,
                            cameraModels = state.camera.cameraModels,
                            cameraFirmwares = state.camera.cameraFirmwares,
                            libraryData = libraryData,
                            slotBackupSets = slotBackupSets,
                            onProgress = { current, total ->
                                _uiState.update {
                                    it.copy(backup = it.backup.copy(
                                        exportProgress = current.toFloat() / total,
                                        exportTotal = total,
                                    ))
                                }
                            },
                        )
                    } ?: error("Could not open export file.")
                }
            }
            _uiState.update {
                it.copy(
                    backup = it.backup.copy(
                        exporting = false,
                        message = if (result.isSuccess) "Backup exported." else null,
                        error = result.exceptionOrNull()?.message,
                    ),
                    toastMessage = if (result.isSuccess) "Backup exported successfully" else result.exceptionOrNull()?.message,
                )
            }
            if (_uiState.value.toastMessage != null) {
                delay(UiTimings.TOAST_DISMISS_MS)
                _uiState.update { it.copy(toastMessage = null) }
            }
        }
    }

    fun handleBackupImportResult(uri: Uri?) {
        uri ?: return
        _uiState.update { it.copy(backup = it.backup.copy(importing = true, message = null, error = null)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBytes()
                        if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                            // ZIP file (PK header)
                            localStore.restoreBackupZip(java.io.ByteArrayInputStream(bytes))
                        } else {
                            localStore.parseBackupJson(String(bytes, Charsets.UTF_8))
                        }
                    } ?: error("Could not open backup file.")
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
                    val savedMode = pendingBackupImportMode
                    viewModelScope.launch(Dispatchers.IO) {
                        localStore.saveSettings(importedState.settings)
                        if (savedMode == BackupImportMode.Merge) {
                            val existing = localStore.loadCameraLabels()
                            localStore.saveCameraLabels(existing + backup.cameraLabels)
                            val existingModels = localStore.loadCameraModels()
                            localStore.saveCameraModels(existingModels + backup.cameraModels)
                            val existingFirmwares = localStore.loadCameraFirmwares()
                            localStore.saveCameraFirmwares(existingFirmwares + backup.cameraFirmwares)
                        } else {
                            localStore.saveCameraLabels(backup.cameraLabels)
                            localStore.saveCameraModels(backup.cameraModels)
                            localStore.saveCameraFirmwares(backup.cameraFirmwares)
                        }
                        if (backup.slotBackupSets.isNotEmpty()) {
                            val existing = if (savedMode == BackupImportMode.Merge) localStore.loadSlotBackupSets() else emptyList()
                            val merged = existing + backup.slotBackupSets.filter { incoming ->
                                existing.none { it.meta.id == incoming.meta.id }
                            }
                            merged.forEach { localStore.saveSlotBackupSet(it.meta, it.slots) }
                        }
                        _events.emit(MainViewModelEvent.BackupImported)
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

    // ── Profile/Camera actions (delegated to CameraViewModel) ──────

    // ── Discover → Library ────────────────────────────────────────────

    suspend fun handleSaveFromDiscover(recipe: FxwRecipe, name: String, includePhotos: Boolean = true) {
        libraryHolder.saveFromDiscover(recipe, name, includePhotos)
    }

    // ── Persistence ───────────────────────────────────────────────────

    private fun loadPersistedState() {
        libraryHolder.load()
        viewModelScope.launch {
            val settings = withContext(Dispatchers.IO) { localStore.loadSettings() }
            libraryHolder.maxReferenceImages = settings.maxReferenceImages
            _uiState.update { it.copy(settings = settings) }
        }
    }

    private fun persistSettings() {
        val settings = _uiState.value.settings
        viewModelScope.launch(Dispatchers.IO) { localStore.saveSettings(settings) }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun List<String>.appendReferenceImages(uris: List<String>): List<String> =
        (this + uris).distinct().take(_uiState.value.settings.maxReferenceImages)

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
