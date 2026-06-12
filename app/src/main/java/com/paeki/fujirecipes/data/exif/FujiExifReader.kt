package com.paeki.fujirecipes.data.exif

import android.content.Context
import android.net.Uri
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.makernotes.FujifilmMakernoteDirectory
import com.paeki.fujirecipes.data.mapper.FujiValueMapper
import com.paeki.fujirecipes.data.ptp.MONO_SIM_CODES
import com.paeki.fujirecipes.domain.model.FujiFilmSimulation
import com.paeki.fujirecipes.domain.model.FujiPropertyCode

// Fujifilm MakerNote tags not yet named in metadata-extractor 2.19.0.
// Values confirmed from controlled X-H2 fw 5.20 test shots; see PROTOCOL.md §13.2.
// TAG_IMAGE_NUMBER (0x1438) is the total shutter actuation counter on X-series cameras.
private const val EXIF_TAG_CLARITY        = 0x100f
private const val EXIF_TAG_SHADOW_TONE    = 0x1040
private const val EXIF_TAG_HIGHLIGHT_TONE = 0x1041
private const val EXIF_TAG_GRAIN_STRENGTH = 0x1047
private const val EXIF_TAG_COLOR_CHROME   = 0x1048
private const val EXIF_TAG_MONO_WC        = 0x1049
private const val EXIF_TAG_SMOOTH_SKIN    = 0x104a
private const val EXIF_TAG_MONO_MG        = 0x104b
private const val EXIF_TAG_GRAIN_SIZE     = 0x104c
private const val EXIF_TAG_CC_FX_BLUE     = 0x104e

data class ExifSection(val name: String, val tags: List<Pair<String, String>>)
data class ExifResult(val sections: List<ExifSection>)

object FujiExifReader {

    // ── Raw dump (used by ExifBenchScreen) ────────────────────────────────────

    fun read(context: Context, uri: Uri): ExifResult {
        val stream = context.contentResolver.openInputStream(uri)
            ?: return ExifResult(emptyList())

        val metadata = stream.use { ImageMetadataReader.readMetadata(it) }
        val sections = mutableListOf<ExifSection>()

        metadata.getDirectoriesOfType(FujifilmMakernoteDirectory::class.java).forEach { dir ->
            dir.toSection("Fujifilm MakerNote")?.let { sections.add(it) }
        }
        metadata.getDirectoriesOfType(ExifIFD0Directory::class.java).forEach { dir ->
            dir.toSection("EXIF IFD0")?.let { sections.add(it) }
        }
        metadata.getDirectoriesOfType(ExifSubIFDDirectory::class.java).forEach { dir ->
            dir.toSection("EXIF SubIFD")?.let { sections.add(it) }
        }
        metadata.directories.forEach { dir ->
            if (dir is FujifilmMakernoteDirectory ||
                dir is ExifIFD0Directory ||
                dir is ExifSubIFDDirectory) return@forEach
            dir.toSection(dir.name)?.let { sections.add(it) }
        }

        return ExifResult(sections)
    }

    // ── Shutter count ─────────────────────────────────────────────────────────

    fun readShutterCount(context: Context, uri: Uri): Int? {
        val stream = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull() ?: return null
        val metadata = runCatching { stream.use { ImageMetadataReader.readMetadata(it) } }.getOrNull() ?: return null
        val dir = metadata.getFirstDirectoryOfType(FujifilmMakernoteDirectory::class.java) ?: return null
        return dir.getInteger(FujifilmMakernoteDirectory.TAG_IMAGE_NUMBER)
    }

    // ── Typed recipe decoder ──────────────────────────────────────────────────

