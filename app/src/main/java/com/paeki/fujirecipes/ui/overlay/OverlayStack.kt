package com.paeki.fujirecipes.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * One dismissible layer in an overlay stack. Pass layers in ascending priority order
 * (index 0 = lowest; last entry = topmost, dismissed first on back).
 */
data class OverlayLayer(val active: Boolean, val dismiss: () -> Unit)

/**
 * Ordered stack of overlay layers with a single back-dispatch point.
 * Eliminates the hand-maintained `when` priority chain and implicit ordering
 * of multiple separate BackHandler calls.
 */
class OverlayStack(private val layers: List<OverlayLayer>) {
    val isAnyActive: Boolean get() = layers.any { it.active }

    /** Dismisses the highest-priority (last) active layer. */
    fun dismissTop() {
        layers.lastOrNull { it.active }?.dismiss?.invoke()
    }
}

/** Build a stack from vararg layers (left = lowest priority, right = highest). */
fun overlayStackOf(vararg layers: OverlayLayer) = OverlayStack(layers.toList())

/** Installs a single BackHandler wired to this stack's dispatch. */
@Composable
fun OverlayStack.BackHandler() {
    BackHandler(enabled = isAnyActive, onBack = ::dismissTop)
}
