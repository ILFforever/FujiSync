package com.ilfforever.fujisync.ui.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconFolder
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim

/**
 * Fills the Library's detail pane on tablets when nothing is selected. Without it the right
 * two-thirds of the screen is simply black, which reads as a rendering fault rather than an
 * empty state.
 */
@Composable
internal fun LibraryDetailPlaceholder() {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = IconFolder,
            contentDescription = null,
            tint = TextDim,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "NO RECIPE SELECTED",
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pick a recipe from the list to see its settings here.",
            fontFamily = SansFamily,
            fontSize = 13.sp,
            color = TextDim,
            textAlign = TextAlign.Center,
        )
    }
}
