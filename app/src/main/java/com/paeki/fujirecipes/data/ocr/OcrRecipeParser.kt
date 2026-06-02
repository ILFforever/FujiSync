package com.paeki.fujirecipes.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.paeki.fujirecipes.domain.model.FujiFilmSimulation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * On-device OCR import for film recipes from screenshots.
 *
 * Uses ML Kit Text Recognition (GMS, model downloads on first use).
 * Parsing is regex-based — Fujifilm's parameter vocabulary is finite and predictable.
 * [OcrParseResult.unmatchedFields] lists any fields where text was detected but the
 * value couldn't be parsed, so callers can warn the user by name rather than a count.
 */
data class OcrParseResult(
    val sim: String,
    val effects: Map<String, String>,
    val tone: Map<String, String>,
    val wb: Map<String, String>,
    val suggestedName: String,
    val matchedCount: Int,
    val unmatchedFields: List<String>,
)

object OcrRecipeParser {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /** Run ML Kit OCR on [uri] and return the raw extracted text. */
    suspend fun recognizeText(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    /**
     * Parse raw OCR text into a [OcrParseResult].
     * Returns null if no recognizable recipe fields were found at all.
     */
    fun parse(rawText: String): OcrParseResult? {
        if (rawText.isBlank()) return null

        // Normalize dashes/minuses so regexes only need to handle ASCII minus.
        val text = rawText
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')

        var matched = 0
        val unmatched = mutableListOf<String>()

        fun <T> track(fieldName: String, value: T?, hint: Boolean): T? {
            when {
                value != null -> matched++
                hint -> unmatched += fieldName
            }
            return value
        }

        // ── Film simulation ───────────────────────────────────────────────────
        val sim = track("Film Simulation", parseFilmSimulation(text), true) ?: "Provia / Standard"

        // ── Dynamic range ─────────────────────────────────────────────────────
        val dr = track("Dynamic Range", parseDynamicRange(text), hasDrHint(text))

        // ── Grain effect ──────────────────────────────────────────────────────
        val grain = track("Grain Effect", parseGrainEffect(text), hasHint(text, "grain"))

        // ── Color chrome (extract blue before plain CC) ───────────────────────
        val ccBlue = track("Color Chrome FX Blue", parseCcFxBlue(text), hasCcBlueHint(text))
        val cc     = track("Color Chrome", parseColorChrome(text), hasCcHint(text))

        // ── Smooth skin ───────────────────────────────────────────────────────
        val skin = track("Smooth Skin", parseSmoothSkin(text), hasHint(text, "smooth skin"))

        // ── Tonal adjustments ─────────────────────────────────────────────────
        val hl      = track("Highlight Tone", parseHighlight(text), hasHint(text, "highlight") || hasHint(text, "HL") || hasHint(text, " H ") || text.startsWith("H "))
        val sh      = track("Shadow Tone",    parseShadow(text),    hasHint(text, "shadow")    || hasHint(text, "SH") || hasHint(text, " S ") || text.startsWith("S "))
        val color   = track("Color",          parseColor(text),     hasColorHint(text))
        val sharp   = track("Sharpness",      parseSharpness(text), hasHint(text, "sharpness") || hasHint(text, "SHARP") || hasHint(text, "SHP"))
        val nr      = track("High ISO NR",    parseHighIsoNr(text), hasHint(text, "noise") || hasHint(text, "iso nr") || hasHint(text, " NR"))
        val clarity = track("Clarity",        parseClarity(text),   hasHint(text, "clarity") || Regex("""\bCL\b""").containsMatchIn(text))

        // ── White balance + inline shifts ────────────────────────────────────
        val (wbDisplay, inlineR, inlineB) = parseWhiteBalanceWithShifts(text)
        if (wbDisplay != null) matched++ else if (hasWbHint(text)) unmatched += "White Balance"

        // Explicit shift lines take priority; fall back to inline shifts.
        val (explicitR, explicitB) = parseWbShiftLines(text)
        val wbR = explicitR ?: inlineR
        val wbB = explicitB ?: inlineB
        if (wbR != null || wbB != null) matched++

        if (matched == 0) return null

        return OcrParseResult(
            sim = sim,
            effects = buildMap {
                put("Dynamic Range",        dr     ?: "DR Auto")
                put("Grain Effect",         grain  ?: "Off")
                put("Color Chrome",         cc     ?: "Off")
                put("Color Chrome FX Blue", ccBlue ?: "Off")
                put("Smooth Skin",          skin   ?: "Off")
            },
            tone = buildMap {
                put("Highlight Tone", hl      ?: "0")
                put("Shadow Tone",    sh      ?: "0")
                put("Color",          color   ?: "0")
                put("Sharpness",      sharp   ?: "0")
                put("High ISO NR",    nr      ?: "0")
                put("Clarity",        clarity ?: "0")
            },
            wb = buildMap {
                put("White Balance", wbDisplay ?: "Auto")
                put("WB Shift R",    wbR       ?: "0")
                put("WB Shift B",    wbB       ?: "0")
            },
            suggestedName   = suggestName(text),
            matchedCount    = matched,
            unmatchedFields = unmatched,
        )
    }

    // ── Film simulation ────────────────────────────────────────────────────────

    private fun parseFilmSimulation(text: String): String? {
        val aliases: List<Pair<String, String>> = listOf(
            "Eterna Bleach Bypass"  to FujiFilmSimulation.EternaBleachBypass.label,
            "Bleach Bypass"         to FujiFilmSimulation.EternaBleachBypass.label,
            "Nostalgic Neg"         to FujiFilmSimulation.NostalgicNeg.label,
            "Classic Chrome"        to FujiFilmSimulation.ClassicChrome.label,
            "Classic Neg"           to FujiFilmSimulation.ClassicNeg.label,
            "Pro Neg Hi"            to FujiFilmSimulation.ProNegHi.label,
            "Pro Neg Std"           to FujiFilmSimulation.ProNegStd.label,
            "Reala Ace"             to FujiFilmSimulation.RealaAce.label,
            "Monochrome + Y"        to FujiFilmSimulation.MonochromeY.label,
            "Monochrome + R"        to FujiFilmSimulation.MonochromeR.label,
            "Monochrome + G"        to FujiFilmSimulation.MonochromeG.label,
            "Monochrome+Y"          to FujiFilmSimulation.MonochromeY.label,
            "Monochrome+R"          to FujiFilmSimulation.MonochromeR.label,
            "Monochrome+G"          to FujiFilmSimulation.MonochromeG.label,
            "Acros + Y"             to FujiFilmSimulation.AcrosY.label,
            "Acros + R"             to FujiFilmSimulation.AcrosR.label,
            "Acros + G"             to FujiFilmSimulation.AcrosG.label,
            "Acros+Y"               to FujiFilmSimulation.AcrosY.label,
            "Acros+R"               to FujiFilmSimulation.AcrosR.label,
            "Acros+G"               to FujiFilmSimulation.AcrosG.label,
            "Acros"                 to FujiFilmSimulation.Acros.label,
            "Monochrome"            to FujiFilmSimulation.Monochrome.label,
            "Eterna"                to FujiFilmSimulation.Eterna.label,
            "Velvia"                to FujiFilmSimulation.Velvia.label,
            "Astia"                 to FujiFilmSimulation.Astia.label,
            "Provia"                to FujiFilmSimulation.Provia.label,
            "Sepia"                 to FujiFilmSimulation.Sepia.label,
        )
        for ((alias, label) in aliases) {
            if (text.contains(alias, ignoreCase = true)) return label
        }
        for (sim in FujiFilmSimulation.entries) {
            if (text.contains(sim.label, ignoreCase = true)) return sim.label
        }
        return null
    }

    // ── Dynamic range ──────────────────────────────────────────────────────────

    private val DR_RE      = Regex("""(?i)\bDR[\s\-_]*(100|200|400|auto)\b""")
    private val DR_LONG_RE = Regex("""(?i)dynamic\s+range[\s:]+\b(100|200|400|auto)\b""")

    private fun parseDynamicRange(text: String): String? {
        val raw = (DR_RE.find(text) ?: DR_LONG_RE.find(text))?.groupValues?.get(1) ?: return null
        return when (raw.uppercase()) {
            "100"  -> "DR100%"
            "200"  -> "DR200%"
            "400"  -> "DR400%"
            "AUTO" -> "DR Auto"
            else   -> null
        }
    }

    private fun hasDrHint(text: String) =
        Regex("""(?i)\bDR\d{3}\b""").containsMatchIn(text) ||
        text.contains("dynamic range", ignoreCase = true)

    // ── Grain effect ────────────────────────────────────────────────────────────

    private val GRAIN_OFF_RE      = Regex("""(?i)\bgrain(?:\s+(?:effect|roughness))?\s*[:\s/]+off\b""")
    private val GRAIN_FULL_RE     = Regex("""(?i)\bgrain(?:\s+(?:effect|roughness))?\s*[:\s/]+(weak|strong)\b.{0,30}?\b(small|large)\b""")
    private val GRAIN_STRENGTH_RE = Regex("""(?i)\bgrain(?:\s+(?:effect|roughness))?\s*[:\s/]+(off|weak|strong)\b""")
    private val GRAIN_SIZE_RE     = Regex("""(?i)(?:grain\s+)?size\s*[:\s/]*(small|large)\b""")

    private fun parseGrainEffect(text: String): String? {
        if (GRAIN_OFF_RE.containsMatchIn(text)) return "Off"
        GRAIN_FULL_RE.find(text)?.let { m ->
            val strength = m.groupValues[1].lowercase().replaceFirstChar { it.uppercaseChar() }
            val size     = m.groupValues[2].lowercase().replaceFirstChar { it.uppercaseChar() }
            return "$strength $size"
        }
        GRAIN_STRENGTH_RE.find(text)?.let { m ->
            val strength = m.groupValues[1].lowercase().replaceFirstChar { it.uppercaseChar() }
            if (strength == "Off") return "Off"
            val size = GRAIN_SIZE_RE.find(text)?.groupValues?.get(1)
                ?.lowercase()?.replaceFirstChar { it.uppercaseChar() } ?: "Small"
            return "$strength $size"
        }
        return null
    }

    // ── Off / Weak / Strong ─────────────────────────────────────────────────────

    private fun parseOws(re: Regex, text: String): String? =
        re.find(text)?.groupValues?.get(1)
            ?.lowercase()?.replaceFirstChar { it.uppercaseChar() }
            ?.takeIf { it in setOf("Off", "Weak", "Strong") }

    private val CC_BLUE_RE = Regex(
        """(?i)(?:color\s+chrome\s+(?:effect\s+)?(?:fx\s+)?blue|cc\s+(?:fx\s+)?blue|fx\s+blue)\s*[:\s/]*(off|weak|strong)"""
    )
    private val CC_RE = Regex(
        """(?i)(?:color\s+chrome(?:\s+effect)?(?!\s+(?:fx|blue))|(?<!\w)cc(?!\s+(?:fx\s+)?blue))\s*[:\s/]*(off|weak|strong)"""
    )
    private val SKIN_RE = Regex("""(?i)smooth[\s-]+skin\s*[:\s/]*(off|weak|strong)""")

    private fun parseCcFxBlue(text: String)   = parseOws(CC_BLUE_RE, text)
    private fun parseColorChrome(text: String) = parseOws(CC_RE, text)
    private fun parseSmoothSkin(text: String)  = parseOws(SKIN_RE, text)

    private fun hasCcHint(text: String)     = text.contains("color chrome", ignoreCase = true) ||
        Regex("""(?i)(?<!\w)cc\s*[:\s]""").containsMatchIn(text)
    private fun hasCcBlueHint(text: String) = text.contains("blue", ignoreCase = true) &&
        text.contains("chrome", ignoreCase = true)

    // ── Highlight Tone ─────────────────────────────────────────────────────────
    // Long form: "Highlight Tone: -2" or "Highlight: -2"
    // Abbreviated: "H -2", "HL -2", "H-2", "HL+1"

    private val HL_LONG_RE   = Regex("""(?i)\bhighlight(?:\s+tone)?\s*[:\s/]*([+-]?\d+(?:[.,]\d+)?)""")
    private val HL_ABBREV_RE = Regex("""(?i)\bHL?\b\s*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseHighlight(text: String): String? =
        (HL_LONG_RE.find(text) ?: HL_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.let { formatDial(it) }

    // ── Shadow Tone ────────────────────────────────────────────────────────────
    // Long form: "Shadow Tone: +1" or "Shadow: +1"
    // Abbreviated: "S +1", "SH +1", "S+1", "SH-2"
    // Note: SH = Shadow in Fuji convention, NOT Sharpness.

    private val SH_LONG_RE   = Regex("""(?i)\bshadow(?:\s+tone)?\s*[:\s/]*([+-]?\d+(?:[.,]\d+)?)""")
    private val SH_ABBREV_RE = Regex("""(?i)\bSH?\b\s*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseShadow(text: String): String? =
        (SH_LONG_RE.find(text) ?: SH_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.let { formatDial(it) }

    // ── Color (saturation) ────────────────────────────────────────────────────
    // Must not match Color Chrome or Color Temperature.
    // Abbreviated: "C +2", "COL +2", "C+2"

    private val COLOR_LONG_RE   = Regex("""(?i)\bcolor(?!\s*(?:chrome|temp(?:erature)?))\s*[:\s/]*([+-]?\d+(?:[.,]\d+)?)""")
    private val COLOR_ABBREV_RE = Regex("""(?i)\bCOL?\b\s*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseColor(text: String): String? =
        (COLOR_LONG_RE.find(text) ?: COLOR_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.let { formatDial(it) }

    private fun hasColorHint(text: String) = COLOR_LONG_RE.containsMatchIn(text) ||
        Regex("""\bCOL?\b""").containsMatchIn(text)

    // ── Sharpness ─────────────────────────────────────────────────────────────
    // Long form: "Sharpness: +2"
    // Abbreviated: "SHARP +2", "SHP +2", "SHP+2"
    // Note: NOT "SH" — that's Shadow in Fuji convention.

    private val SHARP_LONG_RE   = Regex("""(?i)\bsharpness\s*[:\s/]*([+-]?\d+(?:[.,]\d+)?)""")
    private val SHARP_ABBREV_RE = Regex("""(?i)\bSH(?:ARP|P)\b\s*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseSharpness(text: String): String? =
        (SHARP_LONG_RE.find(text) ?: SHARP_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.let { formatDial(it) }

    // ── High ISO NR ─────────────────────────────────────────────────────────────
    // Long form: "High ISO NR: -2", "Noise Reduction: 0"
    // Abbreviated: "NR -2", "NR+1"

    private val NR_LONG_RE   = Regex("""(?i)(?:high[\s-]+iso[\s-]+(?:nr|noise\s+reduction)|noise\s+reduction)\s*[:\s/]*([+-]?\d+(?:[.,]\d+)?)""")
    private val NR_ABBREV_RE = Regex("""(?i)\bNR\b\s*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseHighIsoNr(text: String): String? =
        (NR_LONG_RE.find(text) ?: NR_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.toInt()?.let { formatInt(it) }

    // ── Clarity ────────────────────────────────────────────────────────────────
    // Long form: "Clarity: -2"
    // Abbreviated: "CL -2", "CLAR -2", "CL+1"

    private val CLAR_LONG_RE   = Regex("""(?i)\bclarity\s*[:\s/]*([+-]?\d+(?:[.,]\d+)?)""")
    private val CLAR_ABBREV_RE = Regex("""(?i)\bCLAR?\b\s*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseClarity(text: String): String? =
        (CLAR_LONG_RE.find(text) ?: CLAR_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.let { formatDial(it) }

    // ── White balance + inline R/B shifts ─────────────────────────────────────
    //
    // Handles:
    //   "WB: Auto"
    //   "WB: Auto / R+3, B-3"        ← inline shifts after the label
    //   "Auto/ R 3, B-3"             ← shifts right after the WB name
    //   "5600K / R+2, B-1"           ← temperature with inline shifts
    //
    // Returns (wbDisplay, wbR, wbB) — any may be null.

    private val WB_LABEL_RE = Regex(
        """(?i)(?:white\s*balance|wb)\s*[:\s/]+(auto\s+white\s+priority|ambience\s+priority|daylight|incandescent|underwater|fluorescent\s*[123]|shade|auto)\b"""
    )
    private val WB_TEMP_RE = Regex("""(?i)(\d{4,5})\s*[kK]\b""")
    // Inline R and B shifts following the WB label on the same logical line.
    private val WB_INLINE_R_RE = Regex("""(?i)\bR\s*([+-]?\d+)""")
    private val WB_INLINE_B_RE = Regex("""(?i)\bB\s*([+-]?\d+)""")

    private fun parseWhiteBalanceWithShifts(text: String): Triple<String?, String?, String?> {
        var wbDisplay: String? = null
        var wbR: String? = null
        var wbB: String? = null

        // Try label match first.
        val labelMatch = WB_LABEL_RE.find(text)
        if (labelMatch != null) {
            val raw = labelMatch.groupValues[1].trim().lowercase()
            wbDisplay = when {
                "white priority" in raw -> "Auto White Priority"
                "ambience"       in raw -> "Ambience Priority"
                "daylight"       in raw -> "Daylight"
                "incandescent"   in raw -> "Incandescent"
                "underwater"     in raw -> "Underwater"
                "shade"          in raw -> "Shade"
                "fluorescent"    in raw -> "Fluorescent ${raw.filter { it.isDigit() }.take(1)}"
                "auto"           in raw -> "Auto"
                else                   -> null
            }
            // Look for R/B shifts on the same line (up to 30 chars after the match).
            val afterLabel = text.substring(labelMatch.range.last).take(50)
            wbR = WB_INLINE_R_RE.find(afterLabel)?.groupValues?.get(1)?.toIntOrNull()?.let { formatInt(it) }
            wbB = WB_INLINE_B_RE.find(afterLabel)?.groupValues?.get(1)?.toIntOrNull()?.let { formatInt(it) }
        }

        // Try temperature match if no label found.
        if (wbDisplay == null) {
            WB_TEMP_RE.find(text)?.let { m ->
                wbDisplay = "${m.groupValues[1]}K"
                val afterTemp = text.substring(m.range.last).take(50)
                wbR = WB_INLINE_R_RE.find(afterTemp)?.groupValues?.get(1)?.toIntOrNull()?.let { formatInt(it) }
                wbB = WB_INLINE_B_RE.find(afterTemp)?.groupValues?.get(1)?.toIntOrNull()?.let { formatInt(it) }
            }
        }

        return Triple(wbDisplay, wbR, wbB)
    }

    private fun hasWbHint(text: String) =
        text.contains("white balance", ignoreCase = true) ||
        Regex("""(?<!\w)WB[\s:]""").containsMatchIn(text)

    // ── Explicit WB shift lines ────────────────────────────────────────────────
    // "WB Shift R: +3" / "WB Shift Blue: -1"

    private val WB_R_RE = Regex("""(?i)wb\s*(?:shift\s*)?r(?:ed)?\s*[:\s/]*([+-]?\d+)""")
    private val WB_B_RE = Regex("""(?i)wb\s*(?:shift\s*)?b(?:lue)?\s*[:\s/]*([+-]?\d+)""")

    private fun parseWbShiftLines(text: String): Pair<String?, String?> {
        val r = WB_R_RE.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { formatInt(it) }
        val b = WB_B_RE.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { formatInt(it) }
        return r to b
    }

    // ── Suggested recipe name ─────────────────────────────────────────────────

    private val PARAM_KEYWORDS = setOf(
        "highlight", "shadow", "sharpness", "clarity", "grain", "chrome",
        "dynamic", "noise", "white balance", "color", "smooth", "eterna", "velvia",
        "provia", "astia", "acros", "monochrome", "sepia", "classic", "pro neg",
        "reala", "nostalgic",
    )

    private fun suggestName(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines) {
            if (line.length < 3 || line.length > 60) continue
            val lower = line.lowercase()
            if (PARAM_KEYWORDS.any { lower.contains(it) }) continue
            if (line.none { it.isLetter() }) continue
            if (line.all { it.isUpperCase() || !it.isLetter() }) continue
            return line.take(40)
        }
        return ""
    }

    // ── Format helpers ─────────────────────────────────────────────────────────

    private fun formatDial(f: Float): String {
        if (f == 0f) return "0"
        val prefix  = if (f > 0f) "+" else "-"
        val rounded = (kotlin.math.round(abs(f) * 10).toFloat() / 10f)
        return if (rounded % 1f == 0f) "$prefix${rounded.toInt()}" else "$prefix$rounded"
    }

    private fun formatInt(i: Int): String = when {
        i == 0 -> "0"
        i > 0  -> "+$i"
        else   -> "$i"
    }

    // ── Generic hint helper ────────────────────────────────────────────────────

    private fun hasHint(text: String, keyword: String) =
        text.contains(keyword, ignoreCase = true)
}
