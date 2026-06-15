package com.ilfforever.fujirecipes.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrRecipeParserTest {

    @Test
    fun `parse tone curve with colon-separated highlight and shadow abbreviations`() {
        val result = OcrRecipeParser.parse("Tone Curve: H: -1.5 S: +0.5")

        assertNotNull(result)
        assertEquals("-1.5", result!!.tone["Highlight Tone"])
        assertEquals("+0.5", result.tone["Shadow Tone"])
    }

    @Test
    fun `parse D Range Priority`() {
        val result = OcrRecipeParser.parse(
            """
                Film Simulation: Classic Chrome
                D Range Priority: Strong
                Dynamic Range: DR400
            """.trimIndent(),
        )

        assertNotNull(result)
        assertEquals("Strong", result!!.effects["D Range Priority"])
        assertEquals("DR400%", result.effects["Dynamic Range"])
    }

    @Test
    fun `parse instagram split-column recipe dump`() {
        val raw = """
            2:03 E
            Explore
            Summer Chrome
            Film Simulation :
            Grain Effect:
            Color Chrome:
            Color Chrome: FX Blue Strong
            1 device
            Color:
            Classic Chrome
            Strong / Large
            White Balance: Auto, +5 Red, -6 Blue
            Dynamic Range :
            Clarity:
            Highlight :
            Shadow
            Add comment...
            Effect Strong
            georgeb0zouris
            J on home Royel Otis C
            DR400
            -2
            -2
            Sharpness :
            Noise Reduction :
            +4
            0
            -4
            Exp. Compensation: 0 to 1
            ISO Auto, : ISO 500 up to ISO 6400
            -4
            VO 5G
            ll
            Follow
            One of my favorite recipe on Fujifilm x100vi
            Summer Chrome Recipe. ...
            ll48
            2/12
            4,687
            88
            133
            784
        """.trimIndent()

        val result = OcrRecipeParser.parse(raw)

        assertNotNull(result)
        assertEquals("Classic Chrome", result!!.sim)
        assertEquals("DR400%", result.effects["Dynamic Range"])
        assertEquals("Strong Large", result.effects["Grain Effect"])
        assertEquals("Strong", result.effects["Color Chrome"])
        assertEquals("Strong", result.effects["Color Chrome FX Blue"])
        assertEquals("-2", result.tone["Highlight Tone"])
        assertEquals("-2", result.tone["Shadow Tone"])
        assertEquals("+4", result.tone["Color"])
        assertEquals("0", result.tone["Sharpness"])
        assertEquals("-4", result.tone["High ISO NR"])
        assertEquals("-4", result.tone["Clarity"])
        assertEquals("Auto", result.wb["White Balance"])
        assertEquals("+5", result.wb["WB Shift R"])
        assertEquals("-6", result.wb["WB Shift B"])
        assertTrue(result.unmatchedFields.isEmpty())
    }

    @Test
    fun `parse split-column recipe with separate red and blue shift labels`() {
        val raw = """
            1:35 O
            Explore
            ESSENZA
            Add comment...
            Film Simulation
            Grain Effect
            Color Chrome Effect
            Color Chrome FX Blue
            White Balance
            Red
            Blue
            Dynamic Range
            Highlights
            Shadows
            SOOC Recipe
            Color
            Sharpness
            Noise Reduction
            Clarity
            emadeluca89
            1 device
            "ESSENZA" is finally here.
            Follo
            Pro Neg. Hi
            Off
            Strong
            Strong
            Auto
            +3
            -5
            DR400
            -2
            +1
            Warm tones. Quiet emotions.
            Made to feel, not just to see.
            +3
            -4
            le 5G
            ll60
            Likes
            244
            57
        """.trimIndent()

        val result = OcrRecipeParser.parse(raw)

        assertNotNull(result)
        assertEquals("Pro Neg Hi", result!!.sim)
        assertEquals("DR400%", result.effects["Dynamic Range"])
        assertEquals("Off", result.effects["Grain Effect"])
        assertEquals("Strong", result.effects["Color Chrome"])
        assertEquals("Strong", result.effects["Color Chrome FX Blue"])
        assertEquals("Auto", result.wb["White Balance"])
        assertEquals("+3", result.wb["WB Shift R"])
        assertEquals("-5", result.wb["WB Shift B"])
        assertEquals("-2", result.tone["Highlight Tone"])
        assertEquals("+1", result.tone["Shadow Tone"])
        assertEquals("+3", result.tone["Color"])
        assertEquals("-4", result.tone["Sharpness"])
        assertEquals("0", result.tone["High ISO NR"])
        assertEquals("0", result.tone["Clarity"])
        assertTrue("Color Chrome FX Blue" !in result.unmatchedFields)
        assertTrue("White Balance" !in result.unmatchedFields)
    }

    @Test
    fun `parse social screenshot where follow appears between split-column values`() {
        val raw = """
            3:29 @EO
            Explore
            Tonkin Mist
            Film Simulation
            Grain Effect
            Color Chrome Effect
            Color Chrome FX Blue
            White Balance
            Dynamic Range
            _fatihurgun
            Fujifilm recipe time!
            Curves
            Noise Reduction
            Color
            Add comment...
            Sharpness
            Clarity
            Classic Negative
            Strong
            Follow
            Weak /Small
            Weak
            (o) DR 400
            * 5200K, +3 Red, -4 Blue
            L H:1 S: +2
            +4
            Vo
            +2
            WIFi ll 58
            4
            -2
            I recently spent some time exploring Ninh Binh i...
            1159
            36
            114
        """.trimIndent()

        val result = OcrRecipeParser.parse(raw)

        assertNotNull(result)
        assertEquals("Classic Neg", result!!.sim)
        assertEquals("DR400%", result.effects["Dynamic Range"])
        assertEquals("Weak Small", result.effects["Grain Effect"])
        assertEquals("Strong", result.effects["Color Chrome"])
        assertEquals("Weak", result.effects["Color Chrome FX Blue"])
        assertEquals("5200K", result.wb["White Balance"])
        assertEquals("+3", result.wb["WB Shift R"])
        assertEquals("-4", result.wb["WB Shift B"])
        assertEquals("+1", result.tone["Highlight Tone"])
        assertEquals("+2", result.tone["Shadow Tone"])
        assertEquals("+4", result.tone["Color"])
        assertEquals("+2", result.tone["Sharpness"])
        assertEquals("+4", result.tone["High ISO NR"])
        assertEquals("-2", result.tone["Clarity"])
        assertTrue("Grain Effect" !in result.unmatchedFields)
        assertTrue("Color Chrome" !in result.unmatchedFields)
        assertTrue("Color Chrome FX Blue" !in result.unmatchedFields)
        assertTrue("Sharpness" !in result.unmatchedFields)
        assertTrue("High ISO NR" !in result.unmatchedFields)
        assertTrue("Clarity" !in result.unmatchedFields)
    }

    @Test
    fun `parse additional pasted OCR recipe formats`() {
        assertParsed(
            raw = """
                Film Simulation: Classic Negative
                Grain Effect: Strong / Large
                Color Chrome Effect: Strong
                Color Chrome FX Blue: Strong
                White Balance: Auto, +4 Red, -5 Blue
                Dynamic Range: DR400
                Highlight: -2
                Shadow: +1
                Color: +4
                Sharpness: 0
                Noise Reduction: -4
                Clarity: -]
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR400%",
            grain = "Strong Large",
            cc = "Strong",
            ccBlue = "Strong",
            wb = "Auto",
            wbR = "+4",
            wbB = "-5",
            hl = "-2",
            sh = "+1",
            color = "+4",
            sharp = "0",
            nr = "-4",
            clarity = "-1",
        )

        assertParsed(
            raw = """
                Film Simulation: Classic Chrome
                Dynamic Range: DR400
                Grain Effect: Strong Large
                Color Chrome Effect: Strong
                Color Chrome Effect Blue: Weak
                White Boalance: 5200K, +1 Red & -5 Blue
                Highlight: +l
                Shadow:0
                Color: +4
                Noise Reduction/ High ISO NR: -4
                Clarity: -4
                Sharpness: -2
            """.trimIndent(),
            sim = "Classic Chrome",
            dr = "DR400%",
            grain = "Strong Large",
            cc = "Strong",
            ccBlue = "Weak",
            wb = "5200K",
            wbR = "+1",
            wbB = "-5",
            hl = "+1",
            sh = "0",
            color = "+4",
            sharp = "-2",
            nr = "-4",
            clarity = "-4",
        )

        assertParsed(
            raw = """
                Film sim: Classic Chrome
                Grain effect: Weak, Small
                Colour Chrome Effect: Strrong
                Colour Chrome FX Blue: Weak
                White Balance: 6300K R: +1 B: -1
                Dynamic Range: DR200
                Tone Curve: H: -1.5 S: +0.5
                Colour: +2
                Sharpness: +2
                High ISO NR: -2
                Clarity: 0
            """.trimIndent(),
            sim = "Classic Chrome",
            dr = "DR200%",
            grain = "Weak Small",
            cc = "Strong",
            ccBlue = "Weak",
            wb = "6300K",
            wbR = "+1",
            wbB = "-1",
            hl = "-1.5",
            sh = "+0.5",
            color = "+2",
            sharp = "+2",
            nr = "-2",
            clarity = "0",
        )

        assertParsed(
            raw = """
                Grain: Weak, Large
                Color Chrome: Weak
                Color FX Blue: Weak
                White Balance: 6500K, R t2/B -3
                Dynamic Range: DR400
                Highlights 2
                ShadowS: -1
                Color: +4
                Sharpness: -1
                High IS0 NR: -4
                Clarity: -3
                Film Simulation: Classic Negative
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR400%",
            grain = "Weak Large",
            cc = "Weak",
            ccBlue = "Weak",
            wb = "6500K",
            wbR = "+2",
            wbB = "-3",
            hl = "+2",
            sh = "-1",
            color = "+4",
            sharp = "-1",
            nr = "-4",
            clarity = "-3",
        )

        assertParsed(
            raw = """
                FILM SIMULATION: CLASSIC CHROME
                GRAIN EFFECT: STRONG, LARGE
                COLOR CHROME EFFECT: WEAK
                COLOR CHROME FX BLUE: WEAK
                SMOOTH SKIN EFFECT: OFF
                WHITE BALANCE: AUTO/ R3, B-3
                DYNAMIC RANGE: DR100
                TONE CURVE: H -2, S +1
                COLOR: +4
                SHARPNESS: -2
                HIGH ISO NR: -4
                CLARITY: 0
            """.trimIndent(),
            sim = "Classic Chrome",
            dr = "DR100%",
            grain = "Strong Large",
            cc = "Weak",
            ccBlue = "Weak",
            wb = "Auto",
            wbR = "+3",
            wbB = "-3",
            hl = "-2",
            sh = "+1",
            color = "+4",
            sharp = "-2",
            nr = "-4",
            clarity = "0",
        )

        assertParsed(
            raw = """
                FILM SIMULATION:
                GRAIN EFFECT
                COLOR CHROME EFFECT
                COLOR CHROME FX BLUE
                WHITE BALANCE
                DYNAMIC RANGE
                HIGHLIGHT
                SHADOW
                COLOR
                SHARPNESS
                CLASSIC CHROME
                OFF
                STRONG
                WEAK
                DAYLIGHT
                DR400
                -1.5
                .ll l 62
                +0.5
                +3
                -2
            """.trimIndent(),
            sim = "Classic Chrome",
            dr = "DR400%",
            grain = "Off",
            cc = "Strong",
            ccBlue = "Weak",
            wb = "Daylight",
            wbR = "0",
            wbB = "0",
            hl = "-1.5",
            sh = "+0.5",
            color = "+3",
            sharp = "-2",
            nr = "0",
            clarity = "0",
        )

        assertParsed(
            raw = """
                RECIPE SETTINGS
                FILM SIM:
                GRAIN:
                COLOR CHROME:
                COLOR FX-BLUE:
                WB:
                DYNAMIC:
                HIGHLIGHTS:
                SHADOWS:
                COLOR:
                SHARPNESS:
                HIGH ISO-NR:
                CLARITY:
                Classic Chrome
                Weak, Small
                Strong
                Off
                AUTO, R +4 / B -5
                DR400
                -2
                +1
                +4
                -2
                -4
                89%
                -3
            """.trimIndent(),
            sim = "Classic Chrome",
            dr = "DR400%",
            grain = "Weak Small",
            cc = "Strong",
            ccBlue = "Off",
            wb = "Auto",
            wbR = "+4",
            wbB = "-5",
            hl = "-2",
            sh = "+1",
            color = "+4",
            sharp = "-2",
            nr = "-4",
            clarity = "-3",
        )

        assertParsed(
            raw = """
                MY O WN RECI PE
                SOFT
                E M B ER
                FILM SIM:
                RECIPE SETTINGS
                GRAIN:
                COLOR CHROME:
                COLOR FX-BLUE:
                WB
                DYNAMIC:
                HIGHLIGHTS:
                SHADOWS:
                COLOR:
                SHARPNESS:
                HIGH ISO-NR:
                CLARITY:
                Classic Negative
                Weak
                Weak, Large
                Weak
                DR400
                6500K, R +2 / B -3
                -2
                -1
                +4
                89%
                -1
                5/13
                Follow
                4
                3
                Likes
                260
                84
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR400%",
            grain = "Weak Large",
            cc = "Weak",
            ccBlue = "Weak",
            wb = "6500K",
            wbR = "+2",
            wbB = "-3",
            hl = "-2",
            sh = "-1",
            color = "+4",
            sharp = "-1",
            nr = "0",
            clarity = "0",
        )

        assertParsed(
            raw = """
                Kodak 200
                Film Simulation: CASSIC CROME
                White Balance: Daylight
                Red: +4
                Blue: -5
                Dynamic Range: DRI00
                Grain Effect-Reughness: Weak
                Grain Effect-Siz2SMALL
                Colour Chrome Efect: Weak
                Colour Chrome Fx Bİue: OFF
                Colour:3
                Sharpness: 2
                Highlight Tone: -1.5
                Shadow Tone: +0.5
                High ISO noise reduction: 0
                Clarity:
                alexandfujifilm
                Follow
                l 63%
                4,679
                144
                75
                394
            """.trimIndent(),
            sim = "Classic Chrome",
            dr = "DR100%",
            grain = "Weak Small",
            cc = "Weak",
            ccBlue = "Off",
            wb = "Daylight",
            wbR = "+4",
            wbB = "-5",
            hl = "-1.5",
            sh = "+0.5",
            color = "+3",
            sharp = "+2",
            nr = "0",
            clarity = "0",
        )

        assertParsed(
            raw = """
                Last Summer Roll
                Film Simulation: Glassic Negative
                rain: Weak(Small
                Color Chrome: Strong
                Color Chrome FX Blue: Strong
                Smooth Skin Effect: Weak
                White Balance:Auto (Red -3 Blue -5)
                Dynamic Range: DR400
                Tone Gurve: Highlights -2 Shadows -0,5
                Follow
                Color:-4
                Sharpness: 0
                Noise Reduction: -4
                Clarity: 0
                Likes
                230
                262
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR400%",
            grain = "Weak Small",
            cc = "Strong",
            ccBlue = "Strong",
            wb = "Auto",
            wbR = "-3",
            wbB = "-5",
            hl = "-2",
            sh = "-0.5",
            color = "-4",
            sharp = "0",
            nr = "-4",
            clarity = "0",
        )

        assertParsed(
            raw = """
                Dolce Vita
                Film simulation: Astia Soft
                Grain Effect: Weakļsmall Golour Ghome FX: Weak
                Colour Chrome FX Blue: Weak
                White Balance: 6000k/6200k, -2 Red,-4 Blue
                Dynamic Range: 400
                Gurve: Highilights-1, Shadows -2
                Colour: •4
                Sharpness:0
                Noise Reduction: -4
                Clarity: 0
            """.trimIndent(),
            sim = "Astia / Soft",
            dr = "DR400%",
            grain = "Weak Small",
            cc = "Weak",
            ccBlue = "Weak",
            wb = "6000K",
            wbR = "-2",
            wbB = "-4",
            hl = "-1",
            sh = "-2",
            color = "+4",
            sharp = "0",
            nr = "-4",
            clarity = "0",
        )

        assertParsed(
            raw = """
                Luce Classir
                Film Simulation: Classic Negative
                Grain Effect: Strong/Small
                Colour Chrome FX: OFF
                Golour Chrome FX Blue: OFF
                White Balance: Daylight (R: -4 B:-5)
                Dynamic Range: DR200
                Follow
                Highlight: 2
                Shadow: 1
                Colour: -3
                Sharpness: 2
                Najse Reduction: -4
                larity: -1
                Likes
                230
                262
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR200%",
            grain = "Strong Small",
            cc = "Off",
            ccBlue = "Off",
            wb = "Daylight",
            wbR = "-4",
            wbB = "-5",
            hl = "+2",
            sh = "+1",
            color = "-3",
            sharp = "+2",
            nr = "-4",
            clarity = "-1",
        )

        assertParsed(
            raw = """
                I took one of my favourite recipes
                gr.scha
                Smmer ChrOme
                butI changed one little detail...
                by @osanbilgi
                Film Sim: Classic Chrome
                Nil l CD 81%
                Dynamic Range: Auto
                Highlights: -2
                Add comment...
                Shadows(+3
                Color: +4
                Noise Reduction/High ISO NR: -4
                Sharpness: 0
                Clarity: -4
                Grain Effect: Strong, Large
                Color Chrome Efect: Strong
                Color Chrome Effect Blue: Strong
                WB: Auto, +5 Red & -6 Blue
                Ps (Instrumental) Giulio C
                Follow
                2/10
                4065
                51
                503
            """.trimIndent(),
            sim = "Classic Chrome",
            dr = "DR Auto",
            grain = "Strong Large",
            cc = "Strong",
            ccBlue = "Strong",
            wb = "Auto",
            wbR = "+5",
            wbB = "-6",
            hl = "-2",
            sh = "+3",
            color = "+4",
            sharp = "0",
            nr = "-4",
            clarity = "-4",
        )

        assertParsed(
            raw = """
                1. Tokyo Summer from Fujistyle
                Film Simulation: CLASSIC Neg.
                White Balance: AUTO
                Red: +2
                Blue: -4
                Dynamic Range: DR200
                Grain Effect-Roughness: OFF
                Colour Chrome Efect: OFF
                Colour Chrome Fx Blue: OFF
                Colour: +3
                Sharpness: +2
                Highlight Tone: -1.5
                Shadow Tone: -1.0
                High ISO noise reduction: -2
                Clarity: O0
                alexandfujifilm
                Follow
                7/10
                617
                100
                27
                76
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR200%",
            grain = "Off",
            cc = "Off",
            ccBlue = "Off",
            wb = "Auto",
            wbR = "+2",
            wbB = "-4",
            hl = "-1.5",
            sh = "-1",
            color = "+3",
            sharp = "+2",
            nr = "-2",
            clarity = "0",
        )

        assertParsed(
            raw = """
                2. KODAK 200
                Film Simulation: CLASSIC CHROME
                White Balance: Daylight
                Red: +4
                Blue: -5
                Dynamic Range: DRI00
                Grain Effect-Roughness: Weak
                Grain Effect-Size: SMALL
                Colour Chrome Effect: Weak
                Colour Chrome Fx Blue: OFF
                Colour: +3
                Sharpness: -2
                Highlight Tone: -1.5
                Shadow Tone: +0.5
                High ISO noise reduction: 0
                Clarity: 0
                Follow
                8/10
                1,617
                100
                27
                76
            """.trimIndent(),
            sim = "Classic Chrome",
            dr = "DR100%",
            grain = "Weak Small",
            cc = "Weak",
            ccBlue = "Off",
            wb = "Daylight",
            wbR = "+4",
            wbB = "-5",
            hl = "-1.5",
            sh = "+0.5",
            color = "+3",
            sharp = "-2",
            nr = "0",
            clarity = "0",
        )

        assertParsed(
            raw = """
                3. Classic Tones by me
                Film Simulation: CLASSIC Neg.
                White Balance: AUTO
                Red: +2
                Blue: -5
                Dynamic Range: DR100
                Grain Efect-Roughness: OFF
                Colour Chrome Effect: Strong
                Colour Chrome Fx Blue: OFF
                Colour: +4
                Sharpness: -2
                Highlight Tone: +1.5
                Shadow Tone: -1.0
                High ISO noise reduction: -4
                Clarity: 0
                Follow
                9/10
                1,617
                100
                27
                76
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR100%",
            grain = "Off",
            cc = "Strong",
            ccBlue = "Off",
            wb = "Auto",
            wbR = "+2",
            wbB = "-5",
            hl = "+1.5",
            sh = "-1",
            color = "+4",
            sharp = "-2",
            nr = "-4",
            clarity = "0",
        )

        assertParsed(
            raw = """
                Dreamland
                I like to use this one for
                tropical sunsets, beach evenings, and golden hour
                Base simulation: Classic Negative
                Grain: Weak/Small
                Color Chrome Effect: Strong
                White Balance: Auto (R+2 B-5)
                Dynamic Range: DR400
                Highlight: -2
                Shadow: -2
                Color: 0
                Sharpness: +1
                High ISO NR: -4
                Clarity: -3
                ll 86%
                globetrotterstudios
                Add comment...
                Follow
                4/9
                785
                80
                37
                274
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR400%",
            grain = "Weak Small",
            cc = "Strong",
            ccBlue = "Off",
            wb = "Auto",
            wbR = "+2",
            wbB = "-5",
            hl = "-2",
            sh = "-2",
            color = "0",
            sharp = "+1",
            nr = "-4",
            clarity = "-3",
        )

        assertParsed(
            raw = """
                Luscious Greens
                I like to use this one for
                springs, lush greenery, rainforest vibes, & tropical nature photos
                Base simulation: Classic Negative
                Grain: Strong/SmallI
                Color Chrome Effect: Off
                White Balance: Daylight (R+4, B-S)
                Dynamic Range: DR400
                Highlight: -2
                Shadow: +1
                ll 86%
                Color: 3
                Sharpness: -2
                High ISO NR: -4
                Clarity: 3
                Add comment...
                globetrotterstudios
                Follow
                6/9
                785
                80
                37
                274
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR400%",
            grain = "Strong Small",
            cc = "Off",
            ccBlue = "Off",
            wb = "Daylight",
            wbR = "+4",
            wbB = "-5",
            hl = "-2",
            sh = "+1",
            color = "+3",
            sharp = "-2",
            nr = "-4",
            clarity = "+3",
        )

        assertParsed(
            raw = """
                Editorial Travel
                This is mny mostthie-to-life' recipe.
                I like to use it for architecture, hotels, detailed travel moments, & brands.
                Base simulation: Cassic Negative
                Grain: Off
                Color Chrome Effect: Off
                White Balance: Auto (R0, B0),
                Dynamic Range: DR100
                Highlight: -2
                Shadow: +1
                Color: 0
                Sharpness: +1
                High ISO NR: 0
                Clarity: +1
                globetrotterstudios
                ll 86%
                Add comment...
                Follow
                8/9
                785
                80
                37
                274
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR100%",
            grain = "Off",
            cc = "Off",
            ccBlue = "Off",
            wb = "Auto",
            wbR = "0",
            wbB = "0",
            hl = "-2",
            sh = "+1",
            color = "0",
            sharp = "+1",
            nr = "0",
            clarity = "+1",
        )

        assertParsed(
            raw = """
                6:22 O
                Explore
                FILM SIMULATION
                Classic Neg.
                DYNAMIC RANGE
                DR400
                Warm street tones, natural blues, and a timeless documentary mood.
                Inspired by Istanbul. Designed to work everywhere.
                D RANGE PRIORITY
                Off
                GRAIN EFFECT
                Weak
                CLASSIC ISTANBUL NEG
                GRAIN SIZE
                Large
                COLOR CHROME
                Strong
                FUJIFILM
                Off
                JPEG RECIPE
                COLOR CHROME FX BLUE
                Off
                sefafilms
                Al lO38%
                SMOOTH SKIN EFFECT
                by @sefafilms
                Add comment...
                Je Jazz · Chai& Chillim l
                WHITE BALANCE
                Auto
                WB SHIFT
                R+3/B -3
                HIGHLIGHT
                -l
                SHADOW
                COLOR
                +4
                SHARPNESS
                HIGH ISO NR
                -4
                CLARITY
                Save this post to try the recipe later.
                +2
                Follow
                Introduing: "Classic lstanbul Neg".
                1,401
                78
                26
                89
            """.trimIndent(),
            sim = "Classic Neg",
            dr = "DR400%",
            grain = "Weak Large",
            cc = "Strong",
            ccBlue = "Off",
            wb = "Auto",
            wbR = "+3",
            wbB = "-3",
            hl = "-1",
            sh = "0",
            color = "+4",
            sharp = "0",
            nr = "-4",
            clarity = "+2",
        )
    }

    @Test
    fun `parse WB temperature with OCR O instead of 0`() {
        val result = OcrRecipeParser.parse("WB 630Ok")
        assertNotNull(result)
        assertEquals("6300K", result!!.wb["White Balance"])
    }

    @Test
    fun `parse WB temperature with multiple O confusions`() {
        val result = OcrRecipeParser.parse("WB 56OOK")
        assertNotNull(result)
        assertEquals("5600K", result!!.wb["White Balance"])
    }

    @Test
    fun `parse WB temperature lowercase ok suffix`() {
        val result = OcrRecipeParser.parse("White Balance: 52O0k")
        assertNotNull(result)
        assertEquals("5200K", result!!.wb["White Balance"])
    }

    @Test
    fun `parse WB temperature with trailing zero as O`() {
        val result = OcrRecipeParser.parse("WB: 650OK")
        assertNotNull(result)
        assertEquals("6500K", result!!.wb["White Balance"])
    }

    @Test
    fun `reject WB temperature outside valid Kelvin range`() {
        val result = OcrRecipeParser.parse("Classic Chrome\n99999K")
        assertNotNull(result)
        assertEquals("Auto", result!!.wb["White Balance"])
    }

    @Test
    fun `parse NR0 without space`() {
        val result = OcrRecipeParser.parse("Pro Neg Hi\nNR0")
        assertNotNull(result)
        assertEquals("0", result!!.tone["High ISO NR"])
    }

    @Test
    fun `parse NR with sign and no space`() {
        val result = OcrRecipeParser.parse("Pro Neg Hi\nNR-2")
        assertNotNull(result)
        assertEquals("-2", result!!.tone["High ISO NR"])
    }

    @Test
    fun `parse NR with plus sign and no space`() {
        val result = OcrRecipeParser.parse("Pro Neg Hi\nNR+3")
        assertNotNull(result)
        assertEquals("+3", result!!.tone["High ISO NR"])
    }

    @Test
    fun `parse shorthand recipe with no spaces between labels and values`() {
        val result = OcrRecipeParser.parse(
            """
                Pro Neg Hi
                DR400
                Highlight+2
                Shadow+2
                Color-1
                Grain Strong
                NR0
                WB 630Ok
            """.trimIndent(),
        )

        assertNotNull(result)
        assertEquals("Pro Neg Hi", result!!.sim)
        assertEquals("DR400%", result.effects["Dynamic Range"])
        assertEquals("+2", result.tone["Highlight Tone"])
        assertEquals("+2", result.tone["Shadow Tone"])
        assertEquals("-1", result.tone["Color"])
        assertEquals("Strong Small", result.effects["Grain Effect"])
        assertEquals("0", result.tone["High ISO NR"])
        assertEquals("6300K", result.wb["White Balance"])
    }

    @Test
    fun `parse DR with O instead of 0`() {
        val result = OcrRecipeParser.parse("Classic Chrome\nDR4OO")
        assertNotNull(result)
        assertEquals("DR400%", result!!.effects["Dynamic Range"])
    }

    @Test
    fun `parse WB temperature with inline shifts and OCR mangled temp`() {
        val result = OcrRecipeParser.parse("White Balance: 63OOK, R+2, B-4")
        assertNotNull(result)
        assertEquals("6300K", result!!.wb["White Balance"])
        assertEquals("+2", result.wb["WB Shift R"])
        assertEquals("-4", result.wb["WB Shift B"])
    }

    @Test
    fun `parse abbreviated recipe with all values glued to labels`() {
        val result = OcrRecipeParser.parse(
            """
                Classic Neg
                HL-2
                SH+1
                Color+4
                Sharpness-2
                NR-4
                Clarity-3
            """.trimIndent(),
        )

        assertNotNull(result)
        assertEquals("Classic Neg", result!!.sim)
        assertEquals("-2", result.tone["Highlight Tone"])
        assertEquals("+1", result.tone["Shadow Tone"])
        assertEquals("+4", result.tone["Color"])
        assertEquals("-2", result.tone["Sharpness"])
        assertEquals("-4", result.tone["High ISO NR"])
        assertEquals("-3", result.tone["Clarity"])
    }

    @Test
    fun `parse WB temperature at boundary values`() {
        val low = OcrRecipeParser.parse("Classic Chrome\nWB: 2500K")
        assertNotNull(low)
        assertEquals("2500K", low!!.wb["White Balance"])

        val high = OcrRecipeParser.parse("Classic Chrome\nWB: 10000K")
        assertNotNull(high)
        assertEquals("10000K", high!!.wb["White Balance"])
    }

    @Test
    fun `parse full recipe with OCR mangled WB and NR`() {
        assertParsed(
            raw = """
                Film Simulation: Pro Neg. Hi
                Dynamic Range: DR400
                Grain Effect: Strong / Small
                Color Chrome Effect: Off
                Color Chrome FX Blue: Off
                White Balance: 630Ok
                Highlight: +2
                Shadow: +2
                Color: -1
                Sharpness: 0
                NR0
                Clarity: 0
            """.trimIndent(),
            sim = "Pro Neg Hi",
            dr = "DR400%",
            grain = "Strong Small",
            cc = "Off",
            ccBlue = "Off",
            wb = "6300K",
            wbR = "0",
            wbB = "0",
            hl = "+2",
            sh = "+2",
            color = "-1",
            sharp = "0",
            nr = "0",
            clarity = "0",
        )
    }

    private fun assertParsed(
        raw: String,
        sim: String,
        dr: String,
        grain: String,
        cc: String,
        ccBlue: String,
        wb: String,
        wbR: String,
        wbB: String,
        hl: String,
        sh: String,
        color: String,
        sharp: String,
        nr: String,
        clarity: String,
    ) {
        val result = OcrRecipeParser.parse(raw)

        assertNotNull(result)
        assertEquals(sim, result!!.sim)
        assertEquals(dr, result.effects["Dynamic Range"])
        assertEquals("Off", result.effects["D Range Priority"])
        assertEquals(grain, result.effects["Grain Effect"])
        assertEquals(cc, result.effects["Color Chrome"])
        assertEquals(ccBlue, result.effects["Color Chrome FX Blue"])
        assertEquals(wb, result.wb["White Balance"])
        assertEquals(wbR, result.wb["WB Shift R"])
        assertEquals(wbB, result.wb["WB Shift B"])
        assertEquals(hl, result.tone["Highlight Tone"])
        assertEquals(sh, result.tone["Shadow Tone"])
        assertEquals(color, result.tone["Color"])
        assertEquals(sharp, result.tone["Sharpness"])
        assertEquals(nr, result.tone["High ISO NR"])
        assertEquals(clarity, result.tone["Clarity"])
    }
}
