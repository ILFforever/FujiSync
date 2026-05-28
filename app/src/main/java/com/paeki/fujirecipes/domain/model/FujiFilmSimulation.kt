package com.paeki.fujirecipes.domain.model

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
