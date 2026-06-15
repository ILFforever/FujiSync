package com.ilfforever.fujisync.data.ptp

import java.text.Normalizer

object CameraPresetName {
    const val MAX_LENGTH = 25
    const val FALLBACK = "Untitled"

    private val ALLOWED: Set<Char> = buildSet {
        ('A'..'Z').forEach(::add)
        ('a'..'z').forEach(::add)
        ('0'..'9').forEach(::add)
        add(' ')
        "!\"#$%&'()*+,-./:;<=>?@[]\\^_{}|~".forEach(::add)
    }

    fun sanitize(name: String): String =
        collapseAndStrip(name).trim().take(MAX_LENGTH).trimEnd()

    fun sanitizeForEditing(name: String): String {
        val collapsed = collapseAndStrip(name).trimStart()
        return if (collapsed.length <= MAX_LENGTH) collapsed else collapsed.take(MAX_LENGTH)
    }

    fun sanitizeOrFallback(name: String, fallback: String = FALLBACK): String =
        sanitize(name).ifEmpty { fallback }

    fun validate(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Name can't be empty"
        if (trimmed.length > MAX_LENGTH) return "Too long — max $MAX_LENGTH characters"
        val folded = foldAccents(trimmed)
        if (folded.any { it !in ALLOWED }) return "Name contains characters the camera doesn't support — letters, numbers, and basic punctuation only"
        return null
    }

    private fun collapseAndStrip(name: String): String {
        val folded = foldAccents(name)
        val sb = StringBuilder(folded.length)
        var lastWasSpace = false
        for (raw in folded) {
            var ch = when (raw) {
                'ʼ', '‘', '’', '‛' -> '\''
                else -> raw
            }
            if (ch !in ALLOWED) ch = ' '
            if (ch == ' ') {
                if (!lastWasSpace) sb.append(' ')
                lastWasSpace = true
            } else {
                sb.append(ch)
                lastWasSpace = false
            }
        }
        return sb.toString()
    }

    private fun foldAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val sb = StringBuilder(normalized.length)
        for (ch in normalized) {
            if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) sb.append(ch)
        }
        return sb.toString()
    }
}
