package com.ilfforever.fujirecipes.data.remote

import com.ilfforever.fujirecipes.data.remote.detectXTransGen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests for FxwApi.parseParams() using real HTML fixtures from 100 FXW posts.
 *
 * Format breakdown (pages 1-2, 100 posts):
 *   Modern format (Film Simulation: key) — 19 posts (all post-2023)
 *   Legacy format A (film sim first line of one <strong>) — dominant pre-2023
 *   Legacy format B (film sim in own <strong>, rest in next) — Serr's 500T era
 *   Non-recipe posts — 81
 *   Transition: legacy → modern ~Nov 2022, fully modern from ~May 2023
 */
class FxwApiParseTest {

    // ── FIXTURE 1: Modern 6-strong (X-Trans V stills, 2026) ──────────────────
    // Source: fujifilm-recipe-provia-positive, 2026-05-27
    private val modernSixStrong = """
        <p class="wp-block-paragraph"><strong>Film Simulation: Provia/STD</strong><br><strong>Grain Effect: Strong, Small</strong><br><strong>Color Chrome Effect: Strong</strong><br><strong>Color Chrome FX Blue: Strong</strong><br><strong>White Balance: Auto White Priority, +2 Red &amp; -3 Blue<br>Dynamic Range: DR400<br>Highlight: -1<br>Shadow: +1<br>Color: +4<br>Sharpness: -1</strong><br><strong>High ISO NR: -4<br>Clarity: +2<br>ISO: Auto, up to ISO 6400<br>Exposure Compensation: +1/3 to +1 (typically)</strong></p>
    """.trimIndent()

    @Test fun `modern 6-strong - film simulation with slash`() =
        assertEquals("Provia/STD", FxwApi.parseParams(modernSixStrong)["Film Simulation"])

    @Test fun `modern 6-strong - grain effect with size`() =
        assertEquals("Strong, Small", FxwApi.parseParams(modernSixStrong)["Grain Effect"])

    @Test fun `modern 6-strong - white balance with html entity`() =
        assertEquals("Auto White Priority, +2 Red & -3 Blue", FxwApi.parseParams(modernSixStrong)["White Balance"])

    @Test fun `modern 6-strong - high ISO NR`() =
        assertEquals("-4", FxwApi.parseParams(modernSixStrong)["High ISO NR"])

    @Test fun `modern 6-strong - clarity`() =
        assertEquals("+2", FxwApi.parseParams(modernSixStrong)["Clarity"])

    // ── FIXTURE 2: Modern 3-strong (video recipe, 2026) ──────────────────────
    // Source: chrome-color-a-video-recipe-for-fujifilm-cameras, 2026-05-02
    // Note: has Interframe NR, decimal shadow value, no Grain/Clarity
    private val modernVideoRecipe = """
        <p class="wp-block-paragraph"><strong>Film Simulation: Classic Chrome</strong><br><strong>White Balance: Auto, +2 Red &amp; -4 Blue<br>Dynamic Range: DR400<br>Highlight: 0<br>Shadow: -1.5<br>Color: +3<br>Sharpness: 0</strong><br><strong>High ISO NR: -4<br>Interframe NR: Auto<br>ISO: up to ISO 6400</strong></p>
    """.trimIndent()

    @Test fun `modern video recipe - film simulation`() =
        assertEquals("Classic Chrome", FxwApi.parseParams(modernVideoRecipe)["Film Simulation"])

    @Test fun `modern video recipe - decimal shadow value`() =
        assertEquals("-1.5", FxwApi.parseParams(modernVideoRecipe)["Shadow"])

    @Test fun `modern video recipe - interframe NR`() =
        assertEquals("Auto", FxwApi.parseParams(modernVideoRecipe)["Interframe NR"])

    // ── FIXTURE 3: Modern with D-Range Priority + conditional cross-gen value ─
    // Source: expired-kodak-vision2-250d..., 2025-12-23
    // Note: uses D-Range Priority not Dynamic Range, text nodes between strongs (cross-gen conditional)
    private val modernDRangePriority = """
        <p class="wp-block-paragraph"><strong>Film Simulation: Eterna Bleach Bypass</strong><br><strong>D-Range Priority: DR-P Auto</strong><br><strong>Grain Effect: Weak, Small</strong><br><strong>Color Chrome Effect: Strong</strong><br><strong>Color Chrome FX Blue: Off </strong>(X-Trans V); <strong>Weak</strong> (X-Trans IV)<br><strong>White Balance: 8700K, -4 Red &amp; -3 Blue<br>Color: -2<br>Sharpness: -2</strong><br><strong>High ISO NR: -4<br>Clarity: -2<br>ISO: Auto, up to ISO 6400<br>Exposure Compensation: -1/3 to +2/3 (typically)</strong></p>
    """.trimIndent()

