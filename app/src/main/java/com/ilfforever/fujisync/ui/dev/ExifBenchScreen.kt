package com.ilfforever.fujirecipes.ui.dev

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujirecipes.data.exif.ExifResult
import com.ilfforever.fujirecipes.data.exif.ExifSection
import com.ilfforever.fujirecipes.data.exif.FujiExifReader
import com.ilfforever.fujirecipes.ui.components.SectionLabel
import com.ilfforever.fujirecipes.ui.theme.Bg
import com.ilfforever.fujirecipes.ui.theme.Border
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelLow
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExifBenchScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var result by remember { mutableStateOf<ExifResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            loading = true
            error = null
            result = null
            val parsed = withContext(Dispatchers.IO) {
                runCatching { FujiExifReader.read(context, uri) }
            }
            result = parsed.getOrNull()
            error = parsed.exceptionOrNull()?.message
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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "EXIF BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (result != null) {
                    Text(
                        text = "SHARE",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.3.sp,
                        color = Gold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { shareExifResult(context, result!!) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
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
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            // Pick button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelLow)
                    .border(1.dp, if (loading) Border else Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !loading) { picker.launch("image/*") }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Gold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(16.dp).width(16.dp),
                    )
                }
                Text(
                    text = if (loading) "Reading…" else "PICK FUJI JPEG",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    letterSpacing = 0.6.sp,
                    color = if (loading) TextMuted else TextPrimary,
                )
            }

            error?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = msg,
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
            }

            result?.let { exif ->
                Spacer(Modifier.height(24.dp))
                if (exif.sections.isEmpty()) {
                    Text(
                        text = "No metadata found.",
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        color = TextMuted,
                    )
                } else {
                    exif.sections.forEach { section ->
                        ExifSectionBlock(section)
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun shareExifResult(context: android.content.Context, result: ExifResult) {
    val text = buildString {
        result.sections.forEach { section ->
            appendLine("=== ${section.name} ===")
            section.tags.forEach { (tag, value) -> appendLine("$tag: $value") }
            appendLine()
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Fuji EXIF dump")
        putExtra(Intent.EXTRA_TEXT, text.trim())
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@Composable
private fun ExifSectionBlock(section: ExifSection) {
    SectionLabel(text = section.name.uppercase())
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        section.tags.forEachIndexed { idx, (tag, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = tag,
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.weight(0.45f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = value,
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(0.55f),
                )
            }
            if (idx < section.tags.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            }
        }
    }
}
