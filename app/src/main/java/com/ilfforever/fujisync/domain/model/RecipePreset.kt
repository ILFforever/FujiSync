package com.ilfforever.fujisync.domain.model

data class RecipePreset(
    val slot: CameraSlot,
    val name: String,
    val properties: Map<FujiPropertyCode, Int> = emptyMap(),
)
