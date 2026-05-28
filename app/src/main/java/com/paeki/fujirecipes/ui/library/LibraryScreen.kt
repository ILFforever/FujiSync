package com.paeki.fujirecipes.ui.library

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.paeki.fujirecipes.ui.components.IconFilter
import com.paeki.fujirecipes.ui.components.IconPlus
import com.paeki.fujirecipes.ui.components.IconSearch
import com.paeki.fujirecipes.ui.components.IconSort
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.components.Pill
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    recipes: List<LibraryRecipeUiModel>,
    sortBy: String,
    onToggleSort: () -> Unit,
    onOpenItem: (LibraryRecipeUiModel) -> Unit,
) {
    val sorted = if (sortBy == "NAME") {
        recipes.sortedBy { it.name }
    } else {
        recipes
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "LIBRARY",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(IconSearch, contentDescription = "Search", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(IconFilter, contentDescription = "Filter", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Gold)
                        .clickable(onClick = {}),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(IconPlus, contentDescription = "Add", tint = Bg, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Count + sort row
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${recipes.size} RECIPES",
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    color = TextMuted,
                )
                Row(
                    modifier = Modifier.clickable(onClick = onToggleSort).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(IconSort, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                    Text(
                        text = sortBy,
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.8.sp,
                        color = TextPrimary,
                    )
                }
            }
            BottomDivider()
        }

        // Recipe list
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sorted, key = { it.id }) { recipe ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenItem(recipe) },
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, Gold, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = recipe.sim.uppercase(),
                                    fontFamily = MonoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 1.4.sp,
                                    color = Gold,
                                )
                            }
                            Text(
                                text = "SAVED ${recipe.saved.uppercase()}",
                                fontFamily = MonoFamily,
                                fontSize = 10.5.sp,
                                letterSpacing = 1.sp,
                                color = TextDim,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = recipe.name,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = 0.1.sp,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            recipe.pills.forEach { Pill(text = it) }
                        }
                    }
                    BottomDivider()
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BottomDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}
