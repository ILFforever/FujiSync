package com.paeki.fujirecipes.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Helper — builds a 24×24 icon with stroke-only paths (1.5pt, round cap/join, currentColor)
private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private fun ImageVector.Builder.stroke(d: String) = addPath(
    pathData = addPathNodes(d),
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 1.5f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
)

// Small filled circle helper: cx, cy, r  →  arc-pair path string
private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 -${2 * r} 0"

private fun ImageVector.Builder.filled(d: String) = addPath(
    pathData = addPathNodes(d),
    fill = SolidColor(Color.Black),
    stroke = null,
    strokeLineWidth = 0f,
)

// ── Navigation ────────────────────────────────────────────────────
val IconCamera = icon("Camera") {
    stroke("M3 8h3l2-3h8l2 3h3v11H3V8z")
    stroke(circle(12f, 13f, 3.5f))
}

val IconFolder = icon("Folder") {
    stroke("M3 6.5A1.5 1.5 0 0 1 4.5 5h4l2 2h9A1.5 1.5 0 0 1 21 8.5V18a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18V6.5z")
}

val IconProfile = icon("Profile") {
    stroke(circle(12f, 8f, 4f))
    stroke("M4 20c1.5-3.5 4.5-5 8-5s6.5 1.5 8 5")
}

// ── UI glyphs ─────────────────────────────────────────────────────
val IconUSB = icon("USB") {
    stroke("M12 21V6")
    stroke("M8 9l4-4 4 4")
    filled(circle(12f, 21f, 1.5f))
    stroke("M9 13h6")
    stroke("M15 13l2 2v2l-3 2")
}

val IconSearch = icon("Search") {
    stroke(circle(11f, 11f, 6f))
    stroke("M16 16l4 4")
}

val IconSort = icon("Sort") {
    stroke("M7 4v16")
    stroke("M3 8l4-4 4 4")
    stroke("M17 20V4")
    stroke("M21 16l-4 4-4-4")
}

val IconFilter = icon("Filter") {
    stroke("M4 6h16")
    stroke("M7 12h10")
    stroke("M10 18h4")
}

val IconPlus = icon("Plus") {
    stroke("M12 5v14M5 12h14")
}

val IconChevronRight = icon("ChevronRight") {
    stroke("M9 6l6 6-6 6")
}

val IconClose = icon("Close") {
    stroke("M6 6l12 12M18 6L6 18")
}

val IconCheck = icon("Check") {
    stroke("M5 12l5 5 9-11")
}

val IconStar = icon("Star") {
    stroke("M12 4l2.5 5.5 6 .7-4.5 4 1.2 6L12 17.3 6.8 20.2l1.2-6L3.5 10.2l6-.7L12 4z")
}

val IconMore = icon("More") {
    filled(circle(6f, 12f, 1f))
    filled(circle(12f, 12f, 1f))
    filled(circle(18f, 12f, 1f))
}

val IconRefresh = icon("Refresh") {
    stroke("M21 12a9 9 0 0 1-15.5 6.3L3 16")
    stroke("M3 12a9 9 0 0 1 15.5-6.3L21 8")
    stroke("M21 3v5h-5")
    stroke("M3 21v-5h5")
}

val IconBackup = icon("Backup") {
    stroke("M12 19V5")
    stroke("M5 12l7-7 7 7")
    stroke("M5 19h14")
}

val IconRestore = icon("Restore") {
    stroke("M21 12a9 9 0 0 1-15.5 6.3L3 16")
    stroke("M3 12a9 9 0 0 1 15.5-6.3L21 8")
    stroke("M3 21v-5h5")
    stroke("M21 3v5h-5")
}

// ── Property row icons ────────────────────────────────────────────
val IconDR = icon("DynamicRange") {
    stroke(circle(12f, 12f, 4f))
    stroke("M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4")
}

val IconGrain = icon("Grain") {
    filled(circle(6f, 8f, 0.8f))
    filled(circle(11f, 6f, 0.8f))
    filled(circle(17f, 9f, 0.8f))
    filled(circle(8f, 13f, 0.8f))
    filled(circle(14f, 14f, 0.8f))
    filled(circle(6f, 18f, 0.8f))
    filled(circle(12f, 19f, 0.8f))
    filled(circle(18f, 17f, 0.8f))
    filled(circle(19f, 5f, 0.8f))
    filled(circle(4f, 13f, 0.8f))
}

val IconCC = icon("ColorChrome") {
    stroke("M6 18l8-12 4 6-8 12-4-6z")
    stroke("M14 6l4 6")
}

val IconCCFXBlue = icon("CCFXBlue") {
    stroke(circle(12f, 12f, 7f))
    filled("M12 5a7 7 0 0 0 0 14V5z")
}

val IconSmoothSkin = icon("SmoothSkin") {
    stroke(circle(12f, 12f, 7f))
    stroke("M9 14c1 1.2 5 1.2 6 0")
    filled(circle(9.5f, 10.5f, 0.4f))
    filled(circle(14.5f, 10.5f, 0.4f))
}

val IconHighlight = icon("Highlight") {
    filled(circle(12f, 12f, 3f))
    stroke("M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4")
}

val IconShadow = icon("Shadow") {
    stroke("M17 14A6 6 0 1 1 10 7a5 5 0 0 0 7 7z")
}

val IconColor = icon("Color") {
    stroke("M9 19l-3-3 9-9 3 3-9 9z")
    stroke("M14 7l3-3")
    stroke("M3 21l4-2")
}

val IconSharpness = icon("Sharpness") {
    stroke("M12 4l9 16H3L12 4z")
}

val IconNR = icon("NoiseReduction") {
    stroke(circle(12f, 12f, 3f))
    stroke(circle(12f, 12f, 7f))
}

val IconClarity = icon("Clarity") {
    stroke("M12 3.5l8.5 8.5-8.5 8.5L3.5 12 12 3.5z")
}

val IconWB = icon("WhiteBalance") {
    stroke(circle(12f, 12f, 7f))
    stroke("M12 5v14")
    stroke("M5 12h14")
}

val IconWBShift = icon("WBShift") {
    stroke("M4 12h16")
    stroke("M10 7l-5 5 5 5")
    stroke("M14 7l5 5-5 5")
}

val IconEdit = icon("Edit") {
    stroke("M11 5H6a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2v-5")
    stroke("M16.5 3.5a2.121 2.121 0 0 1 3 3L10 16l-4 1 1-4 9.5-9.5z")
}

// ── Property icon map ─────────────────────────────────────────────
val PROP_ICONS: Map<String, ImageVector> = mapOf(
    "Dynamic Range" to IconDR,
    "Grain Effect" to IconGrain,
    "Color Chrome" to IconCC,
    "Color Chrome FX Blue" to IconCCFXBlue,
    "Smooth Skin" to IconSmoothSkin,
    "Highlight Tone" to IconHighlight,
    "Shadow Tone" to IconShadow,
    "Color" to IconColor,
    "Sharpness" to IconSharpness,
    "High ISO NR" to IconNR,
    "Clarity" to IconClarity,
    "White Balance" to IconWB,
    "WB Shift R" to IconWBShift,
    "WB Shift B" to IconWBShift,
)
