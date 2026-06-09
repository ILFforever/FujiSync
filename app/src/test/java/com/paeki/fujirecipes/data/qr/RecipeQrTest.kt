package com.paeki.fujirecipes.data.qr

import com.paeki.fujirecipes.ui.model.RecipeUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeQrTest {
    @Test
    fun encodeDecode_roundTripsEditableRecipeFields() {
        val recipe = RecipeUiModel(
            libraryId = "lib-1",
            slot = "C3",
            name = "Kodak Gold",
            sim = "Classic Chrome",
            pills = listOf("DR200%", "GRAIN WK/S"),
            description = "Warm daylight look",
            effects = mapOf(
                "Dynamic Range" to "DR200%",
                "Grain Effect" to "Weak Small",
            ),
            tone = mapOf(
                "Highlight Tone" to "+1",
                "Shadow Tone" to "−1",
            ),
            wb = mapOf(
                "White Balance" to "Daylight",
                "WB Shift R" to "+2",
                "WB Shift B" to "−4",
            ),
            referenceImageUris = listOf("file:///local-only.jpg"),
            favorite = true,
        )

        val decoded = RecipeQr.decode(RecipeQr.encode(recipe))!!

        assertNull(decoded.libraryId)
        assertEquals("", decoded.slot)
        assertEquals(recipe.name, decoded.name)
        assertEquals(recipe.sim, decoded.sim)
        assertEquals(recipe.pills, decoded.pills)
        assertEquals(recipe.description, decoded.description)
        assertEquals(recipe.effects, decoded.effects)
        assertEquals(recipe.tone, decoded.tone)
        assertEquals(recipe.wb, decoded.wb)
        assertEquals(emptyList<String>(), decoded.referenceImageUris)
        assertEquals(false, decoded.favorite)
    }

    @Test
    fun decode_rejectsNonFujiSyncPayload() {
        assertNull(RecipeQr.decode("""{"f":"other.recipe","v":1}"""))
        assertNull(RecipeQr.decode("plain text"))
    }
}
