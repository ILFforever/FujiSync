package com.ilfforever.fujisync.data.capability

/**
 * Builds the `<Model>_<FirmwareGeneration>` key that Fuji's capability table is indexed by.
 *
 * Modern bodies report the whole key themselves from `0xD186` / `0xD187` (an X-H2 on firmware 2.00
 * answers `"X-H2_0200"`). Older bodies fail that read, and Fuji's own client then synthesises the
 * key by appending `_0100` to the model name — which is why the table only ever holds `_0100` rows
 * for those bodies. This mirrors that behaviour rather than inventing a scheme.
 *
 * Note the generation suffix is *not* the display firmware version: an X-H2 showing "5.20" in its
 * menus still reports capability generation `_0200`.
 */
object CameraDeviceKey {

    private val KEY_SHAPE = Regex("""^(.+)_(\d{4})$""")
    private const val FIRST_GENERATION = "0100"

    /**
     * Accepts a string read from `0xD186`/`0xD187` only if it has the documented key shape.
     * Anything else — an empty read, a stray battery string, a truncated payload — is rejected so a
     * malformed value never becomes a lookup key.
     */
    fun fromReported(reported: String?): String? {
        val trimmed = reported?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return trimmed.takeIf { KEY_SHAPE.matches(it) }
    }

    /** `X-T3` → `X-T3_0100`. Null when the model name is unusable. */
    fun synthesize(model: String?): String? {
        val normalized = normalizeModel(model) ?: return null
        return "${normalized}_$FIRST_GENERATION"
    }

    /**
     * Strips the vendor prefix and surrounding whitespace that some bodies include in DeviceInfo,
     * leaving the bare model as the capability table spells it.
     */
    fun normalizeModel(model: String?): String? {
        val cleaned = model?.trim()
            ?.removePrefix("FUJIFILM")
            ?.removePrefix("Fujifilm")
            ?.removePrefix("fujifilm")
            ?.trim()
            .orEmpty()
        return cleaned.ifEmpty { null }
    }

    /** Splits a key into its model and numeric generation, or null if it is not key-shaped. */
    fun split(key: String): Pair<String, Int>? {
        val match = KEY_SHAPE.matchEntire(key.trim()) ?: return null
        val generation = match.groupValues[2].toIntOrNull() ?: return null
        return match.groupValues[1] to generation
    }

    /**
     * Canonical form for comparing model names across sources: case and separators are dropped, so
     * `X-T5`, `x-t5` and `XT5` all match, while `X-T5` and `X-T50` stay distinct.
     */
    fun canonicalModel(model: String): String =
        model.filter { it.isLetterOrDigit() }.uppercase()
}
