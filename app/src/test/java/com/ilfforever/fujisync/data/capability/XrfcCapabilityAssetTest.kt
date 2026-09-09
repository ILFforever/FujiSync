package com.ilfforever.fujisync.data.capability

import com.ilfforever.fujisync.domain.model.FujiPropertyCode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the generated capability asset.
 *
 * It is produced by `tools/generate_capabilities.py` from Fuji's decoded `XRFC.DAT`, so nobody
 * reads it by hand — which is exactly why it needs a test. A regenerated file with a renamed key,
 * a dangling config reference or a variant whose value table was not transcribed would otherwise
 * fail silently at runtime as "this camera supports nothing".
 */
class XrfcCapabilityAssetTest {

    private val asset: JSONObject by lazy {
        val candidates = listOf(
            File("src/main/assets/xrfc_capabilities.json"),
            File("app/src/main/assets/xrfc_capabilities.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
        assertNotNull("capability asset not found in ${candidates.map { it.absolutePath }}", file)
        JSONObject(file!!.readText())
    }

    private fun devices() = asset.getJSONObject("devices")
    private fun configs() = asset.getJSONObject("configs")

    private fun propertiesOf(device: String): JSONObject {
        val config = devices().getString(device)
        return configs().getJSONObject(config).getJSONObject("properties")
    }

    private fun variantOf(device: String, property: FujiPropertyCode): String? =
        propertiesOf(device)
            .getJSONObject("0x%04X".format(property.code))
            .optString("variant")
            .takeIf { it.isNotEmpty() }

    private fun valuesFor(property: FujiPropertyCode, variant: String): List<Int> {
        val entry = asset.getJSONObject("variants")
            .getJSONObject("0x%04X".format(property.code))
            .getJSONObject(variant)
        val array = entry.getJSONArray("values")
        return (0 until array.length()).map { array.getInt(it) }
    }

    @Test
    fun `asset covers Fuji's whole compatibility table`() {
        // 41 camera/firmware combinations across 36 configurations, per the decoded XRFC.DAT.
        assertEquals(41, devices().length())
        assertEquals(36, configs().length())
    }

    @Test
    fun `every device resolves to a config that exists`() {
        devices().keys().forEach { device ->
            val config = devices().getString(device)
            assertTrue("$device -> $config missing from configs", configs().has(config))
        }
    }

    @Test
    fun `every property key is one the app knows`() {
        configs().keys().forEach { config ->
            val properties = configs().getJSONObject(config).getJSONObject("properties")
            properties.keys().forEach { key ->
                val code = key.removePrefix("0x").toInt(16)
                assertNotNull("$config carries unknown property $key", FujiPropertyCode.fromCode(code))
            }
        }
    }

    @Test
    fun `every declared variant has a value table`() {
        val variants = asset.getJSONObject("variants")
        configs().keys().forEach { config ->
            val properties = configs().getJSONObject(config).getJSONObject("properties")
            properties.keys().forEach { key ->
                val variant = properties.getJSONObject(key).optString("variant")
                if (variant.isEmpty() || !variants.has(key)) return@forEach
                assertTrue(
                    "$config/$key declares variant $variant with no value table",
                    variants.getJSONObject(key).has(variant),
                )
            }
        }
    }

    @Test
    fun `film simulation variants form the documented nested chain`() {
        // Std1 1..15 through Std6 1..20, each generation appending exactly one simulation and
        // never renumbering — which is why a newer value is rejected by an older body rather than
        // applied as a different look.
        val expected = mapOf("Std1" to 15, "Std2" to 16, "Std3" to 17, "Std4" to 18, "Std5" to 19, "Std6" to 20)
        expected.forEach { (variant, top) ->
            val values = valuesFor(FujiPropertyCode.FilmSimulation, variant)
            assertEquals("$variant size", top, values.size)
            assertEquals("$variant starts at Provia", 1, values.first())
            assertEquals("$variant ceiling", top, values.max())
        }
    }

    @Test
    fun `known bodies map to the variants the research recorded`() {
        // Spot checks against docs/reverse-engineering/xrfc-value-tables.md section 14.
        assertEquals("Std6", variantOf("X-H2_0200", FujiPropertyCode.FilmSimulation))
        assertEquals("Std5", variantOf("X-H2_0100", FujiPropertyCode.FilmSimulation))
        assertEquals("Std4", variantOf("X-T4_0100", FujiPropertyCode.FilmSimulation))
        assertEquals("Std1", variantOf("X-T2_0100", FujiPropertyCode.FilmSimulation))

        // The X-H2 gains Reala Ace on the _0200 capability record without new hardware.
        assertTrue(20 in valuesFor(FujiPropertyCode.FilmSimulation, "Std6"))
        assertTrue(20 !in valuesFor(FujiPropertyCode.FilmSimulation, "Std5"))
    }

    @Test
    fun `tone dials gain half steps on newer bodies only`() {
        assertEquals("Std1", variantOf("X-T3_0100", FujiPropertyCode.HighlightTone))
        assertEquals("Std2", variantOf("X-T4_0100", FujiPropertyCode.HighlightTone))

        // Wire values are the dial x 10, so a multiple of 5 that is not a multiple of 10 is a half
        // step. Std2 must be a strict superset of Std1, or a recipe would break moving forwards.
        val whole = valuesFor(FujiPropertyCode.HighlightTone, "Std1")
        val half = valuesFor(FujiPropertyCode.HighlightTone, "Std2")
        assertTrue(whole.all { it % 10 == 0 })
        assertTrue(half.any { it % 10 != 0 })
        assertTrue(half.containsAll(whole))
    }

    @Test
    fun `white balance Std2 adds exactly the two auto-priority modes`() {
        val std1 = valuesFor(FujiPropertyCode.WhiteBalance, "Std1")
        val std2 = valuesFor(FujiPropertyCode.WhiteBalance, "Std2")

        assertEquals(12, std1.size)
        assertEquals(14, std2.size)
        assertTrue(std2.containsAll(std1))
        assertEquals(setOf(0x8020, 0x8021), (std2 - std1.toSet()).toSet())
    }

    @Test
    fun `colour temperature is a fixed list on older bodies and a range on newer ones`() {
        val variants = asset.getJSONObject("variants")
            .getJSONObject("0x%04X".format(FujiPropertyCode.ColorTemperature.code))

        assertEquals(31, variants.getJSONObject("Std1").getJSONArray("values").length())

        // The one property that gets *less* restrictive with a newer body.
        val std2 = variants.getJSONObject("Std2")
        assertEquals(2500, std2.getInt("min"))
        assertEquals(10000, std2.getInt("max"))
        assertEquals(10, std2.getInt("step"))
    }

    @Test
    fun `grain size axis exists only on Std2 bodies`() {
        val variants = asset.getJSONObject("variants")
            .getJSONObject("0x%04X".format(FujiPropertyCode.GrainEffect.code))

        // Std1 is not a missing table — it means the body has no grain *size* axis at all, so the
        // composites 4 and 5 (weak/strong x large) are unreachable there.
        assertEquals(false, variants.getJSONObject("Std1").getBoolean("grainSize"))
        assertEquals(true, variants.getJSONObject("Std2").getBoolean("grainSize"))
        assertEquals(listOf(1, 2, 3), valuesFor(FujiPropertyCode.GrainEffect, "Std1"))
        assertEquals(listOf(1, 2, 3, 4, 5), valuesFor(FujiPropertyCode.GrainEffect, "Std2"))
    }

    @Test
    fun `mono warm-cool is the narrowest supported property in the block`() {
        // BlackImageTone (0xD193) is supported on only 4 of 36 configurations.
        val supported = configs().keys().asSequence().count { config ->
            configs().getJSONObject(config)
                .getJSONObject("properties")
                .getJSONObject("0x%04X".format(FujiPropertyCode.MonoWc.code))
                .getBoolean("supported")
        }
        assertEquals(4, supported)
    }
}
