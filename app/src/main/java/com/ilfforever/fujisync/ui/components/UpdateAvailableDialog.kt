package com.ilfforever.fujisync.ui.components

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.UpdateUiState
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.SheetBg
import com.ilfforever.fujisync.ui.theme.SheetBorder
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Startup modal shown when [checkForUpdatesOnStart][com.ilfforever.fujisync.ui.MainViewModel]
 * detects a newer GitHub release. [onUpdate] kicks off the existing download + install flow
 * (mirroring the About screen action); [onDismiss] defers the update.
 */
@Composable
fun UpdateAvailableDialog(
    update: UpdateUiState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val context = LocalContext.current
    val view = LocalView.current
    val busy = update.checking || update.downloading

    fun dismissWithMotion() {
        if (busy) return
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "update-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "update-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * overlayAlpha))
            .clickable(onClick = ::dismissWithMotion),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150, easing = FastOutSlowInEasing)) + slideInVertically(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 3 },
            ),
            exit = fadeOut(tween(105, easing = FastOutSlowInEasing)) + slideOutVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 4 },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(SheetBg)
                    .border(1.dp, SheetBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = {})
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp)
                        .width(38.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(TextDim.copy(alpha = 0.55f)),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UPDATE AVAILABLE",
                                fontFamily = MonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.8.sp,
                                color = Gold.copy(alpha = 0.78f),
                            )
                            Spacer(Modifier.height(6.dp))
                            val version = update.latestVersion?.takeIf { it.isNotBlank() }
                            val title = update.releaseName?.takeIf { it.isNotBlank() }
                                ?: version?.let { "v$it" }
                                ?: "Update available"
                            Text(
                                text = title,
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 0.1.sp,
                                color = TextPrimary,
                            )
                            // Only show the version sub-line when it adds info the title
                            // doesn't already convey (release name often equals the tag).
                            val showVersionSub = version != null &&
                                !title.equals("v$version", ignoreCase = true) &&
                                !title.equals(version, ignoreCase = true)
                            if (showVersionSub) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "v$version",
                                    fontFamily = MonoFamily,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.4.sp,
                                    color = TextDim,
                                )
                            }
                        }
                        if (!busy) {
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = "LATER",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.3.sp,
                                color = Gold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable(onClick = ::dismissWithMotion)
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                            )
                        }
                    }

                    val notes = update.releaseNotes?.trim().orEmpty()
                    if (notes.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "WHAT'S NEW",
                            fontFamily = MonoFamily,
                            fontSize = 9.sp,
                            letterSpacing = 1.6.sp,
                            color = TextDim,
                        )
                        Spacer(Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            ReleaseNotesMarkdown(notes)
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "A newer version of FujiSync is available on GitHub.",
                            fontFamily = SansFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextMuted,
                        )
                    }

                    update.error?.let { error ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = error,
                            fontFamily = SansFamily,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Gold,
                        )
                    }
                    if (update.installPermissionRequired) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = update.message
                                ?: "Allow FujiSync to install unknown apps, then tap Update again.",
                            fontFamily = SansFamily,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = TextMuted,
                        )
                    }

                    Spacer(Modifier.height(22.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Gold.copy(alpha = if (busy) 0.07f else 0.12f))
                            .border(
                                1.dp,
                                Gold.copy(alpha = if (busy) 0.22f else 0.45f),
                                RoundedCornerShape(14.dp),
                            )
                            .clickable(enabled = !busy, onClick = {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                                onUpdate()
                            })
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = updateButtonLabel(update),
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.6.sp,
                            color = if (busy) TextDim else Gold,
                        )
                    }
                }
            }
        }
    }
}

private fun updateButtonLabel(update: UpdateUiState): String = when {
    update.checking -> "CHECKING"
    update.downloading -> "DOWNLOADING..."
    update.installPermissionRequired -> "GRANT PERMISSION"
    update.error != null -> "RETRY UPDATE"
    else -> "UPDATE NOW"
}

// ── Minimal markdown rendering for GitHub release notes ──────────────────────
// Supports #/##/### headings, - / * bullets, and **bold** inline spans. This is
// intentionally tiny (no extra dependency); release notes only use this subset.

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val text: AnnotatedString) : MdBlock
    data class Paragraph(val text: AnnotatedString) : MdBlock
}

@Composable
private fun ReleaseNotesMarkdown(raw: String) {
    val blocks = remember(raw) {
        parseReleaseNotes(raw).let { list ->
            // Drop a leading "What's new" heading — the section already has that label.
            val first = list.firstOrNull()
            if (first is MdBlock.Heading && first.text.trim().equals("What's new", ignoreCase = true)) {
                list.drop(1)
            } else {
                list
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> {
                    if (index != 0) Spacer(Modifier.height(12.dp))
                    Text(
                        text = block.text,
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = when (block.level) {
                            1 -> 15.sp
                            2 -> 14.sp
                            else -> 12.5.sp
                        },
                        lineHeight = 19.sp,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(5.dp))
                }

                is MdBlock.Bullet -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "•",
                            fontFamily = SansFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = Gold.copy(alpha = 0.8f),
                            modifier = Modifier.width(16.dp),
                        )
                        Text(
                            text = block.text,
                            fontFamily = SansFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextMuted,
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                }

                is MdBlock.Paragraph -> {
                    Text(
                        text = block.text,
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
    }
}

private fun parseReleaseNotes(raw: String): List<MdBlock> {
    val lines = raw.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    val blocks = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()

    fun flushParagraph() {
        val text = paragraph.toString().trim()
        if (text.isNotEmpty()) blocks += MdBlock.Paragraph(inlineMarkdown(text))
        paragraph.setLength(0)
    }

    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> flushParagraph()

            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                val text = trimmed.dropWhile { it == '#' }.trim()
                blocks += MdBlock.Heading(level, stripInlineMarkers(text))
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                blocks += MdBlock.Bullet(inlineMarkdown(trimmed.substring(2).trim()))
            }

            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed)
            }
        }
    }
    flushParagraph()
    return blocks
}

/** Builds an [AnnotatedString], rendering `**bold**` spans and stripping `` ` `` markers. */
private fun inlineMarkdown(source: String): AnnotatedString = buildAnnotatedString {
    val text = source.replace("`", "")
    var i = 0
    while (i < text.length) {
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", startIndex = i + 2)
            if (end != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                append(text.substring(i + 2, end))
                pop()
                i = end + 2
                continue
            }
        }
        append(text[i])
        i++
    }
}

/** Removes inline markers from plain heading text. */
private fun stripInlineMarkers(text: String): String =
    text.replace("**", "").replace("`", "")
