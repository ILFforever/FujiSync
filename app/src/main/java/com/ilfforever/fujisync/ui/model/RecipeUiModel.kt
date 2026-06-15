package com.ilfforever.fujisync.ui.model

data class RecipeUiModel(
    val libraryId: String? = null,
    val slot: String,
    val name: String,
    val sim: String,
    val pills: List<String>,
    val description: String = "",
    val effects: Map<String, String> = emptyMap(),
    val tone: Map<String, String> = emptyMap(),
    val wb: Map<String, String> = emptyMap(),
    val saved: String? = null,
    val sourceCameraName: String? = null,
    val sourceCameraModel: String? = null,
    val sourceUsbId: String? = null,
    val sourceUrl: String? = null,
    val sourceLabel: String? = null,
    val referenceImageUris: List<String> = emptyList(),
    val groupIds: List<String> = emptyList(),
    val group: String? = null,
    val favorite: Boolean = false,
    val isoMin: Int? = null,
    val isoMax: Int? = null,
    val exposureCompMin: Float? = null,
    val exposureCompMax: Float? = null,
    val sensorGens: List<Int> = emptyList(),
) {
    val groupId: String? get() = groupIds.firstOrNull()
}

data class LibraryGroupUiModel(
    val id: String,
    val name: String,
)

data class LibraryRecipeUiModel(
    val id: String,
    val name: String,
    val sim: String,
    val pills: List<String>,
    val saved: String,
    val description: String = "",
    val effects: Map<String, String> = emptyMap(),
    val tone: Map<String, String> = emptyMap(),
    val wb: Map<String, String> = emptyMap(),
    val sourceCameraName: String? = null,
    val sourceCameraModel: String? = null,
    val sourceUsbId: String? = null,
    val sourceUrl: String? = null,
    val sourceLabel: String? = null,
    val referenceImageUris: List<String> = emptyList(),
    val groupIds: List<String> = emptyList(),
    val favorite: Boolean = false,
    val isoMin: Int? = null,
    val isoMax: Int? = null,
    val exposureCompMin: Float? = null,
    val exposureCompMax: Float? = null,
    val sensorGens: List<Int> = emptyList(),
) {
    val groupId: String? get() = groupIds.firstOrNull()
}

data class LibraryRecipeSource(
    val cameraName: String,
    val cameraModel: String,
    val usbId: String,
)

data class LibraryGroupStyle(
    val imageUri: String? = null,
    val icon: String = "📁",
    val color: String = "Gold",
)

fun RecipeUiModel.sourceCameraDisplayName(): String? =
    preferredSourceCameraDisplayName(sourceCameraName, sourceCameraModel)

fun LibraryRecipeUiModel.sourceCameraDisplayName(): String? =
    preferredSourceCameraDisplayName(sourceCameraName, sourceCameraModel)

private fun preferredSourceCameraDisplayName(name: String?, model: String?): String? {
    val cleanName = name?.trim()?.takeIf { it.isNotBlank() && !it.equals("My Camera", ignoreCase = true) }
    val cleanModel = model?.trim()?.takeIf { it.isNotBlank() }
    return cleanName
        ?.takeIf { cleanModel == null || !it.equals(cleanModel, ignoreCase = true) }
        ?: cleanModel
        ?: cleanName
}
