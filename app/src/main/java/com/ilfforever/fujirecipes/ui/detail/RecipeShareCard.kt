package com.ilfforever.fujirecipes.ui.detail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.ilfforever.fujirecipes.ui.model.RecipeUiModel

private const val CARD_W = 1080
private const val PAD = 72
private const val QR_SIZE = 280
private const val REF_IMAGE_H = 420

// Brand palette as ARGB ints
private val C_BG        = Color.parseColor("#0D0D0D")
private val C_PANEL     = Color.parseColor("#161413")
private val C_GOLD      = Color.parseColor("#C99A4E")
private val C_GOLD_DIM  = 0x20C99A4E.toInt()
private val C_GOLD_RING = 0x55C99A4E.toInt()
private val C_TEXT_PRI  = Color.parseColor("#F5F1EC")
private val C_TEXT_MUT  = Color.parseColor("#8A847E")
private val C_TEXT_DIM  = Color.parseColor("#5B5650")
private val C_BORDER    = 0x18FFFFFF.toInt()
private val C_WHITE     = Color.WHITE

fun createRecipeShareCard(recipe: RecipeUiModel, qrBitmap: Bitmap, referenceBitmap: Bitmap? = null): Bitmap {
    val w = CARD_W.toFloat()
    val cw = w - PAD * 2f
    val hasReferenceImage = referenceBitmap != null && !referenceBitmap.isRecycled

    // Shared paint (mutated per-use)
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { isAntiAlias = true }

    // Pre-measure text blocks that affect height
    val nameLines = wrapText(
        recipe.name.uppercase(),
        tp.also { it.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); it.textSize = 64f; it.letterSpacing = 0.03f },
        cw,
    )
    val descLines = if (recipe.description.isNotBlank()) {
        wrapText(
            recipe.description,
            tp.also { it.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); it.textSize = 34f; it.letterSpacing = 0f },
            cw,
        )
    } else emptyList()
    val pills = recipe.pills.take(9)
    val pillCols = 3
    val pillRows = (pills.size + pillCols - 1) / pillCols

    // Compute total height
    var h = 0f
    h += 10f                                            // gold bar
    h += 64f                                            // top pad
    h += 36f                                            // FUJISYNC row
    h += 40f                                            // gap
    h += 1f                                             // divider
    h += 48f                                            // gap
    if (hasReferenceImage) {
        h += REF_IMAGE_H.toFloat()                      // reference image
        h += 44f                                        // gap
    }
    h += 22f                                            // RECIPE label
    h += 18f                                            // label gap
    h += nameLines.size * 78f                           // name
    h += 32f                                            // gap
    h += 44f                                            // sim pill
    if (descLines.isNotEmpty()) {
        h += 36f                                        // gap before desc
        h += descLines.size * 50f                       // description
    }
    if (pills.isNotEmpty()) {
        h += 52f                                        // gap + divider + gap
        h += 24f                                        // "SETTINGS" label
        h += 24f                                        // label gap
        h += pillRows * 52f                             // pill grid
    }
    h += 56f                                            // gap before QR section
    h += 1f                                             // divider
    h += 52f                                            // gap
    val qrBlockH = (QR_SIZE + 32).toFloat()
    h += qrBlockH                                       // QR + white padding
    h += 56f                                            // gap
    h += 1f                                             // footer divider
    h += 40f                                            // gap
    h += 28f                                            // footer text
    h += 64f                                            // bottom pad

    val totalH = h.toInt()
    val bitmap = Bitmap.createBitmap(CARD_W, totalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background
    canvas.drawColor(C_BG)

    // Subtle gradient overlay: slightly lighter at top, purely dark at bottom
    val gradPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, totalH * 0.55f,
            0x10FFFFFF.toInt(), 0x00000000,
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, w, totalH.toFloat(), gradPaint)

    // Gold top bar
    canvas.drawRect(0f, 0f, w, 10f, Paint().apply { color = C_GOLD })

    var y = 10f + 64f

    // --- FUJISYNC header ---
    drawText(canvas, tp, "FUJISYNC", PAD.toFloat(), y + 26f,
        Typeface.MONOSPACE, 22f, C_GOLD, ls = 0.18f)
    y += 36f + 40f

    // Divider
    canvas.drawRect(PAD.toFloat(), y, w - PAD, y + 1f, Paint().apply { color = C_BORDER })
    y += 1f + 48f

    // --- Optional reference image ---
    if (hasReferenceImage) {
        val imageRect = RectF(PAD.toFloat(), y, w - PAD, y + REF_IMAGE_H)
        drawCroppedRoundBitmap(canvas, referenceBitmap!!, imageRect, 22f)
        canvas.drawRoundRect(
            imageRect,
            22f, 22f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x22000000
                style = Paint.Style.STROKE
                strokeWidth = 2f
            },
        )
        val imageFade = Paint().apply {
            shader = LinearGradient(
                0f, imageRect.top, 0f, imageRect.bottom,
                0x00000000, 0xB0000000.toInt(),
                Shader.TileMode.CLAMP,
            )
        }
        val imagePath = Path().apply { addRoundRect(imageRect, 22f, 22f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(imagePath)
        canvas.drawRect(imageRect, imageFade)
        canvas.restore()

        drawText(canvas, tp, "REFERENCE IMAGE", PAD + 28f, imageRect.bottom - 28f,
            Typeface.MONOSPACE, 18f, 0xB8F5F1EC.toInt(), ls = 0.18f)
        y += REF_IMAGE_H + 44f
    }

    // --- RECIPE label ---
    drawText(canvas, tp, "RECIPE", PAD.toFloat(), y + 18f,
        Typeface.MONOSPACE, 18f, C_TEXT_DIM, ls = 0.22f)
    y += 22f + 18f

    // --- Recipe name ---
    tp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    tp.textSize = 64f
    tp.color = C_TEXT_PRI
    tp.letterSpacing = 0.03f
    for (line in nameLines) {
        canvas.drawText(line, PAD.toFloat(), y + 64f, tp)
        y += 78f
    }
    y += 32f

    // --- Film sim pill ---
    val simText = recipe.sim.uppercase()
    tp.typeface = Typeface.MONOSPACE
    tp.textSize = 24f
    tp.letterSpacing = 0.10f
    val simTw = tp.measureText(simText)
    val simPillW = simTw + 48f
    val simPillH = 44f
    val simRect = RectF(PAD.toFloat(), y, PAD + simPillW, y + simPillH)
    canvas.drawRoundRect(simRect, 22f, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C_GOLD_DIM })
    canvas.drawRoundRect(simRect, 22f, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C_GOLD_RING; style = Paint.Style.STROKE; strokeWidth = 1.5f
    })
    tp.color = C_GOLD
    canvas.drawText(simText, PAD + 24f, y + 29f, tp)
    y += simPillH

    // --- Description ---
    if (descLines.isNotEmpty()) {
        y += 36f
        tp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        tp.textSize = 34f
        tp.color = C_TEXT_MUT
        tp.letterSpacing = 0f
        for (line in descLines) {
            canvas.drawText(line, PAD.toFloat(), y + 34f, tp)
            y += 50f
        }
    }

    // --- Settings pills ---
    if (pills.isNotEmpty()) {
        y += 28f
        canvas.drawRect(PAD.toFloat(), y, w - PAD, y + 1f, Paint().apply { color = C_BORDER })
        y += 1f + 24f

        drawText(canvas, tp, "SETTINGS", PAD.toFloat(), y + 18f,
            Typeface.MONOSPACE, 18f, C_TEXT_DIM, ls = 0.22f)
        y += 24f + 24f

        val gap = 14f
        val pillW = (cw - gap * (pillCols - 1)) / pillCols
        val pillH = 38f
        tp.typeface = Typeface.MONOSPACE
        tp.textSize = 22f
        tp.letterSpacing = 0.05f

        pills.forEachIndexed { i, pill ->
            val col = i % pillCols
            val row = i / pillCols
            val px = PAD + col * (pillW + gap)
            val py = y + row * 52f
            val pr = RectF(px, py, px + pillW, py + pillH)
            canvas.drawRoundRect(pr, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C_BORDER })
            tp.color = C_TEXT_MUT
            // Clip text to pill width
            val maxTw = pillW - 24f
            val drawn = clipText(pill, tp, maxTw)
            canvas.drawText(drawn, px + 12f, py + 26f, tp)
        }
        y += pillRows * 52f
    }

    // --- QR section ---
    y += 28f
    canvas.drawRect(PAD.toFloat(), y, w - PAD, y + 1f, Paint().apply { color = C_BORDER })
    y += 1f + 52f

    val qrRight = w - PAD.toFloat()
    val qrLeft = qrRight - QR_SIZE - 32f  // white card: 16px padding each side
    val qrCardTop = y
    val qrCardBottom = qrCardTop + QR_SIZE + 32f

    // White QR card
    canvas.drawRoundRect(
        RectF(qrLeft, qrCardTop, qrRight, qrCardBottom),
        12f, 12f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C_WHITE },
    )
    val scaledQr = Bitmap.createScaledBitmap(qrBitmap, QR_SIZE, QR_SIZE, false)
    canvas.drawBitmap(scaledQr, qrLeft + 16f, qrCardTop + 16f, null)
    scaledQr.recycle()

    // Scan text, vertically centered beside QR
    val scanMaxW = qrLeft - PAD - 32f
    tp.typeface = Typeface.MONOSPACE
    tp.textSize = 26f
    tp.letterSpacing = 0.04f
    val scanLines = wrapText("Scan with FujiSync to import this recipe.", tp, scanMaxW)
    val scanBlockH = scanLines.size * 40f
    var scanY = qrCardTop + (qrCardBottom - qrCardTop - scanBlockH) / 2f
    tp.color = C_TEXT_MUT
    for (line in scanLines) {
        canvas.drawText(line, PAD.toFloat(), scanY + 26f, tp)
        scanY += 40f
    }

    y = qrCardBottom + 56f

    // Footer divider
    canvas.drawRect(PAD.toFloat(), y, w - PAD, y + 1f, Paint().apply { color = C_BORDER })
    y += 1f + 40f

    // Footer: FUJISYNC left, fujisync.app right
    drawText(canvas, tp, "FUJISYNC", PAD.toFloat(), y + 22f,
        Typeface.MONOSPACE, 20f, C_TEXT_DIM, ls = 0.18f)

    tp.typeface = Typeface.MONOSPACE
    tp.textSize = 20f
    tp.letterSpacing = 0.05f
    tp.color = C_TEXT_DIM
    val tagline = "fujisync.app"
    canvas.drawText(tagline, w - PAD - tp.measureText(tagline), y + 22f, tp)

    return bitmap
}

