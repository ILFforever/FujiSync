package com.ilfforever.fujisync.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

// ── EXIF import loading ───────────────────────────────────────────

@Composable
internal fun ExifImportLoadingScreen(
    eyebrow: String = "READING PHOTO",
    subtitle: String = "Extracting recipe data",
) {
    val transition = rememberInfiniteTransition(label = "exif-load")

    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan-y",
    )
    val borderAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "border-pulse",
    )
    val textAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "text-pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Final).changes.forEach {
                            if (!it.isConsumed) it.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            // ── Scan frame ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PanelLow)
                    .border(1.dp, Gold.copy(alpha = borderAlpha), RoundedCornerShape(18.dp)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gold = Color(0xFFC99A4E)
                    val inset = 11.dp.toPx()
                    val scanLineInset = inset + 8.dp.toPx()
                    val innerTop = scanLineInset
                    val innerBottom = size.height - scanLineInset
                    val innerLeft = scanLineInset
                    val innerRight = size.width - scanLineInset
                    val y = innerTop + (innerBottom - innerTop) * scanProgress
                    val trailH = (innerBottom - innerTop) * 0.4f

                    // Glow trail above scan line, clipped to the inset scan lane.
                    if (y > innerTop) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, gold.copy(alpha = 0.10f)),
                                startY = maxOf(innerTop, y - trailH),
                                endY = y,
                            ),
                            topLeft = Offset(innerLeft, maxOf(innerTop, y - trailH)),
                            size = Size(innerRight - innerLeft, minOf(trailH, y - innerTop)),
                        )
                    }

                    // Scan line — gradient fade at edges, inset from the frame.
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                gold.copy(alpha = 0.55f),
                                gold,
                                gold.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                            startX = innerLeft,
                            endX = innerRight,
                        ),
                        start = Offset(innerLeft, y),
                        end = Offset(innerRight, y),
                        strokeWidth = 1.5.dp.toPx(),
                    )

                    // Corner brackets — viewfinder aesthetic
                    val arm = 13.dp.toPx()
                    val pad = 11.dp.toPx()
                    val sw = 1.5.dp.toPx()
                    // Top-left
                    drawLine(gold, Offset(pad, pad), Offset(pad + arm, pad), sw)
                    drawLine(gold, Offset(pad, pad), Offset(pad, pad + arm), sw)
                    // Top-right
                    drawLine(gold, Offset(size.width - pad, pad), Offset(size.width - pad - arm, pad), sw)
                    drawLine(gold, Offset(size.width - pad, pad), Offset(size.width - pad, pad + arm), sw)
                    // Bottom-left
                    drawLine(gold, Offset(pad, size.height - pad), Offset(pad + arm, size.height - pad), sw)
                    drawLine(gold, Offset(pad, size.height - pad), Offset(pad, size.height - pad - arm), sw)
                    // Bottom-right
                    drawLine(gold, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - arm, size.height - pad), sw)
                    drawLine(gold, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - arm), sw)
                }
            }

            // ── Labels ─────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = eyebrow,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = TextPrimary.copy(alpha = textAlpha),
                )
                Text(
                    text = subtitle,
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextDim.copy(alpha = textAlpha * 0.7f),
                )
            }
        }
    }
}

// ── EXIF import error ─────────────────────────────────────────────

@Composable
internal fun ExifImportErrorScreen(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onShareRawDump: (() -> Unit)? = null,
    onCreateNew: (() -> Unit)? = null,
    title: String = "NO RECIPE FOUND",
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Final).changes.forEach {
                            if (!it.isConsumed) it.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // ── Icon frame — static, error state ──────────────────
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✕",
                    fontFamily = MonoFamily,
                    fontSize = 28.sp,
                    color = TextDim,
                )
            }

            // ── Labels ────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = TextPrimary,
                )
                Text(
                    text = message,
                    fontFamily = SansFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            // ── Actions ───────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                com.ilfforever.fujisync.ui.components.PrimaryCTA(
                    label = "Try Another Photo",
                    onClick = onRetry,
                )
                if (onCreateNew != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onCreateNew)
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "CREATE NEW RECIPE FROM THIS PHOTO",
                            fontFamily = MonoFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.3.sp,
                            color = Gold,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Done",
                        fontFamily = SansFamily,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        fontSize = 15.sp,
                        color = TextMuted,
                    )
                }
                if (onShareRawDump != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onShareRawDump)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "SHARE OCR DUMP",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp,
                            color = Gold,
                        )
                    }
                }
            }
        }
    }
}

internal fun formatOcrDump(
    rawText: String?,
    result: com.ilfforever.fujisync.data.ocr.OcrParseResult?,
): String = buildString {
    appendLine("=== PARSE RESULT ===")
    if (result == null) {
        appendLine("No recipe fields found (parse returned null)")
    } else {
        appendLine("Matched: ${result.matchedCount} field(s)")
        appendLine()
        appendLine("Film Simulation: ${result.sim}")
        appendLine()
        appendLine("Effects:")
        result.effects.forEach { (k, v) -> appendLine("  $k: $v") }
        appendLine()
        appendLine("Tone:")
        result.tone.forEach { (k, v) -> appendLine("  $k: $v") }
        appendLine()
        appendLine("White Balance:")
        result.wb.forEach { (k, v) -> appendLine("  $k: $v") }
        if (result.unmatchedFields.isNotEmpty()) {
            appendLine()
            appendLine("Detected but not parsed: ${result.unmatchedFields.joinToString(", ")}")
        }
    }
    appendLine()
    appendLine("=== RAW OCR TEXT ===")
    appendLine(rawText ?: "(none)")
}
