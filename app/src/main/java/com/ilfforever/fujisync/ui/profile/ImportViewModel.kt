package com.ilfforever.fujisync.ui.profile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.ilfforever.fujisync.data.exif.FujiExifReader
import com.ilfforever.fujisync.data.exif.toPreset
import com.ilfforever.fujisync.data.local.LocalStore
import com.ilfforever.fujisync.data.ocr.OcrParseResult
import com.ilfforever.fujisync.data.ocr.OcrRecipeParser
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.ui.library.LibraryStateHolder
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.SmartRefResult
import com.ilfforever.fujisync.ui.model.toPreset
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

sealed class ImportEvent {
    object LaunchExifImagePicker : ImportEvent()
    object LaunchOcrImagePicker : ImportEvent()
    object LaunchQrImagePicker : ImportEvent()
    object LaunchShutterCheckPicker : ImportEvent()
    object LaunchSmartRefPicker : ImportEvent()
}

data class ImportUiState(
    val exifLoading: Boolean = false,
    val exifError: String? = null,
    val shutterCount: Int? = null,
    val shutterCheckLoading: Boolean = false,
    val shutterCheckError: String? = null,
    val ocrLoading: Boolean = false,
    val ocrError: String? = null,
    val ocrRawText: String? = null,
    val ocrParseResult: OcrParseResult? = null,
    val qrLoading: Boolean = false,
    val qrError: String? = null,
    val smartRefLoading: Boolean = false,
    val smartRefError: String? = null,
    val smartRefPendingUri: Uri? = null,
    val smartRefPendingRecipe: RecipeUiModel? = null,
    val smartRefResult: SmartRefResult? = null,
)

