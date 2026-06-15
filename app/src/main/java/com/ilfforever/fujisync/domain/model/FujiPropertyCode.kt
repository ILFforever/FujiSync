package com.ilfforever.fujisync.domain.model

enum class FujiPropertyCode(
    val code: Int,
    val displayName: String,
    val signed: Boolean = false,
) {
    DynamicRange(0xD190, "Dynamic Range"),
    DRangePriority(0xD191, "D Range Priority"),
    FilmSimulation(0xD192, "Film Simulation"),
    MonoWc(0xD193, "Mono WC", signed = true),
    MonoMg(0xD194, "Mono MG", signed = true),
    GrainEffect(0xD195, "Grain Effect"),
    ColorChrome(0xD196, "Color Chrome"),
    ColorChromeFxBlue(0xD197, "Color Chrome FX Blue"),
    SmoothSkin(0xD198, "Smooth Skin"),
    WhiteBalance(0xD199, "White Balance"),
    WbShiftRed(0xD19A, "WB Shift Red", signed = true),
    WbShiftBlue(0xD19B, "WB Shift Blue", signed = true),
    ColorTemperature(0xD19C, "Color Temperature"),
    HighlightTone(0xD19D, "Highlight Tone", signed = true),
    ShadowTone(0xD19E, "Shadow Tone", signed = true),
    Color(0xD19F, "Color", signed = true),
    Sharpness(0xD1A0, "Sharpness", signed = true),
    HighIsoNr(0xD1A1, "High ISO NR"),
    Clarity(0xD1A2, "Clarity", signed = true);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromCode(code: Int): FujiPropertyCode? = byCode[code]
    }
}
