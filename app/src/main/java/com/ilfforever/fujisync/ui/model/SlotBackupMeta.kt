package com.ilfforever.fujirecipes.ui.model

data class SlotBackupMeta(
    val label: String,
    val savedAt: String,
    val id: String = "",
)

data class SlotBackupSet(
    val meta: SlotBackupMeta,
    val slots: List<RecipeUiModel>,
)