    /**
     * Decodes a Fujifilm JPEG's MakerNote into a [RecipeFromExif].
     *
     * Returns null if the file cannot be read, contains no Fujifilm MakerNote,
     * or the film simulation cannot be identified (required to interpret other fields).
     */
    fun readRecipe(context: Context, uri: Uri): RecipeFromExif? {
        val stream = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull() ?: return null
        val metadata = runCatching { stream.use { ImageMetadataReader.readMetadata(it) } }
            .getOrNull() ?: return null

        val dir = metadata.getFirstDirectoryOfType(FujifilmMakernoteDirectory::class.java)
            ?: return null

        val props = mutableMapOf<FujiPropertyCode, Int>()

        val filmSim = decodeFilmSimulation(dir) ?: return null
        props[FujiPropertyCode.FilmSimulation] = filmSim.protocolValue

        val isMono = filmSim.protocolValue in MONO_SIM_CODES

        // Dynamic Range — use Development Dynamic Range (actual value, not the DR setting label)
        dir.getInteger(FujifilmMakernoteDirectory.TAG_DEVELOPMENT_DYNAMIC_RANGE)?.let { raw ->
            props[FujiPropertyCode.DynamicRange] = when (raw) {
                200  -> 200
                400  -> 400
                else -> 100
            }
        }

        // White Balance
        decodeWhiteBalance(dir)?.let { (wbCode, kelvin) ->
            props[FujiPropertyCode.WhiteBalance] = wbCode
            kelvin?.let { props[FujiPropertyCode.ColorTemperature] = it }
        }

        // Grain: strength (0x1047) × size (0x104c) → combined PTP value
        decodeGrainEffect(dir)?.let { props[FujiPropertyCode.GrainEffect] = it }

        // Smooth Skin applies to all sims
        props[FujiPropertyCode.SmoothSkin] = decodeOws(dir.getInteger(EXIF_TAG_SMOOTH_SKIN))

        // Sharpness and High ISO NR apply to all sims.
        // Decoders return dial positions; convert to PTP wire here (see FujiValueMapper / PROTOCOL §6.3).
        decodeSharpness(dir)?.let { props[FujiPropertyCode.Sharpness] = FujiValueMapper.scaledDialToRaw(it.toFloat()) }
        decodeHighIsoNr(dir)?.let { props[FujiPropertyCode.HighIsoNr] = FujiValueMapper.nrDialToRaw(it) }

        // Clarity: EXIF raw = dial × 1000 → dial; PTP wire = dial × 10.
        dir.getInteger(EXIF_TAG_CLARITY)?.let { raw ->
            props[FujiPropertyCode.Clarity] = FujiValueMapper.scaledDialToRaw((raw / 1000).toFloat())
        }

        if (isMono) {
            // Monochrome WC/MG: stored as uint8 in EXIF, interpreted as signed dial; PTP wire = dial × 10.
            dir.getInteger(EXIF_TAG_MONO_WC)?.let { props[FujiPropertyCode.MonoWc] = FujiValueMapper.scaledDialToRaw(it.toByte().toInt().toFloat()) }
            dir.getInteger(EXIF_TAG_MONO_MG)?.let { props[FujiPropertyCode.MonoMg] = FujiValueMapper.scaledDialToRaw(it.toByte().toInt().toFloat()) }
        } else {
            // Color-only properties
            dir.getDescription(FujifilmMakernoteDirectory.TAG_WHITE_BALANCE_FINE_TUNE)
                ?.let { decodeWbFineTune(it) }
                ?.let { (r, b) ->
                    props[FujiPropertyCode.WbShiftRed]  = r
                    props[FujiPropertyCode.WbShiftBlue] = b
                }

            decodeColorDial(dir)?.let { props[FujiPropertyCode.Color] = FujiValueMapper.scaledDialToRaw(it.toFloat()) }

            props[FujiPropertyCode.ColorChrome]       = decodeOws(dir.getInteger(EXIF_TAG_COLOR_CHROME))
            props[FujiPropertyCode.ColorChromeFxBlue] = decodeOws(dir.getInteger(EXIF_TAG_CC_FX_BLUE))
        }

        // Highlight and Shadow Tone: raw = dial × −16, step 0.5.
        // Returned as Float dial values (e.g. −1.5); PTP encoding is a separate concern
        // for the RecipeFromExif → RecipePreset mapper, not here.
        val hlRaw = dir.getInteger(EXIF_TAG_HIGHLIGHT_TONE) ?: 0
        val shRaw = dir.getInteger(EXIF_TAG_SHADOW_TONE)    ?: 0

        return RecipeFromExif(
            properties     = props,
            highlightTone  = hlRaw / -16f,
            shadowTone     = shRaw / -16f,
            sourceFileName = uri.lastPathSegment,
        )
    }

