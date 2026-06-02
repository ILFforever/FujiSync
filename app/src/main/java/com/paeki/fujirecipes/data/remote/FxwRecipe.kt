package com.paeki.fujirecipes.data.remote

data class FxwRecipe(
    val id: Int,
    val slug: String,
    val title: String,
    val postUrl: String,
    val imageUrls: List<String> = emptyList(),
    val date: String,
    val params: Map<String, String>,
    val articleText: String = "",
) {
    val imageUrl: String? get() = imageUrls.firstOrNull()
    val filmSim: String get() {
        val raw = params["Film Simulation"] ?: return ""
        // "ACROS (INCLUDING +YE, +R, OR +G)" → "Acros"
        if (raw.contains("(", ignoreCase = false)) return raw.substringBefore("(").trim()
        return raw
    }
    val dynamicRange: String get() = params["Dynamic Range"] ?: ""
    val highlight: String get() = params["Highlight"] ?: ""
    val shadow: String get() = params["Shadow"] ?: ""
    val color: String get() = params["Color"] ?: ""
    val clarity: String get() = params["Clarity"] ?: ""
    val grainEffect: String get() = params["Grain Effect"] ?: ""
    val whiteBalance: String get() = params["White Balance"] ?: ""
    val iso: String get() = params["ISO"] ?: ""
    val sharpness: String get() = params["Sharpness"] ?: ""
    val highIsoNr: String get() = params["High ISO NR"] ?: ""
    val colorChrome: String get() = params["Color Chrome Effect"] ?: ""
    val colorChromeFxBlue: String get() = params["Color Chrome FX Blue"] ?: ""

    fun pillLabels(): List<String> = buildList {
        if (dynamicRange.isNotBlank()) add(dynamicRange)
        if (highlight.isNotBlank() && shadow.isNotBlank()) add("Hi $highlight · Sh $shadow")
        if (color.isNotBlank() && color != "0") add("Color $color")
        if (clarity.isNotBlank() && clarity != "0") add("Clarity $clarity")
        if (grainEffect.isNotBlank() && !grainEffect.lowercase().startsWith("off")) {
            val level = when {
                grainEffect.lowercase().contains("strong") -> "Str"
                grainEffect.lowercase().contains("weak") -> "Wk"
                else -> grainEffect.substringBefore(",").trim()
            }
            add("Grain $level")
        }
    }
}