    @Test fun `modern D-Range Priority - film simulation`() =
        assertEquals("Eterna Bleach Bypass", FxwApi.parseParams(modernDRangePriority)["Film Simulation"])

    @Test fun `modern D-Range Priority - d-range priority param`() =
        assertEquals("DR-P Auto", FxwApi.parseParams(modernDRangePriority)["D-Range Priority"])

    @Test fun `modern D-Range Priority - kelvin white balance`() =
        assertEquals("8700K, -4 Red & -3 Blue", FxwApi.parseParams(modernDRangePriority)["White Balance"])

    // ── FIXTURE 4: Legacy A (all-in-one <strong>, pre-2023) ──────────────────
    // Source: fujifilm-x-trans-iv-film-simulation-recipe-kodak-brilliance, 2022-03-29
    // Note: uses Noise Reduction (not High ISO NR), Color Chrome Effect Blue (not FX Blue)
    private val legacyVariantA = """
        <p class="wp-block-paragraph"><strong>Classic Chrome<br>Dynamic Range: DR200<br>Highlight: +4<br>Shadow: -2<br>Color: +4<br>Noise Reduction: -4<br>Sharpness: -1<br>Clarity: +2<br>Grain Effect: Weak, Small<br>Color Chrome Effect: Strong<br>Color Chrome Effect Blue: Weak<br>White Balance: Daylight, +2 Red &amp; -1 Blue<br>ISO: Auto, up to ISO 6400<br>Exposure Compensation: -1 to 0 (typically)</strong></p>
    """.trimIndent()

    @Test fun `legacy A - film simulation extracted from first line`() =
        assertEquals("Classic Chrome", FxwApi.parseParams(legacyVariantA)["Film Simulation"])

    @Test fun `legacy A - noise reduction (older param name)`() =
        assertEquals("-4", FxwApi.parseParams(legacyVariantA)["Noise Reduction"])

    @Test fun `legacy A - color chrome effect blue (older param name)`() =
        assertEquals("Weak", FxwApi.parseParams(legacyVariantA)["Color Chrome Effect Blue"])

    @Test fun `legacy A - all core params present`() {
        val params = FxwApi.parseParams(legacyVariantA)
        assertEquals("DR200", params["Dynamic Range"])
        assertEquals("+4", params["Highlight"])
        assertEquals("-2", params["Shadow"])
        assertEquals("+4", params["Color"])
        assertEquals("-1", params["Sharpness"])
        assertEquals("+2", params["Clarity"])
        assertEquals("Weak, Small", params["Grain Effect"])
        assertEquals("Strong", params["Color Chrome Effect"])
    }

    // ── FIXTURE 5: Legacy B (film sim in separate <strong>) ──────────────────
    // Source: fujifilm-x-trans-iv-film-simulation-recipe-serrs-500t, 2022-03-20
    private val legacyVariantB = """
        <p class="wp-block-paragraph"><strong>Classic Chrome</strong><br><strong>Dynamic Range: DR-Auto<br>Highlight: -2<br>Shadow: -1<br>Color: -1<br>Noise Reduction: -2<br>Sharpness: -1<br>Clarity: 0<br>Grain Effect: Strong, Large<br>Color Chrome Effect: Strong<br>Color Chrome Effect Blue: Strong<br>White Balance: 3200K, -1 Red &amp; +4 Blue<br>ISO: Auto, up to ISO 6400<br>Exposure Compensation: -1/3 to +1/3 (typically)</strong></p>
    """.trimIndent()

    @Test fun `legacy B - film simulation from standalone strong tag`() =
        assertEquals("Classic Chrome", FxwApi.parseParams(legacyVariantB)["Film Simulation"])

    @Test fun `legacy B - kelvin white balance`() =
        assertEquals("3200K, -1 Red & +4 Blue", FxwApi.parseParams(legacyVariantB)["White Balance"])

    @Test fun `legacy B - DR-Auto`() =
        assertEquals("DR-Auto", FxwApi.parseParams(legacyVariantB)["Dynamic Range"])

