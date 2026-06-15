package com.ilfforever.fujisync.domain.model

fun String.canonicalFilmSimLabel(): String {
    val t = trim()
    if (t.isBlank()) return t
    // Exact match
    FujiFilmSimulation.entries.firstOrNull { it.label.equals(t, ignoreCase = true) }
        ?.let { return it.label }
    // Slash-based labels (Provia / Standard, Velvia / Vivid, Astia / Soft):
    // match on the part before "/" in the input against the canonical primary
    val inputPrimary = t.substringBefore("/").trim()
    FujiFilmSimulation.entries
        .filter { "/" in it.label }
        .firstOrNull { it.label.substringBefore("/").trim().equals(inputPrimary, ignoreCase = true) }
        ?.let { return it.label }
    // Compact/alias forms
    return when (t.lowercase()) {
        "acros+r", "acros +r", "acros+ r" -> FujiFilmSimulation.AcrosR.label
        "acros+y", "acros +y", "acros+ y" -> FujiFilmSimulation.AcrosY.label
        "acros+g", "acros +g", "acros+ g" -> FujiFilmSimulation.AcrosG.label
        "monochrome+r", "monochrome +r", "monochrome+ r" -> FujiFilmSimulation.MonochromeR.label
        "monochrome+y", "monochrome +y", "monochrome+ y" -> FujiFilmSimulation.MonochromeY.label
        "monochrome+g", "monochrome +g", "monochrome+ g" -> FujiFilmSimulation.MonochromeG.label
        "classic neg", "classic negative" -> FujiFilmSimulation.ClassicNeg.label
        "pro neg hi", "pro neg. hi" -> FujiFilmSimulation.ProNegHi.label
        "pro neg std", "pro neg. std" -> FujiFilmSimulation.ProNegStd.label
        "nostalgic neg", "nostalgic negative" -> FujiFilmSimulation.NostalgicNeg.label
        "eterna bleach bypass", "bleach bypass" -> FujiFilmSimulation.EternaBleachBypass.label
        else -> t
    }
}

enum class FujiFilmSimulation(val protocolValue: Int, val label: String) {
    Provia(1, "Provia / Standard"),
    Velvia(2, "Velvia / Vivid"),
    Astia(3, "Astia / Soft"),
    ProNegHi(4, "Pro Neg Hi"),
    ProNegStd(5, "Pro Neg Std"),
    Monochrome(6, "Monochrome"),
    MonochromeY(7, "Monochrome + Y"),
    MonochromeR(8, "Monochrome + R"),
    MonochromeG(9, "Monochrome + G"),
    Sepia(10, "Sepia"),
    ClassicChrome(11, "Classic Chrome"),
    Acros(12, "Acros"),
    AcrosY(13, "Acros + Y"),
    AcrosR(14, "Acros + R"),
    AcrosG(15, "Acros + G"),
    Eterna(16, "Eterna"),
    ClassicNeg(17, "Classic Neg"),
    EternaBleachBypass(18, "Eterna Bleach Bypass"),
    NostalgicNeg(19, "Nostalgic Neg"),
    RealaAce(20, "Reala Ace"),
}
