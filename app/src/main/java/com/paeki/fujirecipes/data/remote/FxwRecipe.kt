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
        if (dynamicRange.isNotBlank()) add(dynamicRange.normalizedDynamicRangePill())
        if (grainEffect.isNotBlank() && !grainEffect.lowercase().startsWith("off")) {
            add("GRAIN ${grainEffect.normalizedGrainPill()}")
        }
        if (highlight.isNotBlank() && highlight != "0") add("HL $highlight")
        if (shadow.isNotBlank() && shadow != "0") add("SH $shadow")
        if (color.isNotBlank() && color != "0") add("COLOR $color")
        if (clarity.isNotBlank() && clarity != "0") add("CLARITY $clarity")
    }

    private fun String.normalizedDynamicRangePill(): String =
        trim().uppercase()
            .replace("DR ", "DR")
            .let { value -> if (value.matches(Regex("""DR\d{3}"""))) "$value%" else value }

    private fun String.normalizedGrainPill(): String {
        val lower = lowercase()
        val strength = when {
            "strong" in lower -> "ST"
            "weak" in lower -> "WK"
            else -> substringBefore(",").trim().uppercase()
        }
        val size = when {
            "large" in lower -> "L"
            "small" in lower -> "S"
            else -> null
        }
        return size?.let { "$strength/$it" } ?: strength
    }
}
