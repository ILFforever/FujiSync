package com.ilfforever.fujisync.ui.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Comfortable reading measure for body text — roughly 60–75 characters per line. */
val ReadableWidth: Dp = 640.dp

/** Wider cap for screens that are mostly rows and cards rather than prose. */
val WideContentWidth: Dp = 840.dp

/** Width of the list column when the Library runs as list-detail on a tablet. */
val LibraryPaneWidth: Dp = 400.dp

/**
 * Caps a column at [max] and centres it in the available space.
 *
 * On a phone this is a no-op — the screen is narrower than the cap, so the content still
 * fills it. On a tablet it stops rows stretching to 1280dp, which is what turns a
 * label-left / value-right row into a label and a value separated by a hand-span of
 * nothing. The dark background still runs edge to edge; only the content is constrained.
 */
fun Modifier.contentWidth(max: Dp = ReadableWidth): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = max)

/**
 * [contentWidth] that only engages on tablets, for screens where a phone-width column
 * would look stranded in a Medium window.
 *
 * A `@Composable` factory rather than `Modifier.composed`: a composed modifier can't be
 * compared for equality, so the layout node re-materializes its whole chain on every
 * recomposition of the caller. These sit on screen roots, so that cost lands on the
 * entire subtree. [LocalWindowWidthClass] is a static local, so the read is free and
 * this needs no composition group of its own.
 */
@Composable
@ReadOnlyComposable
fun Modifier.tabletContentWidth(max: Dp = ReadableWidth): Modifier =
    if (LocalWindowWidthClass.current.isTablet) contentWidth(max) else this

/** Horizontal page padding — roomier on tablets so content isn't flush to the bezel. */
@Composable
@ReadOnlyComposable
fun pagePadding(): Dp = if (LocalWindowWidthClass.current.isTablet) 32.dp else 20.dp

/**
 * Caps a bottom sheet or dialog on tablets. The sheets are aligned centre-bottom by their
 * parent, so constraining the width is enough to centre them — a full-bleed drawer across
 * 1280dp reads as a second screen rather than a sheet.
 */
@Composable
@ReadOnlyComposable
fun Modifier.sheetWidth(max: Dp = 560.dp): Modifier =
    if (LocalWindowWidthClass.current.isTablet) widthIn(max = max) else this
