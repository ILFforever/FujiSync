package com.ilfforever.fujirecipes.ui.model

enum class DuplicateMatchKind { ExactSettings, SameName, Similar }

data class DuplicateMatch(
    val libraryRecipe: LibraryRecipeUiModel,
    val kind: DuplicateMatchKind,
)

data class DuplicateDialogState(
    val incomingRecipe: RecipeUiModel,
    val source: LibraryRecipeSource?,
    val topMatch: DuplicateMatch,
)

data class SmartRefResult(
    val matchedRecipe: LibraryRecipeUiModel,
    val matchKind: DuplicateMatchKind,
    val localImageUri: String,
    val isAlreadyRef: Boolean = false,
)


data class SaveAllSkipped(
    val slot: String,
    val name: String,
    val sim: String,
    val matchKind: DuplicateMatchKind,
    val matchedName: String,
)

data class SaveAllReport(
    val saved: Int,
    val skipped: List<SaveAllSkipped>,
) {
    val hasSkipped: Boolean get() = skipped.isNotEmpty()
}
