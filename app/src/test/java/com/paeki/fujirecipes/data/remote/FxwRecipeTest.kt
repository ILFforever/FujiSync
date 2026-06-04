package com.paeki.fujirecipes.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class FxwRecipeTest {

    @Test
    fun `pillLabels uses the same compact casing as local recipe pills`() {
        val recipe = recipe(
            mapOf(
                "Film Simulation" to "Classic Chrome",
                "Dynamic Range" to "DR400",
                "Grain Effect" to "Weak Small",
                "Highlight" to "-1",
                "Shadow" to "+1",
                "Color" to "+2",
                "Clarity" to "-2",
            ),
        )

        assertEquals(
            listOf("DR400%", "GRAIN WK/S", "HL -1", "SH +1", "COLOR +2", "CLARITY -2"),
            recipe.pillLabels(),
        )
    }

    @Test
    fun `pillLabels omits off and zero values`() {
        val recipe = recipe(
            mapOf(
                "Film Simulation" to "Provia",
                "Dynamic Range" to "DR100%",
                "Grain Effect" to "Off",
                "Highlight" to "0",
                "Shadow" to "0",
                "Color" to "0",
                "Clarity" to "0",
            ),
        )

        assertEquals(listOf("DR100%"), recipe.pillLabels())
    }

    private fun recipe(params: Map<String, String>): FxwRecipe =
        FxwRecipe(
            id = 1,
            slug = "test-recipe",
            title = "Test Recipe",
            postUrl = "https://example.com/test",
            date = "2026-06-03",
            params = params,
        )
}
