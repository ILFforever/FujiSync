package com.paeki.fujirecipes.data.qr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.QRCodeWriter
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import org.json.JSONArray
import org.json.JSONObject

object RecipeQr {
    private const val FORMAT = "fujisync.recipe"
    private const val VERSION = 1

    fun encode(recipe: RecipeUiModel): String =
        JSONObject().apply {
            put("f", FORMAT)
            put("v", VERSION)
            put("n", recipe.name)
            put("s", recipe.sim)
            put("d", recipe.description)
            put("e", JSONObject(recipe.effects as Map<*, *>))
            put("t", JSONObject(recipe.tone as Map<*, *>))
            put("w", JSONObject(recipe.wb as Map<*, *>))
            put("p", JSONArray(recipe.pills))
        }.toString()

    fun decode(payload: String): RecipeUiModel? = runCatching {
        val obj = JSONObject(payload)
        if (obj.optString("f") != FORMAT) return@runCatching null
        if (obj.optInt("v") != VERSION) return@runCatching null
        RecipeUiModel(
            slot = "",
            name = obj.optString("n").trim().ifBlank { "QR Import" },
            sim = obj.optString("s").trim().ifBlank { "Provia / Standard" },
            description = obj.optString("d"),
            pills = obj.optJSONArray("p")?.toStringList().orEmpty(),
            effects = obj.optJSONObject("e")?.toStringMap().orEmpty(),
            tone = obj.optJSONObject("t")?.toStringMap().orEmpty(),
            wb = obj.optJSONObject("w")?.toStringMap().orEmpty(),
        )
    }.getOrNull()

    fun createBitmap(payload: String, sizePx: Int = 960): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        return matrix.toBitmap()
    }

    fun decodeBitmap(context: Context, uri: Uri): RecipeUiModel? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null
        return runCatching {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(
                binaryBitmap,
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.CHARACTER_SET to "UTF-8",
                    DecodeHintType.TRY_HARDER to true,
                ),
            )
            decode(result.text)
        }.getOrNull()
    }

    fun decodeYPlane(
        data: ByteArray,
        dataWidth: Int,
        dataHeight: Int,
        frameWidth: Int,
        frameHeight: Int,
    ): RecipeUiModel? {
        val safeWidth = frameWidth.coerceAtMost(dataWidth)
        val safeHeight = frameHeight.coerceAtMost(dataHeight)
        val cropSize = (minOf(safeWidth, safeHeight) * 0.82f).toInt().coerceAtLeast(1)
        val cropLeft = ((safeWidth - cropSize) / 2).coerceAtLeast(0)
        val cropTop = ((safeHeight - cropSize) / 2).coerceAtLeast(0)
        val full = PlanarYUVLuminanceSource(
            data,
            dataWidth,
            dataHeight,
            0,
            0,
            safeWidth,
            safeHeight,
            false,
        )
        val center = PlanarYUVLuminanceSource(
            data,
            dataWidth,
            dataHeight,
            cropLeft,
            cropTop,
            cropSize,
            cropSize,
            false,
        )
        return decodeLuminanceSource(center) ?: decodeLuminanceSource(full)
    }

    private fun decodeLuminanceSource(source: com.google.zxing.LuminanceSource): RecipeUiModel? {
        val reader = MultiFormatReader().apply {
            setHints(qrDecodeHints())
        }
        return runCatching {
            decode(reader, source) ?: source
                .takeIf { it.isRotateSupported }
                ?.let { decode(reader, it.rotateCounterClockwise()) }
        }.getOrNull()
    }

    private fun decode(reader: MultiFormatReader, source: com.google.zxing.LuminanceSource): RecipeUiModel? =
        runCatching {
            decode(reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text)
        }.getOrNull()

    private fun BitMatrix.toBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, if (get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bitmap
    }

    private fun qrDecodeHints(): Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8",
        DecodeHintType.TRY_HARDER to true,
    )

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }

    private fun JSONObject.toStringMap(): Map<String, String> =
        mutableMapOf<String, String>().also { m -> keys().forEach { key -> m[key] = getString(key) } }
}