    // ── FIXTURE 6: Legacy X-Trans II (parenthetical labels in values) ─────────
    // Source: fujifilm-x-t1-x-trans-ii-film-simulation-recipe-scanned-negative, 2022-01-24
    // Note: values have "(Medium-Soft)" style annotations
    private val legacyXTransII = """
        <p class="wp-block-paragraph"><strong>PRO Neg. Std<br>Dynamic Range: DR200<br>Highlight: -1 (Medium-Soft)<br>Shadow: 0 (Standard)<br>Color: -2 (Low)<br>Sharpness: 0 (Standard)<br>Noise Reduction: -2 (Low)<br>White Balance: 5300K, -5 Red &amp; -4 Blue<br>ISO: Auto up to ISO 3200</strong><br><strong>Exposure Compensation: +1/3 to +2/3 (typically)</strong></p>
    """.trimIndent()

    @Test fun `legacy X-Trans II - film simulation with dot`() =
        assertEquals("PRO Neg. Std", FxwApi.parseParams(legacyXTransII)["Film Simulation"])

    @Test fun `legacy X-Trans II - highlight value with parenthetical annotation`() {
        // Parser should at minimum return something for Highlight
        val params = FxwApi.parseParams(legacyXTransII)
        assertTrue(params.containsKey("Highlight"))
        assertTrue(params["Highlight"]!!.startsWith("-1"))
    }

    @Test fun `legacy X-Trans II - shadow with Standard annotation`() {
        val params = FxwApi.parseParams(legacyXTransII)
        assertTrue(params.containsKey("Shadow"))
    }

    // ── Edge cases ─────────────────────────────────────────────────────────────

    @Test fun `returns empty map when no recipe block found`() {
        assertTrue(FxwApi.parseParams("<p>Just a blog post, no recipe here.</p>").isEmpty())
    }

    @Test fun `film sim in anchor link (roundup post) is not matched`() {
        val html = """<p>Check out <a href="https://fujixweekly.com/sepia"><strong>Sepia</strong></a> for a classic look.</p>"""
        assertTrue(FxwApi.parseParams(html).isEmpty())
    }

    @Test fun `modern format - Nostalgic Neg with trailing period`() {
        val html = """<p><strong>Film Simulation: Nostalgic Neg.</strong><br><strong>Dynamic Range: DR400<br>Color: +2</strong></p>"""
        assertEquals("Nostalgic Neg.", FxwApi.parseParams(html)["Film Simulation"])
    }

    @Test fun `legacy A - Provia STD GFX variant`() {
        val html = """<p><strong>Provia/STD<br>Dynamic Range: DR400<br>Highlight: +1<br>Noise Reduction: -4</strong></p>"""
        val params = FxwApi.parseParams(html)
        assertEquals("Provia/STD", params["Film Simulation"])
        assertEquals("DR400", params["Dynamic Range"])
    }

    @Test fun `modern format with br without slash`() {
        val html = "<p><strong>Film Simulation: Velvia<br>Dynamic Range: DR400<br>Color: +4</strong></p>"
        val params = FxwApi.parseParams(html)
        assertEquals("Velvia", params["Film Simulation"])
        assertEquals("DR400", params["Dynamic Range"])
        assertEquals("+4", params["Color"])
    }

    @Test fun `film simulation value is trimmed`() {
        // Some posts have trailing space: "Film Simulation: Acros " (before closing strong)
        val html = "<p><strong>Film Simulation: Acros </strong><br><strong>Dynamic Range: DR100</strong></p>"
        assertEquals("Acros", FxwApi.parseParams(html)["Film Simulation"]?.trim())
    }

