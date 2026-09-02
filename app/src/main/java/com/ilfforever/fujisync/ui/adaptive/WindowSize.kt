package com.ilfforever.fujisync.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Coarse width buckets the UI adapts to. Deliberately three values rather than a full
 * responsive system — the app only needs to know "phone", "small tablet", "wide tablet".
 *
 * Breakpoints follow the Material window size classes so they line up with what device
 * makers actually ship: a phone is ~360–430dp, a 10" tablet is ~800dp portrait and
 * ~1280dp landscape.
 */
enum class WindowWidthClass {
    /** Phones, and tablets in split-screen. Single column, bottom tab bar. */
    Compact,

    /** Small tablets and large foldables. Single column, but capped and centred. */
    Medium,

    /** 10"+ tablets. Navigation rail, two-pane where it earns its place. */
    Expanded;

    val isTablet: Boolean get() = this != Compact

    /** Only [Expanded] is wide enough for a list beside a detail pane. */
    val supportsTwoPane: Boolean get() = this == Expanded
}

val LocalWindowWidthClass = staticCompositionLocalOf { WindowWidthClass.Compact }

/**
 * Derives the current width class from the configuration. Recomputes on rotation and on
 * split-screen resize, because [LocalConfiguration] changes in both cases.
 */
@Composable
@ReadOnlyComposable
fun currentWindowWidthClass(): WindowWidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> WindowWidthClass.Compact
        widthDp < 840 -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }
}
