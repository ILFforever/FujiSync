package com.ilfforever.fujirecipes.ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujirecipes.data.remote.FxwApi
import com.ilfforever.fujirecipes.data.remote.FxwRecipe
import com.ilfforever.fujirecipes.ui.theme.Bg
import com.ilfforever.fujirecipes.ui.theme.Border
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelHigh
import com.ilfforever.fujirecipes.ui.theme.PanelLow
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun FxwSearchBenchScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FxwRecipe>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }

    fun search() {
        val q = query.trim()
        if (q.isBlank() || loading) return
        scope.launch {
            loading = true; error = null; results = emptyList(); searched = true
            runCatching { FxwApi.fetchRecipes(search = q, perPage = 20) }
                .onSuccess { results = it.recipes }
                .onFailure { error = it.message ?: "Unknown error" }
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FXW SEARCH BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
            Text(
                text = "CLOSE",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 1.3.sp,
                color = TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            // Search bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    enabled = !loading,
                    textStyle = TextStyle(
                        fontFamily = SansFamily,
                        fontSize = 15.sp,
                        color = TextPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search FXW…", fontFamily = SansFamily, fontSize = 15.sp, color = TextDim)
                        inner()
                    },
                )
                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (loading) PanelLow else Gold.copy(alpha = 0.15f))
                        .border(1.dp, if (loading) Border else Gold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable(enabled = !loading) { search() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loading) {
                        CircularProgressIndicator(color = Gold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("SEARCH", fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp, color = Gold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Status
            error?.let {
                Text(
                    text = it,
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                    color = Gold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PanelLow)
                        .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                )
                Spacer(Modifier.height(12.dp))
            }

            if (searched && !loading && results.isEmpty() && error == null) {
                Text("No results.", fontFamily = SansFamily, fontSize = 13.sp, color = TextMuted)
                Spacer(Modifier.height(12.dp))
            }

            // Results
            if (results.isNotEmpty()) {
                Text(
                    text = "${results.size} RESULT${if (results.size != 1) "S" else ""}",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = TextDim,
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(14.dp)),
                ) {
                    results.forEachIndexed { idx, recipe ->
                        SearchResultRow(recipe)
                        if (idx < results.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SearchResultRow(recipe: FxwRecipe) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(
            text = recipe.title,
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (recipe.filmSim.isNotBlank()) {
                Text(recipe.filmSim.uppercase(), fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 1.sp, color = Gold)
            }
            Text(recipe.date, fontFamily = MonoFamily, fontSize = 10.sp, color = TextDim)
            Text("${recipe.imageUrls.size} img", fontFamily = MonoFamily, fontSize = 10.sp, color = TextDim)
            Text("${recipe.params.size} params", fontFamily = MonoFamily, fontSize = 10.sp, color = TextDim)
        }
        if (recipe.params.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelHigh)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                recipe.params.entries.take(6).forEach { (k, v) ->
                    Row {
                        Text(k, fontFamily = MonoFamily, fontSize = 10.sp, color = TextMuted, modifier = Modifier.width(130.dp))
                        Text(v, fontFamily = MonoFamily, fontSize = 10.sp, color = TextPrimary)
                    }
                }
                if (recipe.params.size > 6) {
                    Text("+${recipe.params.size - 6} more", fontFamily = MonoFamily, fontSize = 10.sp, color = TextDim)
                }
            }
        }
    }
}
