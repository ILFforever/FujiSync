package com.ilfforever.fujisync.data.qr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.util.Base64
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
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object RecipeQr {
    private const val FORMAT = "fujisync.recipe"
    private const val MAX_DESC_CHARS = 400

    // Effects map keys
    private val EK_ENC = mapOf(
        "D Range Priority" to "drp", "Dynamic Range" to "dr",
        "Grain Effect" to "gr", "Color Chrome" to "cc",
        "Color Chrome FX Blue" to "ccb", "Smooth Skin" to "ss",
    )
    private val EK_DEC = EK_ENC.entries.associate { (k, v) -> v to k }

    // Effects map values
    private val EV_ENC = mapOf(
        "Off" to "0", "DR Auto" to "dra",
        "DR100%" to "100", "DR200%" to "200", "DR400%" to "400",
        "Weak" to "w", "Strong" to "s",
        "Weak Small" to "ws", "Weak Large" to "wl",
        "Strong Small" to "ss", "Strong Large" to "sl",
        "—" to "-",
    )
    private val EV_DEC = EV_ENC.entries.associate { (k, v) -> v to k }

    // Tone map keys (values are numeric strings, no abbreviation needed)
    private val TK_ENC = mapOf(
        "Highlight Tone" to "hl", "Shadow Tone" to "sh",
        "Color" to "cl", "Sharpness" to "sp",
        "High ISO NR" to "nr", "Clarity" to "cy",
        "Mono WC" to "mw", "Mono MG" to "mm",
    )
    private val TK_DEC = TK_ENC.entries.associate { (k, v) -> v to k }

    // White balance keys
    private val WK_ENC = mapOf(
        "White Balance" to "m", "WB Shift R" to "r", "WB Shift B" to "b",
    )
    private val WK_DEC = WK_ENC.entries.associate { (k, v) -> v to k }

    // White balance values
    private val WV_ENC = mapOf(
        "Auto" to "a", "Auto White Priority" to "awp", "Ambience Priority" to "ap",
        "Daylight" to "d", "Incandescent" to "i", "Underwater" to "uw",
        "Fluorescent 1" to "f1", "Fluorescent 2" to "f2", "Fluorescent 3" to "f3",
        "Shade" to "sh", "Color Temperature" to "ct",
    )
    private val WV_DEC = WV_ENC.entries.associate { (k, v) -> v to k }

    /** Produces the QR payload: abbreviated JSON → gzip → base64. */
    fun encode(recipe: RecipeUiModel): String {
        val json = toJson(recipe)
        val compressed = gzip(json.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(compressed, Base64.NO_WRAP)
    }

    private fun toJson(recipe: RecipeUiModel): String {
        val abbrevEffects = recipe.effects.entries.associate { (k, v) -> (EK_ENC[k] ?: k) to (EV_ENC[v] ?: v) }
        val abbrevTone = recipe.tone.entries.associate { (k, v) -> (TK_ENC[k] ?: k) to v }
        val abbrevWb = recipe.wb.entries.associate { (k, v) -> (WK_ENC[k] ?: k) to (WV_ENC[v] ?: v) }
        return JSONObject().apply {
            put("f", FORMAT)
            put("n", recipe.name)
            put("s", recipe.sim)
            put("d", recipe.description.take(MAX_DESC_CHARS))
            put("e", JSONObject(abbrevEffects as Map<*, *>))
            put("t", JSONObject(abbrevTone as Map<*, *>))
            put("w", JSONObject(abbrevWb as Map<*, *>))
            put("p", JSONArray(recipe.pills))
            recipe.isoMin?.let { put("iMin", it) }
            recipe.isoMax?.let { put("iMax", it) }
            recipe.exposureCompMin?.let { put("ecMin", it.toDouble()) }
            recipe.exposureCompMax?.let { put("ecMax", it.toDouble()) }
            if (recipe.sensorGens.isNotEmpty()) put("sg", JSONArray(recipe.sensorGens))
        }.toString()
    }

    fun decode(payload: String): RecipeUiModel? = runCatching {
        val json = gunzip(Base64.decode(payload, Base64.NO_WRAP)).toString(Charsets.UTF_8)
        fromJson(json)
    }.getOrNull()

    private fun fromJson(json: String): RecipeUiModel? = runCatching {
        val obj = JSONObject(json)
        if (obj.optString("f") != FORMAT) return@runCatching null
        RecipeUiModel(
            slot = "",
            name = obj.optString("n").trim().ifBlank { "QR Import" },
            sim = obj.optString("s").trim().ifBlank { "Provia / Standard" },
            description = obj.optString("d"),
            pills = obj.optJSONArray("p")?.toStringList().orEmpty(),
            effects = obj.optJSONObject("e")?.toStringMap()
                ?.entries?.associate { (k, v) -> (EK_DEC[k] ?: k) to (EV_DEC[v] ?: v) }
                .orEmpty(),
            tone = obj.optJSONObject("t")?.toStringMap()
                ?.entries?.associate { (k, v) -> (TK_DEC[k] ?: k) to v }
                .orEmpty(),
            wb = obj.optJSONObject("w")?.toStringMap()
                ?.entries?.associate { (k, v) -> (WK_DEC[k] ?: k) to (WV_DEC[v] ?: v) }
                .orEmpty(),
            isoMin = obj.optInt("iMin", -1).takeIf { it >= 0 },
            isoMax = obj.optInt("iMax", -1).takeIf { it >= 0 },
            exposureCompMin = obj.optDouble("ecMin", Double.NaN).takeIf { !it.isNaN() }?.toFloat(),
            exposureCompMax = obj.optDouble("ecMax", Double.NaN).takeIf { !it.isNaN() }?.toFloat(),
            sensorGens = obj.optJSONArray("sg")?.let { arr -> (0 until arr.length()).map { arr.getInt(it) } } ?: emptyList(),
        )
    }.getOrNull()

    fun createBitmap(payload: String, sizePx: Int = 960): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0,
        )
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        return matrix.toBitmap()
    }

    fun decodeBitmap(context: Context, uri: Uri): RecipeUiModel? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null
        // On FujiSync share cards the QR is always right-aligned at ~30% of card width.
        // Try full image first, then crop progressively tighter from the right edge.
        return tryDecodeBitmap(bitmap)
            ?: tryDecodeBitmap(bitmap.cropRight(0.50f))
            ?: tryDecodeBitmap(bitmap.cropRight(0.35f))
    }

    private fun tryDecodeBitmap(bitmap: Bitmap): RecipeUiModel? = runCatching {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val result = MultiFormatReader().decode(
            BinaryBitmap(HybridBinarizer(source)),
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.CHARACTER_SET to "UTF-8",
                DecodeHintType.TRY_HARDER to true,
            ),
        )
        decode(result.text)
    }.getOrNull()

    // Crop the right `fraction` of the image at full height — QR is always right-aligned.
    private fun Bitmap.cropRight(fraction: Float): Bitmap {
        val cropW = (width * fraction).toInt().coerceAtLeast(1)
        return Bitmap.createBitmap(this, width - cropW, 0, cropW, height)
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
        val full = PlanarYUVLuminanceSource(data, dataWidth, dataHeight, 0, 0, safeWidth, safeHeight, false)
        val center = PlanarYUVLuminanceSource(data, dataWidth, dataHeight, cropLeft, cropTop, cropSize, cropSize, false)
        return decodeLuminanceSource(center) ?: decodeLuminanceSource(full)
    }

    private fun decodeLuminanceSource(source: com.google.zxing.LuminanceSource): RecipeUiModel? {
        val reader = MultiFormatReader().apply { setHints(qrDecodeHints()) }
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

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPInputStream(data.inputStream()).use { it.copyTo(out) }
        return out.toByteArray()
    }

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
