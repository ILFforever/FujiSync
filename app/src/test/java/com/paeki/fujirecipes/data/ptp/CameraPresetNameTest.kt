package com.paeki.fujirecipes.data.ptp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPresetNameTest {

    // ── sanitize: basic stripping ─────────────────────────────────────────────

    @Test
    fun `sanitize strips leading and trailing whitespace`() {
        assertEquals("Hello", CameraPresetName.sanitize("  Hello  "))
    }

    @Test
    fun `sanitize collapses internal whitespace runs to single space`() {
        assertEquals("A B C", CameraPresetName.sanitize("A   B   C"))
    }

    @Test
    fun `sanitize replaces disallowed chars with spaces`() {
        assertEquals("A B", CameraPresetName.sanitize("A🎞️B"))
    }

    @Test
    fun `sanitize allows camera keyboard punctuation`() {
        listOf(
            "!\"#$%&'()*+,-./",
            ":;<=>?@[]\\^_{}|~",
        ).forEach { input ->
            assertEquals(input, CameraPresetName.sanitize(input))
        }
    }

    @Test
    fun `sanitize replaces unicode dash and middle dot with spaces`() {
        assertEquals("A B C D E", CameraPresetName.sanitize("A–B—C―D·E"))
    }

    @Test
    fun `sanitize allows alphanumerics digits and punctuation`() {
        val input = "Kodak's 400TX"
        assertEquals("Kodak's 400TX", CameraPresetName.sanitize(input))
    }

    @Test
    fun `sanitize folds accented letters to base ASCII`() {
        // é → e, ñ → n
        assertEquals("Eterna Neg", CameraPresetName.sanitize("Eterna Neg"))
        assertEquals("resume", CameraPresetName.sanitize("résumé"))
        assertEquals("Canon", CameraPresetName.sanitize("Cañon"))
    }

    @Test
    fun `sanitize returns empty string for all-disallowed input`() {
        assertEquals("", CameraPresetName.sanitize("🎞️"))
    }

    @Test
    fun `sanitize truncates to MAX_LENGTH`() {
        val long = "A".repeat(50)
        val result = CameraPresetName.sanitize(long)
        assertEquals(CameraPresetName.MAX_LENGTH, result.length)
    }

    @Test
    fun `sanitize trims trailing spaces after truncation`() {
        // MAX_LENGTH 'A's + spaces -> truncate, then strip the trailing space if it lands on the cut.
        val input = "A".repeat(CameraPresetName.MAX_LENGTH) + "     "
        val result = CameraPresetName.sanitize(input)
        assertTrue(result.length <= CameraPresetName.MAX_LENGTH)
        assertTrue(!result.endsWith(" "))
    }

    // ── sanitizeOrFallback ────────────────────────────────────────────────────

    @Test
    fun `sanitizeOrFallback returns fallback for empty-after-sanitize input`() {
        assertEquals(CameraPresetName.FALLBACK, CameraPresetName.sanitizeOrFallback("🎞️"))
    }

    @Test
    fun `sanitizeOrFallback returns sanitized value when non-empty`() {
        assertEquals("Kodachrome", CameraPresetName.sanitizeOrFallback("Kodachrome"))
    }

    @Test
    fun `sanitizeOrFallback uses custom fallback string`() {
        assertEquals("Custom", CameraPresetName.sanitizeOrFallback("", fallback = "Custom"))
    }

    // ── sanitizeForEditing ────────────────────────────────────────────────────

    @Test
    fun `sanitizeForEditing preserves trailing space for in-progress input`() {
        // Unlike sanitize(), sanitizeForEditing trims start but not end
        val result = CameraPresetName.sanitizeForEditing("Velvia ")
        assertEquals("Velvia ", result)
    }

    @Test
    fun `sanitizeForEditing strips leading spaces`() {
        val result = CameraPresetName.sanitizeForEditing("  Provia")
        assertEquals("Provia", result)
    }

    @Test
    fun `sanitizeForEditing truncates at MAX_LENGTH`() {
        val long = "B".repeat(40)
        val result = CameraPresetName.sanitizeForEditing(long)
        assertEquals(CameraPresetName.MAX_LENGTH, result.length)
    }

    // ── smart-quote normalisation ─────────────────────────────────────────────

    @Test
    fun `sanitize normalises curly apostrophe to straight apostrophe`() {
        // U+2019 RIGHT SINGLE QUOTATION MARK → '
        val input = "Nat’s Recipe"
        assertEquals("Nat's Recipe", CameraPresetName.sanitize(input))
    }
}
