package com.ilfforever.fujisync.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.editor.ControlLabel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldDim
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted

@Composable
internal fun ChipControl(
    label: String,
    icon: ImageVector,
    options: List<String>,
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    ControlLabel(label = label, icon = icon, enabled = enabled)
    ChipGrid(options = options, selected = selected, enabled = enabled, onSelect = onSelect)
}

/**
 * A row of choices. Camera-agnostic by design: a recipe is authored independently of any body, so
 * every value stays offered here and compatibility is settled at push time.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipGrid(
    options: List<String>,
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            !enabled && active -> GoldDim.copy(alpha = 0.28f)
                            !enabled -> PanelLow
                            active -> Gold
                            else -> PanelHigh
                        },
                    )
                    .border(
                        1.dp,
                        when {
                            !enabled -> Color.Transparent
                            active -> Gold
                            else -> Border
                        },
                        RoundedCornerShape(8.dp),
                    )
                    .clickable(enabled = enabled) { onSelect(option) }
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    fontFamily = SansFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = when {
                        !enabled && active -> Gold.copy(alpha = 0.82f)
                        !enabled -> TextDim
                        active -> Bg
                        else -> TextMuted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