    // ── Decoders ──────────────────────────────────────────────────────────────

    private fun decodeFilmSimulation(dir: FujifilmMakernoteDirectory): FujiFilmSimulation? {
        val filmMode = dir.getDescription(FujifilmMakernoteDirectory.TAG_FILM_MODE)?.trim()
        val colorSat = dir.getDescription(FujifilmMakernoteDirectory.TAG_COLOR_SATURATION)?.trim()

        // Named film modes from Film Mode field
        when (filmMode) {
            "F0/Standard (Provia)"                         -> return FujiFilmSimulation.Provia
            "F2/Fujichrome (Velvia)"                       -> return FujiFilmSimulation.Velvia
            "F1b/Studio Portrait Smooth Skin Tone (Astia)" -> return FujiFilmSimulation.Astia
            "Pro Neg. Hi"                                  -> return FujiFilmSimulation.ProNegHi
            "Pro Neg. Std"                                 -> return FujiFilmSimulation.ProNegStd
            "Classic Chrome"                               -> return FujiFilmSimulation.ClassicChrome
            "Eterna"                                       -> return FujiFilmSimulation.Eterna
            "Classic Negative"                             -> return FujiFilmSimulation.ClassicNeg
            "Bleach Bypass"                                -> return FujiFilmSimulation.EternaBleachBypass
            "Nostalgic Neg"                                -> return FujiFilmSimulation.NostalgicNeg
            "Unknown (2816)"                               -> return FujiFilmSimulation.RealaAce
        }

        // Monochrome, ACROS, and Sepia: Film Mode field absent; identified by Color Saturation.
        // Note: metadata-extractor labels for MonochromeR/G are intentionally wrong — use as
        // opaque keys only. See PROTOCOL.md §13.1.
        return when (colorSat) {
            "None (B&W)"        -> FujiFilmSimulation.Monochrome
            "B&W Yellow Filter" -> FujiFilmSimulation.MonochromeY
            "B&W Green Filter"  -> FujiFilmSimulation.MonochromeR  // ⚠ label wrong — raw value is Red
            "B&W Blue Filter"   -> FujiFilmSimulation.MonochromeG  // ⚠ label wrong — raw value is Green
            "Unknown (784)"     -> FujiFilmSimulation.Sepia
            "Unknown (1280)"    -> FujiFilmSimulation.Acros
            "Unknown (1281)"    -> FujiFilmSimulation.AcrosR
            "Unknown (1282)"    -> FujiFilmSimulation.AcrosY
            "Unknown (1283)"    -> FujiFilmSimulation.AcrosG
            else                -> null
        }
    }

    private fun decodeWhiteBalance(dir: FujifilmMakernoteDirectory): Pair<Int, Int?>? {
        val desc = dir.getDescription(FujifilmMakernoteDirectory.TAG_WHITE_BALANCE)?.trim()
            ?: return null
        return when (desc) {
            "Auto"                  -> 0x0002 to null
            "Unknown (1)"           -> 0x8020 to null  // Auto White Priority
            "Unknown (2)"           -> 0x8021 to null  // Ambience Priority
            "Daylight"              -> 0x0004 to null
            "Cloudy"                -> 0x8006 to null  // Shade — metadata-extractor uses standard EXIF label
            "Incandescence"         -> 0x0006 to null
            "Unknown (1536)"        -> 0x0008 to null  // Underwater
            "Daylight Fluorescent"  -> 0x8001 to null
            "Day White Fluorescent" -> 0x8002 to null
            "White Fluorescent"     -> 0x8003 to null
            "Kelvin"                -> {
                val k = dir.getInteger(FujifilmMakernoteDirectory.TAG_COLOR_TEMPERATURE) ?: 5600
                0x8007 to k
            }
            else -> null
        }
    }

