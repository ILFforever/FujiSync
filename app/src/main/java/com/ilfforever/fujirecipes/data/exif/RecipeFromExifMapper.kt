package com.ilfforever.fujirecipes.data.exif

import com.ilfforever.fujirecipes.domain.model.CameraSlot
import com.ilfforever.fujirecipes.domain.model.FujiPropertyCode
import com.ilfforever.fujirecipes.domain.model.RecipePreset
import kotlin.math.roundToInt

fun RecipeFromExif.toPreset(name: String, slot: CameraSlot = CameraSlot.C1): RecipePreset {
    val props = properties.toMutableMap()
    props[FujiPropertyCode.HighlightTone] = (highlightTone * 10).roundToInt()
    props[FujiPropertyCode.ShadowTone] = (shadowTone * 10).roundToInt()
    return RecipePreset(slot = slot, name = name, properties = props)
}
