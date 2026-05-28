package com.paeki.fujirecipes.ui.model

object SampleData {
    val slots = listOf(
        RecipeUiModel(
            slot = "C1", name = "LEICA-LIKE", sim = "Pro Neg Hi",
            pills = listOf("DR400%", "GRAIN ST/L", "CC STRONG", "HL −1"),
            description = "A muted, slightly desaturated portrait look inspired by Leica's classic color science. Pro Neg Hi base with strong color chrome for depth.",
            effects = mapOf("Dynamic Range" to "DR400%", "Grain Effect" to "Strong Large", "Color Chrome" to "Strong", "Color Chrome FX Blue" to "Weak", "Smooth Skin" to "Weak"),
            tone = mapOf("Highlight Tone" to "−1", "Shadow Tone" to "0", "Color" to "+2", "Sharpness" to "0", "High ISO NR" to "0", "Clarity" to "0"),
            wb = mapOf("White Balance" to "4700K", "WB Shift R" to "+2", "WB Shift B" to "+2"),
        ),
        RecipeUiModel(
            slot = "C2", name = "PORTRA 160", sim = "Classic Chrome",
            pills = listOf("DR100%", "GRAIN WK/S", "CC WEAK", "HL +1"),
            description = "Echoes the gentle pastel palette of Kodak Portra 160 — soft skin tones, lifted highlights, the easiest look to live with.",
            effects = mapOf("Dynamic Range" to "DR100%", "Grain Effect" to "Weak Small", "Color Chrome" to "Weak", "Color Chrome FX Blue" to "Off", "Smooth Skin" to "Off"),
            tone = mapOf("Highlight Tone" to "+1", "Shadow Tone" to "−0.5", "Color" to "0", "Sharpness" to "−1", "High ISO NR" to "−2", "Clarity" to "0"),
            wb = mapOf("White Balance" to "Auto", "WB Shift R" to "0", "WB Shift B" to "−1"),
        ),
        RecipeUiModel(
            slot = "C3", name = "KODACHROME", sim = "Classic Chrome",
            pills = listOf("DR AUTO", "GRAIN ST/S", "CC STRONG", "FX BLUE STR"),
            description = "Warm reds, deep blues, that mid-century editorial cast. Lifted shadows and red-shifted white balance for the unmistakable Kodachrome glow.",
            effects = mapOf("Dynamic Range" to "DR Auto", "Grain Effect" to "Strong Small", "Color Chrome" to "Strong", "Color Chrome FX Blue" to "Strong", "Smooth Skin" to "Off"),
            tone = mapOf("Highlight Tone" to "−1", "Shadow Tone" to "+2", "Color" to "+4", "Sharpness" to "+1", "High ISO NR" to "0", "Clarity" to "+2"),
            wb = mapOf("White Balance" to "Daylight", "WB Shift R" to "+1", "WB Shift B" to "−2"),
        ),
        RecipeUiModel(
            slot = "C4", name = "TRI-X 400", sim = "Acros+R",
            pills = listOf("DR400%", "GRAIN ST/L", "CLARITY +3"),
            description = "Pushed black-and-white reportage. High contrast, lifted grain, the documentary feel of TRI-X souped a stop hot.",
            effects = mapOf("Dynamic Range" to "DR400%", "Grain Effect" to "Strong Large", "Color Chrome" to "—", "Color Chrome FX Blue" to "—", "Smooth Skin" to "Off"),
            tone = mapOf("Highlight Tone" to "+1", "Shadow Tone" to "+2", "Color" to "0", "Sharpness" to "+2", "High ISO NR" to "−4", "Clarity" to "+3"),
            wb = mapOf("White Balance" to "5500K", "WB Shift R" to "0", "WB Shift B" to "0"),
        ),
        RecipeUiModel(
            slot = "C5", name = "EKTACHROME E100", sim = "Velvia",
            pills = listOf("DR200%", "CC STRONG", "COLOR +2"),
            description = "Slide-film vibrance with a daylight-balanced bite — saturated greens and a touch of cyan in the shadows. Outdoor stock.",
            effects = mapOf("Dynamic Range" to "DR200%", "Grain Effect" to "Off", "Color Chrome" to "Strong", "Color Chrome FX Blue" to "Strong", "Smooth Skin" to "Off"),
            tone = mapOf("Highlight Tone" to "0", "Shadow Tone" to "+1", "Color" to "+2", "Sharpness" to "+1", "High ISO NR" to "−2", "Clarity" to "+1"),
            wb = mapOf("White Balance" to "Daylight", "WB Shift R" to "−1", "WB Shift B" to "+1"),
        ),
        RecipeUiModel(
            slot = "C6", name = "CINESTILL 800T", sim = "Eterna",
            pills = listOf("DR400%", "TUNGSTEN", "HL −2"),
            description = "Tungsten-balanced cinema stock. Low-contrast Eterna base, halated highlights via grain, the night-photography cult favorite.",
            effects = mapOf("Dynamic Range" to "DR400%", "Grain Effect" to "Weak Large", "Color Chrome" to "Weak", "Color Chrome FX Blue" to "Weak", "Smooth Skin" to "Off"),
            tone = mapOf("Highlight Tone" to "−2", "Shadow Tone" to "+1", "Color" to "+1", "Sharpness" to "−1", "High ISO NR" to "0", "Clarity" to "−1"),
            wb = mapOf("White Balance" to "Tungsten", "WB Shift R" to "+3", "WB Shift B" to "−4"),
        ),
        RecipeUiModel(
            slot = "C7", name = "REALA ACE", sim = "Reala Ace",
            pills = listOf("DR AUTO", "CC WEAK", "HL 0"),
            description = "A neutral everyday recipe on the new Reala Ace simulation — accurate skin tones, nothing showy.",
            effects = mapOf("Dynamic Range" to "DR Auto", "Grain Effect" to "Weak Small", "Color Chrome" to "Weak", "Color Chrome FX Blue" to "Off", "Smooth Skin" to "Weak"),
            tone = mapOf("Highlight Tone" to "0", "Shadow Tone" to "0", "Color" to "+1", "Sharpness" to "0", "High ISO NR" to "−2", "Clarity" to "0"),
            wb = mapOf("White Balance" to "Auto", "WB Shift R" to "0", "WB Shift B" to "0"),
        ),
    )

    val library = listOf(
        LibraryRecipeUiModel("lib1", "GOLDEN HOUR", "Classic Chrome", listOf("DR200%", "CC STRONG", "HL −1", "SH +1", "COLOR +2"), "May 24"),
        LibraryRecipeUiModel("lib2", "SUMMER 84", "Velvia", listOf("DR400%", "GRAIN ST/L", "CC STRONG", "COLOR +4"), "May 20"),
        LibraryRecipeUiModel("lib3", "COOL FOG", "Classic Neg", listOf("DR400%", "CC WEAK", "HL +2", "COLOR −2"), "May 18"),
        LibraryRecipeUiModel("lib4", "STREET MONO", "Acros", listOf("DR400%", "GRAIN ST/S", "CLARITY +2"), "May 12"),
        LibraryRecipeUiModel("lib5", "POLAROID 600", "Astia", listOf("DR100%", "GRAIN WK/L", "HL +2", "COLOR −1"), "May 09"),
        LibraryRecipeUiModel("lib6", "OVERCAST PORTRAIT", "Pro Neg Std", listOf("DR200%", "CC WEAK", "SMOOTH SKIN WK", "HL +1"), "May 02"),
    )
}