    // ── Multi-variant (Wes Anderson post) ─────────────────────────────────────
    // Source: getting-a-wes-anderson-look-from-your-fujifilm-camera-4-new-film-simulation-recipes
    // 4 recipe blocks: Vibrant Arizona (X-Trans V), Vibrant Arizona (X-Trans IV),
    //                  Indoor Angouleme (X-Trans V), Indoor Angouleme (X-Trans IV)
    private val wesAndersonHtml = """
        <p>Some article text about Asteroid City.</p>
        <p><strong>Vibrant Arizona</strong> (X-Trans V)</p>
        <p>More text about the recipe.</p>
        <p class="wp-block-paragraph"><strong>Film Simulation: Classic Chrome</strong><br><strong>Grain Effect: Weak, Small</strong><br><strong>Color Chrome Effect: Off</strong><br><strong>Color Chrome FX Blue: Weak</strong><br><strong>White Balance: 4350K, +6 Red &amp; -8 Blue<br>Dynamic Range: DR-P Strong<br>Color: +4<br>Sharpness: -2</strong><br><strong>High ISO NR: -4<br>Clarity: -3<br>ISO: Auto, up to ISO 6400<br>Exposure Compensation: +2/3 to +1 1/3 (typically)</strong></p>
        <p>Some photos here.</p>
        <p><strong>Vibrant Arizona</strong> (X-Trans IV)</p>
        <p>Because X-Trans V cameras render blue more deeply.</p>
        <p class="wp-block-paragraph"><strong>Film Simulation: Classic Chrome</strong><br><strong>Grain Effect: Weak, Small</strong><br><strong>Color Chrome Effect: Off</strong><br><strong>Color Chrome FX Blue: Strong</strong><br><strong>White Balance: 4350K, +6 Red &amp; -8 Blue<br>Dynamic Range: DR-P Strong<br>Color: +4<br>Sharpness: -2</strong><br><strong>High ISO NR: -4<br>Clarity: -3<br>ISO: Auto, up to ISO 6400<br>Exposure Compensation: +2/3 to +1 1/3 (typically)</strong></p>
        <p>Some photos here.</p>
        <p><strong>Indoor Angouleme</strong> (X-Trans V)</p>
        <p>Inspired by The French Dispatch.</p>
        <p class="wp-block-paragraph"><strong>Film Simulation: Nostalgic Neg.</strong><br><strong>Grain Effect: Weak, Small</strong><br><strong>Color Chrome Effect: Strong</strong><br><strong>Color Chrome FX Blue: Weak</strong><br><strong>White Balance: Auto Ambience Priority, -2 Red &amp; -6 Blue<br>Dynamic Range: DR400<br>Highlight: -2<br>Shadow: -2<br>Color: -1<br>Sharpness: -2</strong><br><strong>High ISO NR: -4<br>Clarity: -3<br>ISO: Auto, up to ISO 6400<br>Exposure Compensation: +2/3 to +1 (typically)</strong></p>
        <p>Photos.</p>
        <p><strong>Indoor Angouleme</strong> (X-Trans IV)</p>
        <p>Uses Eterna instead of Nostalgic Neg.</p>
        <p class="wp-block-paragraph"><strong>Film Simulation: Eterna</strong><br><strong>Grain Effect: Weak, Small</strong><br><strong>Color Chrome Effect: Strong</strong><br><strong>Color Chrome FX Blue: Strong</strong><br><strong>White Balance: Auto Ambience Priority, -1 Red &amp; -6 Blue<br>Dynamic Range: DR200<br>Highlight: -1<br>Shadow: -1<br>Color: +4<br>Sharpness: -2</strong><br><strong>High ISO NR: -4<br>Clarity: -3<br>ISO: Auto, up to ISO 6400<br>Exposure Compensation: 0 to +2/3 (typically)</strong></p>
    """.trimIndent()

    @Test fun `wes anderson - detects 4 variants`() =
        assertEquals(4, FxwApi.parseAllVariants(wesAndersonHtml).size)

    @Test fun `wes anderson - first variant is Classic Chrome with Weak CCFxBlue`() {
        val variants = FxwApi.parseAllVariants(wesAndersonHtml)
        assertEquals("Classic Chrome", variants[0]["Film Simulation"])
        assertEquals("Weak", variants[0]["Color Chrome FX Blue"])
    }

    @Test fun `wes anderson - second variant has Strong CCFxBlue`() {
        val variants = FxwApi.parseAllVariants(wesAndersonHtml)
        assertEquals("Classic Chrome", variants[1]["Film Simulation"])
        assertEquals("Strong", variants[1]["Color Chrome FX Blue"])
    }

    @Test fun `wes anderson - third variant is Nostalgic Neg`() {
        val variants = FxwApi.parseAllVariants(wesAndersonHtml)
        assertEquals("Nostalgic Neg.", variants[2]["Film Simulation"])
    }

    @Test fun `wes anderson - fourth variant is Eterna`() {
        val variants = FxwApi.parseAllVariants(wesAndersonHtml)
        assertEquals("Eterna", variants[3]["Film Simulation"])
    }

    // ── detectXTransGen ───────────────────────────────────────────────────────

    @Test fun `detectXTransGen - X-Trans V from title`() =
        assertEquals("X-Trans V", detectXTransGen("Kodachrome 64 — Fujifilm X-T5 (X-Trans V) Film Simulation Recipe"))

    @Test fun `detectXTransGen - X-Trans IV from title`() =
        assertEquals("X-Trans IV", detectXTransGen("Fujifilm X-Trans IV Film Simulation Recipe: Serr's 500T"))

    @Test fun `detectXTransGen - X-Trans III from title`() =
        assertEquals("X-Trans III", detectXTransGen("My Fujifilm X-H1 X-Trans III Film Simulation Recipe"))

    @Test fun `detectXTransGen - returns null for no generation in title`() =
        assertEquals(null, detectXTransGen("Getting a Wes Anderson Look from your Fujifilm Camera"))
}
