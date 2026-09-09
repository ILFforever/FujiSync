package com.ilfforever.fujisync.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.editor.filmSimFamilies
import com.ilfforever.fujisync.ui.editor.filmSimFamilyFor
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldDim
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted

/**
 * Film simulations, grouped by family.
 *
 * Every simulation is offered regardless of which camera is attached: a recipe is authored on its
 * own terms, and whether a given body accepts the value is settled when it is pushed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilmSimulationPicker(
    selected: String,
    selectedFamily: String,
    onFamilySelect: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    val family = filmSimFamilies.firstOrNull { it.label == selectedFamily } ?: filmSimFamilyFor(selected)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        filmSimFamilies.forEach { item ->
            val active = item.label == family.label
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) GoldDim else Color.Transparent)
                    .border(1.dp, if (active) Gold else Border, RoundedCornerShape(999.dp))
                    .clickable { onFamilySelect(item.label) }
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = item.label.uppercase(),
                    fontFamily = MonoFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp,
                    color = if (active) Gold else TextMuted,
                )
                Text(
                    text = item.sims.size.toString(),
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    color = if (active) Gold else TextDim,
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        family.sims.forEach { option ->
            val active = option == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Gold else PanelHigh)
                    .border(1.dp, if (active) Gold else Border, RoundedCornerShape(10.dp))
                    .clickable { onSelect(option) }
                    .padding(start = 10.dp, end = 13.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (active) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Bg),
                    )
                }
                Text(
                    text = option,
                    fontFamily = SansFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = if (active) Bg else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
