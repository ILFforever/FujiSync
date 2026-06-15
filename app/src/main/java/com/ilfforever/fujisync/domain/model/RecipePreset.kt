package com.ilfforever.fujirecipes.domain.model

data class RecipePreset(
    val slot: CameraSlot,
    val name: String,
    val properties: Map<FujiPropertyCode, Int> = emptyMap(),
)
