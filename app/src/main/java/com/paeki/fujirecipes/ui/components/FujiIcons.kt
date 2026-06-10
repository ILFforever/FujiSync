package com.paeki.fujirecipes.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Icon paths are vendored/adapted from Lucide (ISC license) into Compose ImageVectors.
// We keep the local wrapper so all icons inherit the app's tint and sizing behavior.
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
    strokeLineWidth = 1.8f,
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
    stroke("M13.997 4a2 2 0 0 1 1.76 1.05l.486.9A2 2 0 0 0 18.003 7H20a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.997a2 2 0 0 0 1.759-1.048l.489-.904A2 2 0 0 1 10.004 4z")
    stroke(circle(12f, 13f, 3f))
}

val IconFolder = icon("Folder") {
    stroke("M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z")
}

val IconProfile = icon("Profile") {
    stroke("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2")
    stroke(circle(12f, 7f, 4f))
}

val IconPhone = icon("Phone") {
    stroke("M18 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2Z")
    stroke("M12 18h.01")
}

// ── UI glyphs ─────────────────────────────────────────────────────
val IconUSB = icon("USB") {
    stroke(circle(10f, 7f, 1f))
    stroke(circle(4f, 20f, 1f))
    stroke("M4.7 19.3 19 5")
    stroke("m21 3-3 1 2 2Z")
    stroke("M9.26 7.68 5 12l2 5")
    stroke("m10 14 5 2 3.5-3.5")
    stroke("m18 12 1-1 1 1-1 1Z")
}

val IconSearch = icon("Search") {
    stroke("m21 21-4.34-4.34")
    stroke(circle(11f, 11f, 8f))
}

val IconSort = icon("Sort") {
    stroke("m21 16-4 4-4-4")
    stroke("M17 20V4")
    stroke("m3 8 4-4 4 4")
    stroke("M7 4v16")
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
    stroke("m9 18 6-6-6-6")
}

val IconClose = icon("Close") {
    stroke("M6 6l12 12M18 6L6 18")
}

val IconCheck = icon("Check") {
    stroke("M20 6 9 17l-5-5")
}

val IconStar = icon("Star") {
    stroke("M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z")
}

val IconStarFilled = icon("StarFilled") {
    filled("M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z")
}

val IconMore = icon("More") {
    stroke(circle(5f, 12f, 1f))
    stroke(circle(12f, 12f, 1f))
    stroke(circle(19f, 12f, 1f))
}

val IconMoreVertical = icon("MoreVertical") {
    stroke(circle(12f, 5f, 1f))
    stroke(circle(12f, 12f, 1f))
    stroke(circle(12f, 19f, 1f))
}

val IconRefresh = icon("Refresh") {
    stroke("M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8")
    stroke("M21 3v5h-5")
    stroke("M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16")
    stroke("M8 16H3v5")
}

val IconBackup = icon("Backup") {
    stroke("M2 8h20")
    stroke("M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8")
    stroke("M10 12h4")
}

val IconRestore = icon("Restore") {
    stroke("M2 8h20")
    stroke("M4 8v11a2 2 0 0 0 2 2h2")
    stroke("M20 8v11a2 2 0 0 1-2 2h-2")
    stroke("m9 15 3-3 3 3")
    stroke("M12 12v9")
}

// ── Property row icons ────────────────────────────────────────────
val IconDR = icon("DynamicRange") {
    stroke(circle(12f, 12f, 10f))
    stroke("m14.31 8 5.74 9.94")
    stroke("M9.69 8h11.48")
    stroke("m7.38 12 5.74-9.94")
    stroke("M9.69 16 3.95 6.06")
    stroke("M14.31 16H2.83")
    stroke("m16.62 12-5.74 9.94")
}

