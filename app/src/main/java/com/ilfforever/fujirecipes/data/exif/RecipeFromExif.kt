package com.ilfforever.fujirecipes.data.exif

import com.ilfforever.fujirecipes.domain.model.FujiFilmSimulation
import com.ilfforever.fujirecipes.domain.model.FujiPropertyCode

/**
 * Recipe parameters decoded from a Fujifilm JPEG MakerNote.
 *
 * [properties] uses the same integer units as RecipePreset.properties and can be used
 * directly to construct a RecipePreset once a slot and name are assigned.
 *
 * [highlightTone] and [shadowTone] are kept separate as Float because they have 0.5-step
 * precision (dial range −2.0..+2.0) that cannot be represented in the Int-keyed properties
 * map without a lossy conversion or an agreed-upon scale factor.
 */
data class RecipeFromExif(
    val properties: Map<FujiPropertyCode, Int>,
    /** Dial value, −2.0..+2.0, step 0.5. */
    val highlightTone: Float,
    /** Dial value, −2.0..+2.0, step 0.5. */
    val shadowTone: Float,
    val sourceFileName: String? = null,
) {
    val filmSimulation: FujiFilmSimulation?
        get() = properties[FujiPropertyCode.FilmSimulation]
            ?.let { code -> FujiFilmSimulation.entries.firstOrNull { it.protocolValue == code } }
}
