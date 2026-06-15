package com.ilfforever.fujirecipes.ui.model

data class AppSettings(
    val showLibraryImages: Boolean = true,
    val showCardImageCount: Boolean = false,
    val showReferenceImageBlur: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val favoritesOnTop: Boolean = false,
    val propertyWriteDelayMs: Long = 0L,
    val smartRefSimilarityPct: Int = 65,
    val maxReferenceImages: Int = 20,
)