val IconGrain = icon("Grain") {
    stroke("M4 6h16")
    stroke("M4 12h16")
    stroke("M4 18h16")
    stroke("M8 6v12")
    stroke("M16 6v12")
    stroke(circle(8f, 6f, 0.9f))
    stroke(circle(16f, 6f, 0.9f))
    stroke(circle(4f, 12f, 0.9f))
    stroke(circle(12f, 12f, 0.9f))
    stroke(circle(20f, 12f, 0.9f))
    stroke(circle(8f, 18f, 0.9f))
    stroke(circle(16f, 18f, 0.9f))
}

val IconCC = icon("ColorChrome") {
    stroke("M15.536 11.293a1 1 0 0 0 0 1.414l2.376 2.377a1 1 0 0 0 1.414 0l2.377-2.377a1 1 0 0 0 0-1.414l-2.377-2.377a1 1 0 0 0-1.414 0z")
    stroke("M2.297 11.293a1 1 0 0 0 0 1.414l2.377 2.377a1 1 0 0 0 1.414 0l2.377-2.377a1 1 0 0 0 0-1.414L6.088 8.916a1 1 0 0 0-1.414 0z")
    stroke("M8.916 17.912a1 1 0 0 0 0 1.415l2.377 2.376a1 1 0 0 0 1.414 0l2.377-2.376a1 1 0 0 0 0-1.415l-2.377-2.376a1 1 0 0 0-1.414 0z")
    stroke("M8.916 4.674a1 1 0 0 0 0 1.414l2.377 2.376a1 1 0 0 0 1.414 0l2.377-2.376a1 1 0 0 0 0-1.414l-2.377-2.377a1 1 0 0 0-1.414 0z")
}

val IconCCFXBlue = icon("CCFXBlue") {
    stroke(circle(12f, 12f, 7f))
    stroke("M12 5a7 7 0 0 0 0 14V5z")
}

val IconSmoothSkin = icon("SmoothSkin") {
    stroke(circle(12f, 12f, 7f))
    stroke("M9 14c1 1.2 5 1.2 6 0")
    filled(circle(9.5f, 10.5f, 0.4f))
    filled(circle(14.5f, 10.5f, 0.4f))
}

val IconHighlight = icon("Highlight") {
    stroke(circle(12f, 12f, 4f))
    stroke("M12 2v2")
    stroke("M12 20v2")
    stroke("m4.93 4.93 1.41 1.41")
    stroke("m17.66 17.66 1.41 1.41")
    stroke("M2 12h2")
    stroke("M20 12h2")
    stroke("m6.34 17.66-1.41 1.41")
    stroke("m19.07 4.93-1.41 1.41")
}

val IconShadow = icon("Shadow") {
    stroke("M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401")
}

val IconColor = icon("Color") {
    stroke("m14.622 17.897-10.68-2.913")
    stroke("M18.376 2.622a1 1 0 1 1 3.002 3.002L17.36 9.643a.5.5 0 0 0 0 .707l.944.944a2.41 2.41 0 0 1 0 3.408l-.944.944a.5.5 0 0 1-.707 0L8.354 7.348a.5.5 0 0 1 0-.707l.944-.944a2.41 2.41 0 0 1 3.408 0l.944.944a.5.5 0 0 0 .707 0z")
    stroke("M9 8c-1.804 2.71-3.97 3.46-6.583 3.948a.507.507 0 0 0-.302.819l7.32 8.883a1 1 0 0 0 1.185.204C12.735 20.405 16 16.792 16 15")
}

val IconSharpness = icon("Sharpness") {
    stroke("M13.73 4a2 2 0 0 0-3.46 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z")
}

val IconNR = icon("NoiseReduction") {
    stroke(circle(12f, 12f, 10f))
    stroke(circle(12f, 12f, 1f))
}

val IconClarity = icon("Clarity") {
    stroke("M2.7 10.3a2.41 2.41 0 0 0 0 3.41l7.59 7.59a2.41 2.41 0 0 0 3.41 0l7.59-7.59a2.41 2.41 0 0 0 0-3.41l-7.59-7.59a2.41 2.41 0 0 0-3.41 0Z")
}

