package com.ilfforever.fujisync.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ilfforever.fujisync.data.remote.FxwRecipe
import com.ilfforever.fujisync.ui.components.PropRow
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.components.recipePropertyRows
import com.ilfforever.fujisync.ui.library.normalizedFxwParams
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.PanelLow

internal data class DiscoverParamSectionData(
    val label: String,
    val data: Map<String, String>,
)

internal fun FxwRecipe.discoverParamSections(overrideParams: Map<String, String>? = null): List<DiscoverParamSectionData> {
    val normalized = (overrideParams ?: params).normalizedFxwParams()
    val used = mutableSetOf<String>()

    fun section(label: String, keys: List<String>): DiscoverParamSectionData? {
        val data = buildMap {
            keys.forEach { key ->
                val value = normalized[key].orEmpty()
                if (value.isNotBlank()) {
                    put(key, value)
                    used += key
                }
            }
        }
        return data.takeIf { it.isNotEmpty() }?.let { DiscoverParamSectionData(label, it) }
    }

    return buildList {
        section(
            label = "Effects",
            keys = listOf("Dynamic Range", "D Range Priority", "Grain Effect", "Color Chrome", "Color Chrome FX Blue", "Smooth Skin"),
        )?.let(::add)
        section(
            label = "White Balance",
            keys = listOf("White Balance", "WB Shift R", "WB Shift B"),
        )?.let(::add)
        section(
            label = "Tone",
            keys = listOf("Highlight Tone", "Shadow Tone", "Color", "Sharpness", "High ISO NR", "Clarity"),
        )?.let(::add)

        val other = normalized
            .filterKeys { it != "Film Simulation" && it !in used }
            .filterValues { it.isNotBlank() }
        if (other.isNotEmpty()) add(DiscoverParamSectionData("Other", other))
    }
}

@Composable
internal fun DiscoverParamSection(label: String, data: Map<String, String>) {
    if (data.isEmpty()) return
    val entries = recipePropertyRows(data)
    if (entries.isEmpty()) return
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 0.dp)) {
        Spacer(Modifier.height(8.dp))
        SectionLabel(text = label, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(14.dp)),
        ) {
            entries.forEachIndexed { idx, (key, value) ->
                PropRow(
                    label = key,
                    value = value,
                    isLast = idx == entries.lastIndex,
                )
                if (idx < entries.lastIndex) Divider()
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
internal fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}
