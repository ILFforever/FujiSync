package com.ilfforever.fujisync.ui.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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
 */
fun Modifier.tabletContentWidth(max: Dp = ReadableWidth): Modifier = composed {
    val widthClass = LocalWindowWidthClass.current
    if (widthClass.isTablet) contentWidth(max) else this@tabletContentWidth
}

/** Horizontal page padding — roomier on tablets so content isn't flush to the bezel. */
@Composable
fun pagePadding(): Dp = if (LocalWindowWidthClass.current.isTablet) 32.dp else 20.dp

/**
 * Caps a bottom sheet or dialog on tablets. The sheets are aligned centre-bottom by their
 * parent, so constraining the width is enough to centre them — a full-bleed drawer across
 * 1280dp reads as a second screen rather than a sheet.
 */
fun Modifier.sheetWidth(max: Dp = 560.dp): Modifier = composed {
    if (LocalWindowWidthClass.current.isTablet) widthIn(max = max) else this@sheetWidth
}
