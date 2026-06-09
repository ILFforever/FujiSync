package com.paeki.fujirecipes.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
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
/** Full ML Kit result plus image width — passed from [OcrRecipeParser.recognizeText] to [OcrRecipeParser.parse]. */
data class OcrInput(
    val mlText: Text,
    val imageWidth: Int,
)

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

    // ── Hoisted regex constants ────────────────────────────────────────────────
    private val SIGN_ONE_RE        = Regex("""([+\-])[|lI\[!]""")
    private val TYPO_STRRONG_RE    = Regex("""(?i)\bstrrong\b""")
    private val TYPO_BOALANCE_RE   = Regex("""(?i)\bboalance\b""")
    private val TYPO_EFECT_RE      = Regex("""(?i)\befect\b""")
    private val TYPO_GOLOUR_RE     = Regex("""(?i)\bgolour\b""")
    private val TYPO_GHOME_RE      = Regex("""(?i)\bghome\b""")
    private val TYPO_GURVE_RE      = Regex("""(?i)\bgurve\b""")
    private val TYPO_HIGHILIGHTS_RE = Regex("""(?i)\bhighilights\b""")
    private val TYPO_NAJSE_RE      = Regex("""(?i)\bnajse\b""")
    private val TYPO_IS0_RE        = Regex("""(?i)\bis0\b""")
    private val TYPO_DRI_RE        = Regex("""(?i)\bdri(?=\d{2}\b)""")
    private val TYPO_REUGHNESS_RE  = Regex("""(?i)\breughness\b""")
    private val TYPO_SIZ2_RE       = Regex("""(?i)\bsiz2\b""")
    private val TYPO_RAIN_RE       = Regex("""(?im)^rain\s*:""")
    private val TYPO_LARITY_RE     = Regex("""(?im)^larity\s*:""")
    private val BULLET_PLUS_RE     = Regex("""(:\s*)•(?=\d)""")
    private val O_ZERO_RE          = Regex("""(?i)(:\s*)o(?=0\b)""")
    private val LONE_LI_RE         = Regex("""\b[lI]\b""")
    private val LONE_O_RE          = Regex("""\bO\b""")
    private val CL_HINT_RE         = Regex("""\bCL\b""")
    private val DR_HINT_RE         = Regex("""(?i)\bDR\d{3}\b""")
    private val DRP_HINT_RE        = Regex("""(?i)\bDRP\b""")
    private val CC_HINT_RE         = Regex("""(?i)(?<!\w)cc\s*[:\s]""")
    private val COL_HINT_RE        = Regex("""\bCOL?\b""")
    private val WB_HINT_RE         = Regex("""(?<!\w)WB[\s:]""")
    private val WHITESPACE_RE      = Regex("""\s+""")
    private val NON_ALNUM_RE       = Regex("""[^a-z0-9]+""")

    /** Run ML Kit OCR on [uri] and return text with spatial metadata. */
    suspend fun recognizeText(context: Context, uri: Uri): OcrInput {
        val image = InputImage.fromFilePath(context, uri)
        val imageWidth = image.width
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(OcrInput(result, imageWidth)) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    /** Parse using bounding-box geometry for column detection. */
    fun parse(input: OcrInput): OcrParseResult? =
        parseInternal(input.mlText.text, geometricStitch(input.mlText.textBlocks, input.imageWidth))

    /** Parse from raw text only — geometry-unaware fallback used in tests. */
    fun parse(rawText: String): OcrParseResult? = parseInternal(rawText, null)

    private fun parseInternal(rawText: String, geomStitched: String?): OcrParseResult? {
        if (rawText.isBlank()) return null

        // Normalize dashes/minuses so regexes only need to handle ASCII minus.
        // Also collapse vertical-bar separators (│, |, ┃) to colon — some screenshot
        // layouts use a long vertical rule between label and value that OCR reads as |.
        val normalizedText = rawText
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            // Sign-attached confusables for '1' — must run before '|' → ':' conversion
            // so that "+|" is caught as "+1" rather than becoming "+:" and failing to parse.
            .replace(SIGN_ONE_RE, "${"$"}11")
            .replace('|', ':')
            .replace('│', ':')
            .replace('┃', ':')
            .replace(']', '1')  // OCR sometimes reads '1' as ']' in certain fonts
            .replace('İ', 'l')
            .replace('ļ', ' ')
            .replace(TYPO_STRRONG_RE, "strong")
            .replace(TYPO_BOALANCE_RE, "balance")
            .replace(TYPO_EFECT_RE, "effect")
            .replace(TYPO_GOLOUR_RE, "colour")
            .replace(TYPO_GHOME_RE, "chrome")
            .replace(TYPO_GURVE_RE, "curve")
            .replace(TYPO_HIGHILIGHTS_RE, "highlights")
            .replace(TYPO_NAJSE_RE, "noise")
            .replace(TYPO_IS0_RE, "iso")
            .replace(TYPO_DRI_RE, "DR1")
            .replace(TYPO_REUGHNESS_RE, "roughness")
            .replace(TYPO_SIZ2_RE, "size ")
            .replace(TYPO_RAIN_RE, "Grain Effect:")
            .replace(TYPO_LARITY_RE, "Clarity:")
            .replace(BULLET_PLUS_RE, "${"$"}1+")
            .replace(O_ZERO_RE, "${"$"}10")
            // Standalone whole-word confusables (word boundaries ensure we don't mangle
            // letters inside words like "Small", "Classic", "Off", "Color", etc.)
            .replace(LONE_LI_RE, "1")  // 'l' or 'I' as whole word → '1'
            .replace(LONE_O_RE, "0")   // capital 'O' as whole word → '0'

        // Prefer geometric column stitching (bounding-box based); fall back to the
        // text-heuristic stitcher for cases where geometry isn't available.
        val normalizedLines = normalizedText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val stitched = geomStitched
            ?: stitchFujiRecipeOrder(normalizedLines)
            ?: stitchColumnLayout(normalizedLines)
        val text = if (stitched != null) "$stitched\n$normalizedText" else normalizedText

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
        val dRangePriority = track("D Range Priority", parseDRangePriority(text), hasDRangePriorityHint(text))

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
        val clarity = track("Clarity",        parseClarity(text),   hasHint(text, "clarity") || CL_HINT_RE.containsMatchIn(text))

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
                put("D Range Priority",     dRangePriority ?: "Off")
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
            "Nostalgic Negative"    to FujiFilmSimulation.NostalgicNeg.label,
            "Classic Chrome"        to FujiFilmSimulation.ClassicChrome.label,
            "Classic Neg"           to FujiFilmSimulation.ClassicNeg.label,
            "Classic Negative"      to FujiFilmSimulation.ClassicNeg.label,
            "Pro Neg Hi"            to FujiFilmSimulation.ProNegHi.label,
            "Pro Neg. Hi"           to FujiFilmSimulation.ProNegHi.label,
            "Pro Neg Std"           to FujiFilmSimulation.ProNegStd.label,
            "Pro Neg. Std"          to FujiFilmSimulation.ProNegStd.label,
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
        return fuzzyFilmSimulation(text, aliases)
    }

    private val FILM_SIM_VALUE_RE = Regex("""(?i)\b(?:film\s*sim(?:ulation)?|base\s+simulation|simulation)\s*[:\-]\s*([^\n\r]{2,40})""")

    private fun fuzzyFilmSimulation(text: String, aliases: List<Pair<String, String>>): String? {
        val candidate = FILM_SIM_VALUE_RE.find(text)?.groupValues?.get(1)?.trim() ?: return null
        val best = aliases
            .map { (alias, label) -> label to similarity(candidate.normalizedToken(), alias.normalizedToken()) }
            .maxByOrNull { it.second }
        return best?.takeIf { it.second >= 0.72f }?.first
    }

    // ── Dynamic range ──────────────────────────────────────────────────────────

    private val DR_RE      = Regex("""(?i)\bDR[\s\-_]*([124][0O]{2}|auto)\b""")
    private val DR_LONG_RE = Regex("""(?i)dynamic\s+range[\s:]+\b([124][0O]{2}|auto)\b""")

    private fun parseDynamicRange(text: String): String? {
        val raw = (DR_RE.find(text) ?: DR_LONG_RE.find(text))?.groupValues?.get(1)?.replace('O', '0') ?: return null
        return when (raw.uppercase()) {
            "100"  -> "DR100%"
            "200"  -> "DR200%"
            "400"  -> "DR400%"
            "AUTO" -> "DR Auto"
            else   -> null
        }
    }

    private fun hasDrHint(text: String) =
        DR_HINT_RE.containsMatchIn(text) ||
        text.contains("dynamic range", ignoreCase = true)

    private val DR_PRIORITY_RE = Regex(
        """(?i)\b(?:d\s*range|dynamic\s+range|dr)\s*priority\s*[:\-_ ]*\s*\b(off|weak|strong|auto)\b""",
    )
    private val DR_PRIORITY_SHORT_RE = Regex("""(?i)\bDRP\s*[:\-_ ]*\s*\b(off|weak|strong|auto)\b""")

    private fun parseDRangePriority(text: String): String? {
        val raw = (DR_PRIORITY_RE.find(text) ?: DR_PRIORITY_SHORT_RE.find(text))
            ?.groupValues
            ?.get(1)
            ?: return null
        return raw.toPriorityLabel()
    }

    private fun hasDRangePriorityHint(text: String) =
        text.contains("d range priority", ignoreCase = true) ||
            text.contains("dynamic range priority", ignoreCase = true) ||
            DRP_HINT_RE.containsMatchIn(text)

    // ── Grain effect ────────────────────────────────────────────────────────────

    private val GRAIN_OFF_RE      = Regex("""(?i)\bgrain(?:\s+effect)?(?:[\s-]+(?:roughness|size))?\s*[:\s/]+off\b""")
    private val GRAIN_FULL_RE     = Regex("""(?i)\bgrain(?:\s+effect)?(?:[\s-]+roughness)?\s*[:\s/(]+(weak|strong)\b[\s\S]{0,60}?\b(small|large)\b""")
    private val GRAIN_STRENGTH_RE = Regex("""(?i)\bgrain(?:\s+effect)?(?:[\s-]+roughness)?\s*[:\s/]+(off|weak|strong)\b""")
    private val GRAIN_SIZE_RE     = Regex("""(?i)(?:grain\s+effect[\s-]*)?size\s*[:\s/]*(small|large)\b""")

    private fun parseGrainEffect(text: String): String? {
        GRAIN_FULL_RE.find(text)?.let { m ->
            val strength = m.groupValues[1].lowercase().replaceFirstChar { it.uppercaseChar() }
            val size     = m.groupValues[2].lowercase().replaceFirstChar { it.uppercaseChar() }
            return "$strength $size"
        }
        if (GRAIN_OFF_RE.containsMatchIn(text)) return "Off"
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
        """(?i)(?:colou?r\s+chrome\s+(?:effect\s+)?(?:(?:fx\s*)?blue)|colou?r\s+fx\s*-\s*blue|cc\s+(?:fx\s+)?blue|fx\s*-\s*blue|fx\s+blue)\s*[:\s/]*(off|weak|strong)"""
    )
    private val CC_RE = Regex(
        """(?i)(?:colou?r\s+chrome(?:\s+(?:effect|fx))?(?!\s+blue)|(?<!\w)cc(?!\s+(?:fx\s+)?blue))\s*[:\s/]*(off|weak|strong)"""
    )
    private val SKIN_RE = Regex("""(?i)smooth[\s-]+skin(?:\s+effect)?\s*[:\s/]*(off|weak|strong)""")

    private fun parseCcFxBlue(text: String)   = parseOws(CC_BLUE_RE, text)
    private fun parseColorChrome(text: String) = parseOws(CC_RE, text)
    private fun parseSmoothSkin(text: String)  = parseOws(SKIN_RE, text)

    private fun hasCcHint(text: String)     = text.contains("color chrome", ignoreCase = true) ||
        CC_HINT_RE.containsMatchIn(text)
    private fun hasCcBlueHint(text: String) = text.contains("blue", ignoreCase = true) &&
        text.contains("chrome", ignoreCase = true)

    // ── Highlight Tone ─────────────────────────────────────────────────────────
    // Long form: "Highlight Tone: -2" or "Highlight: -2"
    // Abbreviated: "H -2", "HL -2", "H-2", "HL+1"

    private val HL_LONG_RE   = Regex("""(?i)\bhighlights?(?:\s+tone)?\s*[:\s/()]*([+-]?\d+(?:[.,]\d+)?)""")
    private val HL_ABBREV_RE = Regex("""(?i)\bHL?\b\s*[:\s]*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseHighlight(text: String): String? =
        (HL_LONG_RE.find(text) ?: HL_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.let { formatDial(it) }

    // ── Shadow Tone ────────────────────────────────────────────────────────────
    // Long form: "Shadow Tone: +1" or "Shadow: +1"
    // Abbreviated: "S +1", "SH +1", "S+1", "SH-2"
    // Note: SH = Shadow in Fuji convention, NOT Sharpness.

    private val SH_LONG_RE   = Regex("""(?i)\bshadows?(?:\s+tone)?\s*[:\s/()]*([+-]?\d+(?:[.,]\d+)?)""")
    private val SH_ABBREV_RE = Regex("""(?i)\bSH?\b\s*[:\s]*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseShadow(text: String): String? =
        (SH_LONG_RE.find(text) ?: SH_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.let { formatDial(it) }

    // ── Color (saturation) ────────────────────────────────────────────────────
    // Must not match Color Chrome or Color Temperature.
    // Abbreviated: "C +2", "COL +2", "C+2"

    private val COLOR_LONG_RE   = Regex("""(?i)\bcolou?r(?!\s*(?:chrome|temp(?:erature)?))\s*[:\s/]*([+-]?\d+(?:[.,]\d+)?)""")
    private val COLOR_ABBREV_RE = Regex("""(?i)\bCOL?\b\s*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseColor(text: String): String? =
        (COLOR_LONG_RE.find(text) ?: COLOR_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.let { formatDial(it) }

    private fun hasColorHint(text: String) = COLOR_LONG_RE.containsMatchIn(text) ||
        COL_HINT_RE.containsMatchIn(text)

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

    private val NR_LONG_RE   = Regex("""(?i)(?:high[\s-]+iso[\s-]+(?:nr|noise\s+reduction)|noise\s+reduction)[ \t:/]*([+-]?\d+(?:[.,]\d+)?)""")
    private val NR_ABBREV_RE = Regex("""(?i)\bNR\b[ \t]*([+-]?\d+(?:[.,]\d+)?)""")

    private fun parseHighIsoNr(text: String): String? =
        (NR_LONG_RE.find(text) ?: NR_ABBREV_RE.find(text))
            ?.groupValues?.get(1)?.replace(',', '.')
            ?.toFloatOrNull()?.toInt()?.let { formatInt(it) }

    // ── Clarity ────────────────────────────────────────────────────────────────
    // Long form: "Clarity: -2"
    // Abbreviated: "CL -2", "CLAR -2", "CL+1"

    private val CLAR_LONG_RE   = Regex("""(?i)\bclarity[ \t:/]*([+-]?\d+(?:[.,]\d+)?)""")
    private val CLAR_ABBREV_RE = Regex("""(?i)\bCLAR?\b[ \t]*([+-]?\d+(?:[.,]\d+)?)""")

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
    // Two forms: "R+4" / "R 4" (channel before value) and "+4 Red" (value before name).
    private val WB_SHIFT_TOKEN = """(?:[+t-]?\d|[+-]s)"""
    private val WB_INLINE_R_RE      = Regex("""(?i)\bR\s*[:/]?\s*($WB_SHIFT_TOKEN)""")
    private val WB_INLINE_B_RE      = Regex("""(?i)\bB\s*[:/]?\s*($WB_SHIFT_TOKEN)""")
    private val WB_INLINE_R_POST_RE = Regex("""(?i)($WB_SHIFT_TOKEN)\s+red\b""")
    private val WB_INLINE_B_POST_RE = Regex("""(?i)($WB_SHIFT_TOKEN)\s+blue\b""")

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
            // Look for R/B shifts on the same line (up to 50 chars after the match).
            val afterLabel = text.substring(labelMatch.range.last).take(50)
            wbR = inlineShift(afterLabel, WB_INLINE_R_RE, WB_INLINE_R_POST_RE)
            wbB = inlineShift(afterLabel, WB_INLINE_B_RE, WB_INLINE_B_POST_RE)
        }

        // Try temperature match if no label found.
        if (wbDisplay == null) {
            WB_TEMP_RE.find(text)?.let { m ->
                wbDisplay = "${m.groupValues[1]}K"
                val afterTemp = text.substring(m.range.last).take(50)
                wbR = inlineShift(afterTemp, WB_INLINE_R_RE, WB_INLINE_R_POST_RE)
                wbB = inlineShift(afterTemp, WB_INLINE_B_RE, WB_INLINE_B_POST_RE)
            }
        }

        return Triple(wbDisplay, wbR, wbB)
    }

    private fun hasWbHint(text: String) =
        text.contains("white balance", ignoreCase = true) ||
        WB_HINT_RE.containsMatchIn(text)

    // ── Explicit WB shift lines ────────────────────────────────────────────────
    // "WB Shift R: +3" / "WB Shift Blue: -1"

    private val WB_R_RE = Regex("""(?i)(?:wb\s*(?:shift\s*)?r(?:ed)?|red)\s*[:\s/]*($WB_SHIFT_TOKEN)""")
    private val WB_B_RE = Regex("""(?i)(?:wb\s*(?:shift\s*)?b(?:lue)?|blue)\s*[:\s/]*($WB_SHIFT_TOKEN)""")
    private val WB_SHIFT_WINDOW_RE = Regex("""(?i)wb\s*shift[\s:/-]*([\s\S]{0,80})""")

    private fun parseWbShiftLines(text: String): Pair<String?, String?> {
        val shiftWindow = WB_SHIFT_WINDOW_RE.find(text)?.groupValues?.get(1)
        val r = WB_R_RE.find(text)?.groupValues?.get(1)?.let { parseWbShiftValue(it) }?.let { formatInt(it) }
            ?: shiftWindow?.let { inlineShift(it, WB_INLINE_R_RE, WB_INLINE_R_POST_RE) }
        val b = WB_B_RE.find(text)?.groupValues?.get(1)?.let { parseWbShiftValue(it) }?.let { formatInt(it) }
            ?: shiftWindow?.let { inlineShift(it, WB_INLINE_B_RE, WB_INLINE_B_POST_RE) }
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

    // ── Geometric column stitching ────────────────────────────────────────────
    // Detects two-column layouts using ML Kit bounding boxes instead of label-name
    // heuristics.  Finds the x-gap between left and right clusters, then pairs each
    // left block with the nearest right block by y-centre.  Language-agnostic: works
    // on any layout as long as labels are spatially to the left of values.

    private fun geometricStitch(blocks: List<Text.TextBlock>, imageWidth: Int): String? {
        if (blocks.size < 6 || imageWidth <= 0) return null

        // Find the split point: look for the largest gap in the sorted x-centre distribution.
        val xCentres = blocks.mapNotNull { it.boundingBox?.centerX() }
        if (xCentres.size < 4) return null
        val sortedX = xCentres.sorted()

        var maxGap = 0
        var splitX = imageWidth / 2
        for (i in 0 until sortedX.size - 1) {
            val gap = sortedX[i + 1] - sortedX[i]
            if (gap > maxGap) { maxGap = gap; splitX = (sortedX[i] + sortedX[i + 1]) / 2 }
        }

        // Require the gap to be at least 15% of image width — rejects single-column layouts.
        if (maxGap < imageWidth * 0.15) return null

        val leftBlocks  = blocks.filter { (it.boundingBox?.centerX() ?: Int.MAX_VALUE) < splitX }
                                .sortedBy { it.boundingBox?.top ?: 0 }
        val rightBlocks = blocks.filter { (it.boundingBox?.centerX() ?: 0) >= splitX }
                                .sortedBy { it.boundingBox?.top ?: 0 }

        if (leftBlocks.size < 3 || rightBlocks.size < 3) return null

        // Greedily pair left blocks with the nearest right block by y-centre.
        val result = StringBuilder()
        var ri = 0
        var pairs = 0
        for (leftBlock in leftBlocks) {
            val leftY = leftBlock.boundingBox?.centerY() ?: continue
            // Advance ri toward the right block closest in y to leftY.
            while (ri + 1 < rightBlocks.size) {
                val cur = abs((rightBlocks[ri    ].boundingBox?.centerY() ?: 0) - leftY)
                val nxt = abs((rightBlocks[ri + 1].boundingBox?.centerY() ?: 0) - leftY)
                if (nxt < cur) ri++ else break
            }
            val rightBlock = rightBlocks.getOrNull(ri) ?: continue
            val rightY     = rightBlock.boundingBox?.centerY() ?: continue
            val rowHeight  = (leftBlock.boundingBox?.height() ?: 40).coerceAtLeast(20)
            if (abs(rightY - leftY) > rowHeight * 2.5) continue  // too far apart — skip
            result.appendLine("${leftBlock.text.trim()}: ${rightBlock.text.trim()}")
            pairs++
        }

        return if (pairs >= 3) result.toString().trim() else null
    }

    // ── Text-heuristic column stitching (fallback) ────────────────────────────
    // When a screenshot has a visual rule separating labels (left column) from
    // values (right column), OCR emits them as separate blocks.  We detect ≥3
    // label-only lines, then semantically assign each the next valid value
    // candidate — skipping noise lines like engagement counts ("89%", "260").

    private val LABEL_CANON = mapOf(
        "film sim"                 to "Film Simulation",
        "film simulation"          to "Film Simulation",
        "grain"                    to "Grain Effect",
        "grain effect"             to "Grain Effect",
        "grain roughness"          to "Grain Effect",
        "color chrome"             to "Color Chrome",
        "colour chrome"            to "Color Chrome",
        "color chrome effect"      to "Color Chrome",
        "colour chrome effect"     to "Color Chrome",
        "color fx-blue"            to "Color Chrome FX Blue",
        "color fx- blue"           to "Color Chrome FX Blue",
        "color fx blue"            to "Color Chrome FX Blue",
        "colour fx-blue"           to "Color Chrome FX Blue",
        "colour fx blue"           to "Color Chrome FX Blue",
        "color chrome fx blue"     to "Color Chrome FX Blue",
        "colour chrome fx blue"    to "Color Chrome FX Blue",
        "cc fx blue"               to "Color Chrome FX Blue",
        "fx blue"                  to "Color Chrome FX Blue",
        "wb"                       to "White Balance",
        "white balance"            to "White Balance",
        "red"                      to "WB Shift R",
        "wb r"                     to "WB Shift R",
        "wb red"                   to "WB Shift R",
        "blue"                     to "WB Shift B",
        "wb b"                     to "WB Shift B",
        "wb blue"                  to "WB Shift B",
        "dynamic"                  to "Dynamic Range",
        "dynamic range"            to "Dynamic Range",
        "d range priority"         to "D Range Priority",
        "dynamic range priority"   to "D Range Priority",
        "dr priority"              to "D Range Priority",
        "drp"                      to "D Range Priority",
        "highlights"               to "Highlight Tone",
        "highlight"                to "Highlight Tone",
        "highlight tone"           to "Highlight Tone",
        "hl"                       to "Highlight Tone",
        "shadows"                  to "Shadow Tone",
        "shadow"                   to "Shadow Tone",
        "shadow tone"              to "Shadow Tone",
        "sh"                       to "Shadow Tone",
        "color"                    to "Color",
        "colour"                   to "Color",
        "col"                      to "Color",
        "sharpness"                to "Sharpness",
        "sharp"                    to "Sharpness",
        "shp"                      to "Sharpness",
        "high iso-nr"              to "High ISO NR",
        "high is0-nr"              to "High ISO NR",
        "high iso nr"              to "High ISO NR",
        "high iso noise reduction" to "High ISO NR",
        "noise reduction"          to "High ISO NR",
        "iso nr"                   to "High ISO NR",
        "nr"                       to "High ISO NR",
        "clarity"                  to "Clarity",
        "smooth skin"              to "Smooth Skin",
    )

    private val FUJI_ORDERED_SPLIT_LABELS = listOf(
        "Film Simulation",
        "Grain Effect",
        "Color Chrome",
        "Color Chrome FX Blue",
        "White Balance",
        "WB Shift R",
        "WB Shift B",
        "D Range Priority",
        "Dynamic Range",
        "Highlight Tone",
        "Shadow Tone",
        "Color",
        "Sharpness",
        "High ISO NR",
        "Clarity",
    )

    private fun stitchFujiRecipeOrder(lines: List<String>): String? {
        val orderedLabels = lines.mapIndexedNotNull { i, line ->
            LABEL_CANON[line.trimEnd(':').trim().lowercase()]?.let { i to it }
        }
        val presentLabels = orderedLabels
            .map { it.second }
            .toSet()
        if (presentLabels.size < 6) return null

        val valueLines = recipeValueLines(lines)
        val candidates = valueLines
            .mapIndexed { i, line -> i to line }
            .filterNot { (_, line) -> LABEL_CANON.containsKey(line.trimEnd(':').trim().lowercase()) }
            .filterNot { (_, line) ->
                DIRECT_VALUE_LINE_RE.containsMatchIn(line) ||
                    line.contains("add comment", ignoreCase = true) ||
                    line.contains("follow", ignoreCase = true)
            }
            .filter { (_, line) -> line.length <= 60 }

        val localPairCount = orderedLabels.count { (labelIndex, label) ->
            val nextLabelIndex = orderedLabels
                .firstOrNull { it.first > labelIndex }
                ?.first
                ?: valueLines.size
            candidates.any { (i, value) ->
                i > labelIndex && i < nextLabelIndex && isColumnValue(label, value)
            }
        }
        val preferLocalMatches = localPairCount >= 3

        val used = mutableSetOf<Int>()
        val result = StringBuilder()
        var pairs = 0

        for (label in FUJI_ORDERED_SPLIT_LABELS) {
            if (label !in presentLabels) continue
            val labelIndex = orderedLabels.firstOrNull { it.second == label }?.first ?: continue
            val nextLabelIndex = orderedLabels
                .firstOrNull { it.first > labelIndex }
                ?.first
                ?: valueLines.size
            val localMatch = selectColumnMatch(label, candidates, used, labelIndex, nextLabelIndex)
            val globalMatch =
                if (preferLocalMatches) null
                else selectColumnMatch(label, candidates, used)
            val match = localMatch ?: globalMatch ?: continue
            used += match.first
            result.appendLine("$label: ${columnValueFor(label, match.second)}")
            pairs++
        }

        return if (pairs >= 5) result.toString().trim() else null
    }

    private fun stitchColumnLayout(lines: List<String>): String? {
        val orderedLabels = mutableListOf<Pair<Int, String>>()
        for ((i, line) in lines.withIndex()) {
            val key = line.trimEnd(':').trim().lowercase()
            val canonical = LABEL_CANON[key] ?: continue
            orderedLabels.add(i to canonical)
        }
        if (orderedLabels.size < 3) return null

        val labelIndices = orderedLabels.map { it.first }.toSet()
        val valueLines = recipeValueLines(lines)
        val candidates = valueLines
            .mapIndexed { i, line -> i to line.trim() }
            .filter { (i, t) -> i !in labelIndices && t.isNotBlank() && t.length <= 60 }

        val result = StringBuilder()
        var cIdx = 0
        for ((_, labelName) in orderedLabels) {
            while (cIdx < candidates.size) {
                val value = candidates[cIdx].second
                cIdx++
                if (isColumnValue(labelName, value)) {
                    result.appendLine("$labelName: $value")
                    break
                }
            }
        }

        val stitched = result.toString().trim()
        return if (stitched.lines().count { it.isNotBlank() } >= 3) stitched else null
    }

    private val COL_DIAL_RE   = Regex("""^[+-]?[0-5](?:[.,]5)?$""")
    private val COL_WB_SHIFT_RE = Regex("""^[+-]?[0-9]$""")
    private val COL_DR_RE     = Regex("""(?i)^(dr\s*(100|200|400|auto)|\d{3}%)$""")
    private val COL_DR_PRIORITY_RE = Regex("""(?i)^(off|weak|strong|auto)$""")
    private val COL_GRAIN_RE  = Regex("""(?i)^(off|(weak|strong)(?:\s*[,/]\s*|\s+)(small|large)?)$""")
    private val COL_GRAIN_WITH_SIZE_RE = Regex("""(?i)^(weak|strong)(?:\s*[,/]\s*|\s+)(small|large)$""")
    private val COL_TOGGLE_RE = Regex("""(?i)^(off|weak|strong)$""")
    private val COL_TOGGLE_EFFECT_RE = Regex("""(?i)^effect\s+(off|weak|strong)$""")
    private val COL_WB_RE     = Regex("""(?i)(auto|daylight|shade|incandescent|underwater|fluorescent|\d{4,5}\s*k)""")
    private val DIRECT_VALUE_LINE_RE = Regex(
        """(?i)^(?:color\s+chrome\s*:\s*fx\s+blue|white\s+balance\s*:|exp\.\s*compensation\s*:|iso\s+auto\s*,?\s*:?)"""
    )

    private fun isEngagementFooterStart(line: String): Boolean =
        line.equals("likes", ignoreCase = true)

    private fun recipeValueLines(lines: List<String>): List<String> {
        val likesIndex = lines.indexOfFirst { isEngagementFooterStart(it) }
        val hardEnd = if (likesIndex >= 0) likesIndex else lines.size
        val followIndex = lines.indexOfFirst { it.equals("follow", ignoreCase = true) }
        if (followIndex in 0 until hardEnd) {
            val afterFollow = lines.subList(followIndex + 1, hardEnd)
            val hasRecipeValuesAfterFollow = afterFollow.any { line ->
                val t = line.trim()
                COL_GRAIN_WITH_SIZE_RE.matches(t) ||
                    COL_DR_RE.matches(t) ||
                    COL_WB_RE.containsMatchIn(t) ||
                    t.contains("red", ignoreCase = true) ||
                    t.contains("blue", ignoreCase = true)
            }
            if (!hasRecipeValuesAfterFollow) return lines.take(followIndex)
        }
        return lines.take(hardEnd)
    }

    private fun selectColumnMatch(
        label: String,
        candidates: List<Pair<Int, String>>,
        used: Set<Int>,
        afterIndex: Int = -1,
        beforeIndex: Int = Int.MAX_VALUE,
    ): Pair<Int, String>? {
        val matches = candidates.filter { (i, value) ->
            i !in used && i > afterIndex && i < beforeIndex && isColumnValue(label, value)
        }
        return when (label) {
            "Grain Effect" -> matches.firstOrNull { (_, value) ->
                COL_GRAIN_WITH_SIZE_RE.matches(value.trim())
            } ?: matches.firstOrNull()
            else -> matches.firstOrNull()
        }
    }

    private fun isColumnValue(label: String, value: String): Boolean {
        val t = value.trim()
        // Dot-normalized form for film sim name matching ("Pro Neg. Hi" → "Pro Neg Hi").
        val tNorm = t.replace(".", " ").replace(WHITESPACE_RE, " ").trim()
        return when (label) {
            "Film Simulation" ->
                FujiFilmSimulation.entries.any { sim ->
                    val labelNorm = sim.label.replace(".", " ").replace(WHITESPACE_RE, " ")
                    tNorm.contains(labelNorm, ignoreCase = true)
                } ||
                listOf("Bleach Bypass", "Nostalgic Neg", "Classic Chrome", "Classic Neg",
                       "Pro Neg Hi", "Pro Neg Std", "Reala Ace").any { tNorm.contains(it, ignoreCase = true) }
            "Grain Effect"                             -> COL_GRAIN_RE.matches(t)
            "Color Chrome", "Color Chrome FX Blue",
            "Smooth Skin"                              -> COL_TOGGLE_RE.matches(t) || COL_TOGGLE_EFFECT_RE.matches(t)
            "D Range Priority"                         -> COL_DR_PRIORITY_RE.matches(t)
            "Dynamic Range"                            -> COL_DR_RE.matches(t)
            "White Balance"                            -> COL_WB_RE.containsMatchIn(t)
            "WB Shift R", "WB Shift B"                  -> COL_WB_SHIFT_RE.matches(t)
            "Highlight Tone", "Shadow Tone",
            "Color", "Sharpness",
            "High ISO NR", "Clarity"                   -> COL_DIAL_RE.matches(t)
            else                                       -> false
        }
    }

    private fun columnValueFor(label: String, value: String): String =
        when (label) {
            "Color Chrome", "Color Chrome FX Blue", "Smooth Skin" ->
                COL_TOGGLE_EFFECT_RE.find(value.trim())?.groupValues?.get(1) ?: value
            "D Range Priority" -> value.toPriorityLabel() ?: value
            else -> value
        }

    // ── WB shift value parser ──────────────────────────────────────────
    // OCR sometimes reads '+' as 't' (e.g. "R t2" instead of "R+2").

    private fun inlineShift(text: String, channelFirst: Regex, valueFist: Regex): String? =
        (channelFirst.find(text)?.groupValues?.get(1) ?: valueFist.find(text)?.groupValues?.get(1))
            ?.let { parseWbShiftValue(it) }?.let { formatInt(it) }

    private fun parseWbShiftValue(raw: String): Int? =
        raw.trimStart().let { s ->
            if (s.startsWith('t', ignoreCase = true) && s.length > 1 && s[1].isDigit())
                s.substring(1).toIntOrNull()
            else if (s.endsWith('s', ignoreCase = true))
                s.dropLast(1).plus("5").toIntOrNull()
            else
                s.toIntOrNull()
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

    private fun String.toPriorityLabel(): String? = when (trim().lowercase()) {
        "off" -> "Off"
        "strong" -> "Strong"
        "weak" -> "Weak"
        "auto" -> "Auto"
        else -> null
    }

    private fun String.normalizedToken(): String =
        lowercase()
            .replace(NON_ALNUM_RE, "")

    private fun similarity(a: String, b: String): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val distance = levenshtein(a, b)
        return 1f - distance.toFloat() / maxOf(a.length, b.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(
                    prev[j] + 1,
                    cur[j - 1] + 1,
                    prev[j - 1] + cost,
                )
            }
            val tmp = prev
            prev = cur
            cur = tmp
        }
        return prev[b.length]
    }

    // ── Generic hint helper ────────────────────────────────────────────────────

    private fun hasHint(text: String, keyword: String) =
        text.contains(keyword, ignoreCase = true)
}
