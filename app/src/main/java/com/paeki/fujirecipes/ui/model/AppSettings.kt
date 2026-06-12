package com.paeki.fujirecipes.ui.model

data class AppSettings(
    val showLibraryImages: Boolean = true,
    val showReferenceImageBlur: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val favoritesOnTop: Boolean = false,
    val propertyWriteDelayMs: Long = 0L,
)
