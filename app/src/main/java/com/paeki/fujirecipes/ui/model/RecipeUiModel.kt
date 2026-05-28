package com.paeki.fujirecipes.ui.model

data class RecipeUiModel(
    val slot: String,
    val name: String,
    val sim: String,
    val pills: List<String>,
    val description: String = "",
    val effects: Map<String, String> = emptyMap(),
    val tone: Map<String, String> = emptyMap(),
    val wb: Map<String, String> = emptyMap(),
)

data class LibraryRecipeUiModel(
    val id: String,
    val name: String,
    val sim: String,
    val pills: List<String>,
    val saved: String,
)