/** Result of a successful import — MainViewModel will open the editor with this. */
data class ImportedRecipe(val recipe: RecipeUiModel, val referenceImageUris: List<String> = emptyList())

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val localStore: LocalStore,
    private val libraryHolder: LibraryStateHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ImportEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ImportEvent> = _events.asSharedFlow()

    /** Emitted when an import produces a recipe ready for the editor. */
    private val _importedRecipe = MutableSharedFlow<ImportedRecipe>(extraBufferCapacity = 1)
    val importedRecipe: SharedFlow<ImportedRecipe> = _importedRecipe.asSharedFlow()

    /** Emitted for short-lived toast messages. */
    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    var smartRefSimilarityPct: Int = 72

    // ── EXIF import ───────────────────────────────────────────────────

    fun launchExifImport() {
        viewModelScope.launch { _events.emit(ImportEvent.LaunchExifImagePicker) }
    }

    fun handleExifImportResult(uri: Uri) {
        _state.update { it.copy(exifLoading = true, exifError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            val recipe = runCatching { FujiExifReader.readRecipe(appContext, uri) }
            delay(minLoadMs(started))
            when {
                recipe.isFailure -> _state.update {
                    it.copy(exifLoading = false, exifError = "Couldn't read this file. Make sure it's an unedited JPEG from a Fujifilm camera.")
                }
                recipe.getOrNull() == null -> _state.update {
                    it.copy(exifLoading = false, exifError = "No Fujifilm recipe found in this file. Use an original JPEG shot on an X-series camera.")
                }
                else -> {
                    val exif = recipe.getOrNull()!!
                    val localImageUri = runCatching { localStore.copyReferenceImage(uri) }.getOrNull()
                    val imageName = imageDisplayName(uri)
                    val uiModel = exif.toPreset("New Recipe").toUiModel().copy(
                        slot = "",
                        description = "Scanned from $imageName",
                        referenceImageUris = listOfNotNull(localImageUri),
                    )
                    _state.update { it.copy(exifLoading = false, exifError = null) }
                    _importedRecipe.tryEmit(ImportedRecipe(uiModel, uiModel.referenceImageUris))
                }
            }
        }
    }

    fun dismissExifImport() {
        _state.update { it.copy(exifLoading = false, exifError = null) }
    }

    // ── Shutter count ─────────────────────────────────────────────────

    fun launchShutterCheck() {
        _state.update { it.copy(shutterCheckError = null, shutterCount = null) }
        viewModelScope.launch { _events.emit(ImportEvent.LaunchShutterCheckPicker) }
    }

    fun handleShutterCheckResult(uri: Uri) {
        _state.update { it.copy(shutterCheckLoading = true, shutterCheckError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            val count = runCatching { FujiExifReader.readShutterCount(appContext, uri) }
            delay(minLoadMs(started))
            val n = count.getOrNull()
            _state.update {
                if (n == null) it.copy(shutterCheckLoading = false, shutterCheckError = "No shutter count found. Use an unedited JPEG taken on a Fujifilm X-series camera.")
                else it.copy(shutterCheckLoading = false, shutterCount = n)
            }
        }
    }

    fun dismissShutterCheck() {
        _state.update { it.copy(shutterCount = null, shutterCheckLoading = false, shutterCheckError = null) }
    }

    // ── Smart Reference ───────────────────────────────────────────────

    fun launchSmartRef() {
        viewModelScope.launch { _events.emit(ImportEvent.LaunchSmartRefPicker) }
    }

    fun handleSmartRefResult(uri: Uri) {
        _state.update { it.copy(smartRefLoading = true, smartRefError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            val recipe = runCatching { FujiExifReader.readRecipe(appContext, uri) }
            delay(minLoadMs(started))
            when {
                recipe.isFailure || recipe.getOrNull() == null -> _state.update {
                    it.copy(smartRefLoading = false, smartRefError = "Couldn't read recipe data from this file. Use an unedited JPEG from a Fujifilm X-series camera.")
                }
                else -> {
                    val exif = recipe.getOrNull()!!
                    val uiModel = exif.toPreset("").toUiModel().copy(slot = "")
                    val threshold = smartRefSimilarityPct / 100f
                    val match = libraryHolder.findDuplicate(uiModel, threshold)
                    if (match == null) {
                        _state.update {
                            it.copy(
                                smartRefLoading = false,
                                smartRefError = "No matching recipe found in your library. The settings in this photo don't match any saved recipe.",
                                smartRefPendingUri = uri,
                                smartRefPendingRecipe = uiModel,
                            )
                        }
                        return@launch
                    }
                    val existingRefUris = libraryHolder.referenceUrisFor(match.libraryRecipe.id)
                    val localImageUri = runCatching { localStore.copyReferenceImage(uri) }.getOrNull()
                    if (localImageUri == null) {
                        _state.update { it.copy(smartRefLoading = false, smartRefError = "Couldn't copy the image. Please try again.") }
                        return@launch
                    }
                    val isAlreadyRef = localStore.isExistingReferenceImage(Uri.parse(localImageUri), existingRefUris)
                    _state.update {
                        it.copy(
                            smartRefLoading = false,
                            smartRefResult = SmartRefResult(
                                matchedRecipe = match.libraryRecipe,
                                matchKind = match.kind,
                                localImageUri = localImageUri,
                                isAlreadyRef = isAlreadyRef,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun handleSmartRefConfirm() {
        val result = _state.value.smartRefResult ?: return
        _state.update { it.copy(smartRefResult = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val existing = libraryHolder.referenceUrisFor(result.matchedRecipe.id)
            val uriToUse = localStore.deduplicateReferenceImage(result.localImageUri, existing)
            libraryHolder.appendReferenceImage(result.matchedRecipe.id, uriToUse)
            _toast.tryEmit("Image added to ${result.matchedRecipe.name}")
        }
    }

    fun handleSmartRefConfirmAndContinue() {
        val result = _state.value.smartRefResult ?: return
        _state.update { it.copy(smartRefResult = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val existing = libraryHolder.referenceUrisFor(result.matchedRecipe.id)
            val uriToUse = localStore.deduplicateReferenceImage(result.localImageUri, existing)
            libraryHolder.appendReferenceImage(result.matchedRecipe.id, uriToUse)
            _toast.tryEmit("Image added to ${result.matchedRecipe.name}")
        }
        viewModelScope.launch { _events.emit(ImportEvent.LaunchSmartRefPicker) }
    }

    fun handleSmartRefDismissAndContinue() {
        val result = _state.value.smartRefResult
        if (result != null) {
            viewModelScope.launch(Dispatchers.IO) { runCatching { localStore.deleteReferenceImageFile(result.localImageUri) } }
        }
        _state.update { it.copy(smartRefLoading = false, smartRefError = null, smartRefPendingUri = null, smartRefPendingRecipe = null, smartRefResult = null) }
        viewModelScope.launch { _events.emit(ImportEvent.LaunchSmartRefPicker) }
    }

    fun handleSmartRefDismiss() {
        val result = _state.value.smartRefResult
        if (result != null) {
            viewModelScope.launch(Dispatchers.IO) { runCatching { localStore.deleteReferenceImageFile(result.localImageUri) } }
        }
        _state.update { it.copy(smartRefLoading = false, smartRefError = null, smartRefPendingUri = null, smartRefPendingRecipe = null, smartRefResult = null) }
    }

    fun handleSmartRefCreateNew() {
        val uri = _state.value.smartRefPendingUri ?: return
        val pendingRecipe = _state.value.smartRefPendingRecipe
        _state.update { it.copy(smartRefError = null, smartRefPendingUri = null, smartRefPendingRecipe = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val localUri = runCatching { localStore.copyReferenceImage(uri) }.getOrNull()
            val uris = if (localUri != null) listOf(localUri.toString()) else emptyList()
            val draft = pendingRecipe?.copy(libraryId = null, slot = "")
                ?: RecipeUiModel(slot = "", name = "", sim = "", pills = emptyList())
            _importedRecipe.tryEmit(ImportedRecipe(draft, uris))
        }
    }

    // ── OCR / screenshot import ───────────────────────────────────────

    fun launchOcrImport() {
        viewModelScope.launch { _events.emit(ImportEvent.LaunchOcrImagePicker) }
    }

    fun handleOcrImportResult(uri: Uri) {
        _state.update { it.copy(ocrLoading = true, ocrError = null) }
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
                result.isFailure -> _state.update {
                    it.copy(ocrLoading = false, ocrError = "Couldn't read this image. Make sure it's a clear screenshot.", ocrRawText = capturedRaw, ocrParseResult = null)
                }
                result.getOrNull() == null -> _state.update {
                    it.copy(ocrLoading = false, ocrError = "No recipe settings found in this screenshot. Try a clearer image with visible parameter labels.", ocrRawText = capturedRaw, ocrParseResult = null)
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
                    val preliminary = RecipeUiModel(
                        slot = "C1", name = name, sim = parsed.sim, pills = emptyList(),
                        effects = parsed.effects, tone = parsed.tone, wb = parsed.wb,
                    )
                    val uiModel = preliminary.toPreset(CameraSlot.C1).toUiModel()
                        .copy(slot = "", name = name, description = description)
                    _state.update { it.copy(ocrLoading = false, ocrError = null, ocrRawText = capturedRaw, ocrParseResult = parsed) }
                    _importedRecipe.tryEmit(ImportedRecipe(uiModel))
                }
            }
        }
    }

    fun dismissOcrImport() {
        _state.update { it.copy(ocrLoading = false, ocrError = null, ocrRawText = null, ocrParseResult = null) }
    }

    // ── QR import ─────────────────────────────────────────────────────

    fun launchQrImageImport() {
        viewModelScope.launch { _events.emit(ImportEvent.LaunchQrImagePicker) }
    }

    fun handleQrImportResult(uri: Uri) {
        _state.update { it.copy(qrLoading = true, qrError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            val recipe = runCatching { com.ilfforever.fujisync.data.qr.RecipeQr.decodeBitmap(appContext, uri) }.getOrNull()
            delay(minLoadMs(started, 700))
            if (recipe == null) {
                _state.update { it.copy(qrLoading = false, qrError = "No FujiSync recipe QR found. Make sure the QR code is sharp and fully visible.") }
            } else {
                handleQrImportRecipe(recipe)
            }
        }
    }

    fun handleQrImportRecipe(recipe: RecipeUiModel) {
        _state.update { it.copy(qrLoading = false, qrError = null) }
        _importedRecipe.tryEmit(ImportedRecipe(recipe.copy(libraryId = null, slot = "", referenceImageUris = emptyList())))
    }

    fun dismissQrImport() {
        _state.update { it.copy(qrLoading = false, qrError = null) }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun minLoadMs(startedAt: Long, minMs: Long = 1400): Long =
        (minMs - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0L)

    private fun imageDisplayName(uri: Uri): String {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.trim()?.takeIf { it.isNotBlank() } ?: "image"
    }
}