    // WB Fine Tune format: "R_raw B_raw" space-separated; raw = dial × 20
    private fun decodeWbFineTune(description: String): Pair<Int, Int>? {
        val parts = description.trim().split("\\s+".toRegex())
        if (parts.size < 2) return null
        val rRaw = parts[0].toIntOrNull() ?: return null
        val bRaw = parts[1].toIntOrNull() ?: return null
        return (rRaw / 20) to (bRaw / 20)
    }

    // Color Saturation field encodes the Color dial for color sims.
    // For mono/ACROS/Sepia the same field carries the film sim identifier — those
    // strings won't match here and null is returned, which is correct.
    private fun decodeColorDial(dir: FujifilmMakernoteDirectory): Int? {
        val desc = dir.getDescription(FujifilmMakernoteDirectory.TAG_COLOR_SATURATION)?.trim()
            ?: return null
        return when (desc) {
            "Normal"      ->  0
            "Medium High" ->  1
            "High"        ->  2
            "Medium Low"  -> -1
            else -> when (dir.getInteger(FujifilmMakernoteDirectory.TAG_COLOR_SATURATION)) {
                192  ->  3
                224  ->  4
                1024 -> -2
                1216 -> -3
                1248 -> -4
                else -> null
            }
        }
    }

    private fun decodeSharpness(dir: FujifilmMakernoteDirectory): Int? {
        val desc = dir.getDescription(FujifilmMakernoteDirectory.TAG_SHARPNESS)?.trim()
            ?: return null
        return when (desc) {
            "Normal"      ->  0
            "Medium Soft" -> -1
            "Soft"        -> -2
            "Softest"     -> -3
            "Medium Hard" ->  1
            "Hard"        ->  2
            "Hardest"     ->  3
            else -> when (dir.getInteger(FujifilmMakernoteDirectory.TAG_SHARPNESS)) {
                0 -> -4
                6 ->  4
                else -> null
            }
        }
    }

    private fun decodeHighIsoNr(dir: FujifilmMakernoteDirectory): Int? {
        val desc = dir.getDescription(FujifilmMakernoteDirectory.TAG_HIGH_ISO_NOISE_REDUCTION)?.trim()
            ?: return null
        return when (desc) {
            "Normal" ->  0
            "Strong" ->  2
            "Weak"   -> -2
            else -> when (dir.getInteger(FujifilmMakernoteDirectory.TAG_HIGH_ISO_NOISE_REDUCTION)) {
                384 ->  1
                448 ->  3
                480 ->  4
                640 -> -1
                704 -> -3
                736 -> -4
                else -> null
            }
        }
    }

    // Combines grain strength (0x1047) and size (0x104c) into the PTP GrainEffect wire value
    // (1/6=Off, 2=WkS, 3=StS, 4=WkL, 5=StL; see FujiValueMapper / PROTOCOL §6.3).
    // If strength ≠ 0 but size = 0 (invalid camera state), treat as Off — a PTP write
    // requires both axes to be known. Off encodes to 6 (the camera default).
    private fun decodeGrainEffect(dir: FujifilmMakernoteDirectory): Int? {
        val strength = dir.getInteger(EXIF_TAG_GRAIN_STRENGTH) ?: return null
        val size     = dir.getInteger(EXIF_TAG_GRAIN_SIZE)     ?: return null
        if (strength == 0 || size == 0) return 6
        return when {
            strength == 32 && size == 16 -> 2  // Weak Small
            strength == 32 && size == 32 -> 4  // Weak Large
            strength == 64 && size == 16 -> 3  // Strong Small
            strength == 64 && size == 32 -> 5  // Strong Large
            else                         -> 6  // unrecognised combination → Off
        }
    }

    // Maps raw EXIF Off/Weak/Strong encoding (0 / 32 / 64) to PTP wire values (1 / 2 / 3).
    private fun decodeOws(raw: Int?): Int = when (raw) {
        32   -> 2
        64   -> 3
        else -> 1
    }

    private fun com.drew.metadata.Directory.toSection(name: String): ExifSection? {
        val tags = this.tags.map { tag ->
            tag.tagName to (runCatching { tag.description }.getOrElse { "—" } ?: "—")
        }
        return if (tags.isEmpty()) null else ExifSection(name, tags)
    }
}
