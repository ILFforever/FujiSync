package com.ilfforever.fujisync.ui

import com.ilfforever.fujisync.ui.model.AppSettings
import com.ilfforever.fujisync.ui.model.DuplicateDialogState
import com.ilfforever.fujisync.ui.model.LibraryGroupStyle
import com.ilfforever.fujisync.ui.model.LibraryGroupUiModel
import com.ilfforever.fujisync.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.SaveAllReport
import com.ilfforever.fujisync.ui.model.SlotBackupMeta
import com.ilfforever.fujisync.ui.model.SlotBackupSet

enum class AppTab { Camera, Library, Discover, Profile }

/**
 * Typed representation of the currently active screen. Derived from [FujiSyncUiState];
 * source of truth for back-stack behavior and overlay ordering.
 */
sealed class Screen {
    data class Tab(val tab: AppTab) : Screen()
    data class RecipeDetail(val recipe: RecipeUiModel) : Screen()
    /** [recipe] is null when creating a new recipe. */
    data class RecipeEditor(val recipe: RecipeUiModel?) : Screen()
}

val FujiSyncUiState.currentScreen: Screen
    get() = when {
        creatingRecipe || editorRecipe != null -> Screen.RecipeEditor(editorRecipe)
        detailRecipe != null -> Screen.RecipeDetail(detailRecipe)
        else -> Screen.Tab(tab)
    }

data class CameraUiState(
    val connected: Boolean = false,
    val scanning: Boolean = false,
    val scanError: String? = null,
    val readingSlots: Boolean = false,
    val readingSlotIndex: Int = -1,
    val cameraModel: String = "",
    val cameraSerial: String = "",
    val firmware: String = "",
    val battery: String = "",
    val slots: List<RecipeUiModel> = emptyList(),
    val selectedSlotIdx: Int = 0,
    val backingUpSlots: Boolean = false,
    val backingUpSlotIndex: Int = -1,
    val hasSlotBackup: Boolean = false,
    val slotBackupMeta: SlotBackupMeta? = null,
    val slotBackupSlots: List<RecipeUiModel>? = null,
    val slotBackupSets: List<SlotBackupSet> = emptyList(),
    val restoringSlots: Boolean = false,
    val restoringSlotIndex: Int = -1,
    val rearrangingSlots: Boolean = false,
    val rearrangingSlotIndex: Int = -1,
    val rearrangingWriteIndex: Int = -1,
    val rearrangingWriteTotal: Int = 0,
    val isRestoringValidation: Boolean = false,
    val isRearrangeValidation: Boolean = false,
    val cameraLabels: Map<String, String> = emptyMap(),
    val cameraModels: Map<String, String> = emptyMap(),
    val cameraFirmwares: Map<String, String> = emptyMap(),
    val showImageTuner: Boolean = false,
    val writeDelayMs: Long = 0L,
)

data class LibraryUiState(
    val recipes: List<LibraryRecipeUiModel> = emptyList(),
    val groups: List<LibraryGroupUiModel> = emptyList(),
    val sort: String = "NEWEST",
    val groupedView: Boolean = false,
    val groupStyles: Map<String, LibraryGroupStyle> = emptyMap(),
    val saveConfirmed: Boolean = false,
    val duplicateDialog: DuplicateDialogState? = null,
    val loadError: String? = null,
)

data class FujiSyncUiState(
    val tab: AppTab = AppTab.Camera,
    val detailRecipe: RecipeUiModel? = null,
    val editorRecipe: RecipeUiModel? = null,
    val creatingRecipe: Boolean = false,
    val editorReferenceImageUris: List<String> = emptyList(),
    val writeBusy: Boolean = false,
    val writeToast: WriteToastState? = null,
    val camera: CameraUiState = CameraUiState(),
    val library: LibraryUiState = LibraryUiState(),
    val settings: AppSettings = AppSettings(),
    val exifImportLoading: Boolean = false,
    val exifImportError: String? = null,
    val shutterCount: Int? = null,
    val shutterCheckLoading: Boolean = false,
    val shutterCheckError: String? = null,
    val ocrImportLoading: Boolean = false,
    val ocrImportError: String? = null,
    val ocrRawText: String? = null,
    val ocrParseResult: com.ilfforever.fujisync.data.ocr.OcrParseResult? = null,
    val qrImportLoading: Boolean = false,
    val qrImportError: String? = null,
    val saveAllSlotsConfirmed: Boolean = false,
    val saveAllReport: SaveAllReport? = null,
    val captureLog: String? = null,
    val update: UpdateUiState = UpdateUiState(),
    val backup: BackupUiState = BackupUiState(),
    val toastMessage: String? = null,
    val smartRefLoading: Boolean = false,
    val smartRefError: String? = null,
    val smartRefPendingUri: android.net.Uri? = null,
    val smartRefPendingRecipe: com.ilfforever.fujisync.ui.model.RecipeUiModel? = null,
    val smartRefResult: com.ilfforever.fujisync.ui.model.SmartRefResult? = null,
)

data class WriteToastState(val slot: String, val name: String, val savedToLibrary: Boolean = false)

data class UpdateUiState(
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val latestVersion: String? = null,
    val releaseName: String? = null,
    val assetName: String? = null,
    val updateAvailable: Boolean = false,
    val downloaded: Boolean = false,
    val installPermissionRequired: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

data class BackupUiState(
    val exporting: Boolean = false,
    val importing: Boolean = false,
    val exportProgress: Float = 0f,
    val exportTotal: Int = 0,
    val message: String? = null,
    val error: String? = null,
)
