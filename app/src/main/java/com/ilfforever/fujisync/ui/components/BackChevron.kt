package com.ilfforever.fujisync.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilfforever.fujisync.ui.theme.Gold

/**
 * Standard back affordance for sub-screen headers.
 *
 * Deliberately a vector rather than the `‹` text glyph these headers used to draw. A glyph is
 * positioned by the font's ascent/descent, which reserves empty space below the mark, so
 * `Alignment.CenterVertically` centred the text box and left the mark sitting low against the
 * title beside it. An [Icon] centres on its own 24×24 viewport, so alignment no longer depends
 * on font metrics.
 */
@Composable
fun BackChevron(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = IconChevronLeft,
        contentDescription = "Back",
        tint = Gold,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
            .size(22.dp),
    )
}
