package com.ilfforever.fujisync.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ilfforever.fujisync.ui.model.LibraryGroupUiModel
import com.ilfforever.fujisync.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujisync.ui.theme.Border

internal val PanelGroupBg = Color(0xFF171512)
internal val LibrarySheetOverlay = Color(0xFF0A0A09)
internal val LibrarySheetBg = Color(0xFF11100E)
internal val LibrarySheetControlBg = Color(0xFF161411)
internal val LibrarySheetBorder = Color(0xFF26221C)

internal data class LibraryFolderGridItem(
    val group: LibraryGroupUiModel,
    val recipes: List<LibraryRecipeUiModel> = emptyList(),
    val count: Int = recipes.size,
)

internal fun String.isGeneratedGroupId(): Boolean =
    removePrefix("group-").matches(
        Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"),
    )

internal enum class GroupColor(val value: Color) {
    Gold(Color(0xFFC99A4E)),
    Brass(Color(0xFFB88746)),
    Amber(Color(0xFFD2A136)),
    Cream(Color(0xFFE9D9B8)),
    Clay(Color(0xFF9F6A4E)),
    Coral(Color(0xFFC56A55)),
    Rose(Color(0xFFB45C69)),
    Plum(Color(0xFF80607E)),
    Indigo(Color(0xFF5F6F96)),
    Blue(Color(0xFF5F7F98)),
    Teal(Color(0xFF5F8B87)),
    Green(Color(0xFF6F8B63)),
    Olive(Color(0xFF83855A)),
    Silver(Color(0xFFA8A49A)),
    Red(Color(0xFFA94B35)),
}

internal fun groupAccent(name: String): Color =
    GroupColor.entries.firstOrNull { it.name == name }?.value ?: GroupColor.Gold.value

@Composable
internal fun BottomDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}