private fun drawText(
    canvas: Canvas,
    paint: Paint,
    text: String,
    x: Float,
    y: Float,
    typeface: Typeface,
    size: Float,
    color: Int,
    ls: Float = 0f,
) {
    paint.typeface = typeface
    paint.textSize = size
    paint.color = color
    paint.letterSpacing = ls
    canvas.drawText(text, x, y, paint)
}

private fun drawCroppedRoundBitmap(canvas: Canvas, bitmap: Bitmap, dest: RectF, radius: Float) {
    val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val dstRatio = dest.width() / dest.height()
    val src = if (srcRatio > dstRatio) {
        val srcW = (bitmap.height * dstRatio).toInt().coerceAtLeast(1)
        val left = ((bitmap.width - srcW) / 2).coerceAtLeast(0)
        Rect(left, 0, left + srcW, bitmap.height)
    } else {
        val srcH = (bitmap.width / dstRatio).toInt().coerceAtLeast(1)
        val top = ((bitmap.height - srcH) / 2).coerceAtLeast(0)
        Rect(0, top, bitmap.width, top + srcH)
    }
    val path = Path().apply { addRoundRect(dest, radius, radius, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(path)
    canvas.drawBitmap(bitmap, src, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    canvas.restore()
}

private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val test = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(test) <= maxWidth) {
            current = test
        } else {
            if (current.isNotEmpty()) lines.add(current)
            current = word
        }
    }
    if (current.isNotEmpty()) lines.add(current)
    return lines.ifEmpty { listOf("") }
}

private fun clipText(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var end = text.length
    while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
    return text.substring(0, end) + "…"
}