val IconISO = icon("ISO") {
    stroke("M5 17h14")
    stroke("M7 17l2.5-10h5L17 17")
    stroke("M10.2 11h3.6")
    stroke("M9 21h6")
    stroke("M12 3v2")
}

val IconExposureComp = icon("ExposureCompensation") {
    stroke(circle(12f, 12f, 8.5f))
    stroke("M12 3.5v2")
    stroke("M12 18.5v2")
    stroke("M3.5 12h2")
    stroke("M18.5 12h2")
    stroke("M7.5 9h5")
    stroke("M10 6.5v5")
    stroke("M13.5 15h4")
    stroke("M6.7 17.3 17.3 6.7")
}

val IconWB = icon("WhiteBalance") {
    stroke("M14 4v10.54a4 4 0 1 1-4 0V4a2 2 0 0 1 4 0Z")
}

val IconWBShift = icon("WBShift") {
    stroke("m18 8 4 4-4 4")
    stroke("M2 12h20")
    stroke("m6 8-4 4 4 4")
}

val IconGlobe = icon("Globe") {
    stroke("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z")
    stroke("M2 12h20")
    stroke("M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z")
}

val IconEdit = icon("Edit") {
    stroke("M11 5H6a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2v-5")
    stroke("M16.5 3.5a2.121 2.121 0 0 1 3 3L10 16l-4 1 1-4 9.5-9.5z")
}

val IconTool = icon("Tool") {
    stroke("M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.8-3.8a6 6 0 0 1-7.9 7.9L7 20a2.1 2.1 0 0 1-3-3l6.5-6.5a6 6 0 0 1 7.9-7.9l-3.7 3.7z")
}

val IconCopy = icon("Copy") {
    stroke("M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-4-4H8z")
    stroke("M14 2v6h6")
    stroke("M8 10h2M8 14h8M8 18h6")
}

val IconImage = icon("Image") {
    stroke("M5 3a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2H5z")
    stroke(circle(8.5f, 8.5f, 1.5f))
    stroke("M21 15l-5-5L5 20")
}

val IconScan = icon("Scan") {
    stroke("M3 7V5a2 2 0 0 1 2-2h2")
    stroke("M17 3h2a2 2 0 0 1 2 2v2")
    stroke("M21 17v2a2 2 0 0 1-2 2h-2")
    stroke("M7 21H5a2 2 0 0 1-2-2v-2")
    stroke("M7 12h10")
}

val IconQrCode = icon("QrCode") {
    stroke("M3 3h7v7H3z")
    stroke("M14 3h7v7h-7z")
    stroke("M3 14h7v7H3z")
    stroke("M14 14h.01")
    stroke("M18 14h.01")
    stroke("M14 18h.01")
    stroke("M18 18h.01")
    stroke("M14 21v-3h3")
    stroke("M17 14h4")
}

val IconArrowDown = icon("ArrowDown") {
    stroke("M12 5v14")
    stroke("M19 12l-7 7-7-7")
}

val IconTrash = icon("Trash") {
    stroke("M3 6h18M8 6V4h8v2M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6")
    stroke("M10 11v6M14 11v6")
}

val IconDragHandle = icon("DragHandle") {
    stroke("M8 8h8")
    stroke("M8 12h8")
    stroke("M8 16h8")
}

val IconReorder = icon("Reorder") {
    stroke("M3 7h18")
    stroke("M3 12h18")
    stroke("M3 17h18")
}

// ── Property icon map ─────────────────────────────────────────────
val PROP_ICONS: Map<String, ImageVector> = mapOf(
    "D Range Priority" to IconDR,
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
    "ISO" to IconISO,
    "Exposure Compensation" to IconExposureComp,
    "Exposure Comp" to IconExposureComp,
    "White Balance" to IconWB,
    "WB Shift R" to IconWBShift,
    "WB Shift B" to IconWBShift,
)
