package com.ilfforever.fujisync.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.library.LibraryPhotoFilter
import com.ilfforever.fujisync.ui.library.LibraryUngroupedFilterId
import com.ilfforever.fujisync.ui.model.LibraryGroupUiModel
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun LibraryFilterDialog(
    sortBy: String,
    onChangeSort: (String) -> Unit,
    filterFavorites: Boolean,
    onToggleFilterFavorites: () -> Unit,
    photoFilter: LibraryPhotoFilter,
    onChangePhotoFilter: (LibraryPhotoFilter) -> Unit,
    groups: List<LibraryGroupUiModel>,
    groupCounts: Map<String, Int>,
    selectedGroupId: String?,
    onSelectGroup: (String?) -> Unit,
    sourceOptions: List<Pair<String, String>>,
    selectedSourceKey: String?,
    onSelectSource: (String?) -> Unit,
    filmSimOptions: List<String>,
    selectedFilmSim: String?,
    onSelectFilmSim: (String?) -> Unit,
    activeFilterCount: Int,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PanelGroupBg)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                text = "FILTER",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                color = TextMuted,
            )
            Spacer(Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterOptionRow(
                    label = "Favorite",
                    active = filterFavorites,
                    onClick = onToggleFilterFavorites,
                )
                FilterOptionRow(
                    label = "With photos",
                    active = photoFilter == LibraryPhotoFilter.WithPhotos,
                    onClick = {
                        onChangePhotoFilter(
                            if (photoFilter == LibraryPhotoFilter.WithPhotos) LibraryPhotoFilter.All else LibraryPhotoFilter.WithPhotos,
                        )
                    },
                )
                FilterOptionRow(
                    label = "No photos",
                    active = photoFilter == LibraryPhotoFilter.NoPhotos,
                    onClick = {
                        onChangePhotoFilter(
                            if (photoFilter == LibraryPhotoFilter.NoPhotos) LibraryPhotoFilter.All else LibraryPhotoFilter.NoPhotos,
                        )
                    },
                )
                FilterOptionRow(
                    label = "Ungrouped",
                    active = selectedGroupId == LibraryUngroupedFilterId,
                    onClick = {
                        onSelectGroup(if (selectedGroupId == LibraryUngroupedFilterId) null else LibraryUngroupedFilterId)
                    },
                )
            }

            if (groups.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                FilterSectionTitle("GROUP")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { group ->
                        FilterOptionRow(
                            label = "${group.name} ${groupCounts[group.id] ?: 0}",
                            active = selectedGroupId == group.id,
                            onClick = { onSelectGroup(if (selectedGroupId == group.id) null else group.id) },
                        )
                    }
                }
            }

            if (filmSimOptions.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                FilterSectionTitle("FILM SIM")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filmSimOptions.forEach { sim ->
                        FilterOptionRow(
                            label = sim,
                            active = selectedFilmSim == sim,
                            onClick = { onSelectFilmSim(if (selectedFilmSim == sim) null else sim) },
                        )
                    }
                }
            }

            if (sourceOptions.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                FilterSectionTitle("SOURCE")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sourceOptions.forEach { (key, label) ->
                        FilterOptionRow(
                            label = label,
                            active = selectedSourceKey == key,
                            onClick = { onSelectSource(if (selectedSourceKey == key) null else key) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            Spacer(Modifier.height(16.dp))

            FilterSectionTitle("SORT")
            SortTabRow(sortBy = sortBy, onChangeSort = onChangeSort)
            if (activeFilterCount > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "CLEAR FILTERS",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    color = Gold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClearFilters)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
internal fun FilterSectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = MonoFamily,
        fontSize = 9.sp,
        letterSpacing = 1.3.sp,
        color = TextDim,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun FilterOptionRow(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    Text(
        text = label.uppercase(),
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.1.sp,
        color = if (active) Gold else TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Gold.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (active) Gold.copy(alpha = 0.5f) else Border, RoundedCornerShape(8.dp))
            .clickable { FujiHaptics.perform(context, view, FujiHapticEffect.SoftSelection); onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}


@Composable
internal fun SortTabRow(sortBy: String, onChangeSort: (String) -> Unit) {
    val dateActive = sortBy == "NEWEST" || sortBy == "OLDEST"
    val nameActive = sortBy == "NAME_ASC" || sortBy == "NAME_DESC"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp)),
    ) {
        SortTabItem(
            label = "DATE",
            active = dateActive,
            direction = if (sortBy == "OLDEST") "↑" else if (dateActive) "↓" else null,
            modifier = Modifier.weight(1f),
            onClick = {
                onChangeSort(if (sortBy == "NEWEST") "OLDEST" else "NEWEST")
            },
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(Border))
        SortTabItem(
            label = "NAME",
            active = nameActive,
            direction = if (sortBy == "NAME_DESC") "↓" else if (nameActive) "↑" else null,
            modifier = Modifier.weight(1f),
            onClick = {
                onChangeSort(if (sortBy == "NAME_ASC") "NAME_DESC" else "NAME_ASC")
            },
        )
    }
}

@Composable
internal fun SortTabItem(
    label: String,
    active: Boolean,
    direction: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (active) Gold.copy(alpha = 0.14f) else Color.Transparent)
            .clickable { FujiHaptics.perform(context, view, FujiHapticEffect.SoftSelection); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp,
                color = if (active) Gold else TextPrimary,
            )
            if (direction != null) {
                Text(
                    text = direction,
                    fontSize = 12.sp,
                    color = Gold,
                )
            }
        }
    }
}
