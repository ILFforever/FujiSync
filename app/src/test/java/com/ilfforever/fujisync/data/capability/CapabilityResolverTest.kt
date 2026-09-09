package com.ilfforever.fujisync.data.capability

import com.ilfforever.fujisync.data.ptp.PTP_TYPE_UINT16
import com.ilfforever.fujisync.data.ptp.PtpPropDesc
import com.ilfforever.fujisync.data.ptp.PtpPropForm
import com.ilfforever.fujisync.domain.model.FujiPropertyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityResolverTest {

    private val everyRecipeCode = FujiPropertyCode.entries.map { it.code }

    private fun tableMatch(
        properties: Map<FujiPropertyCode, TableProperty>,
        grainSize: Boolean? = null,
    ) = TableMatch(
        requestedKey = "X-H2_0200",
        matchedKey = "X-H2_0200",
        configName = "X-H2Config2",
        exact = true,
        properties = properties,
        grainSizeSupported = grainSize,
    )

    @Test
    fun `camera's own property list decides support`() {
        val advertised = everyRecipeCode - FujiPropertyCode.Clarity.code

        val capability = CapabilityResolver.resolve(
            deviceKey = "X-T3_0100",
            keyOrigin = KeyOrigin.ReportedByCamera,
            table = null,
            tableVersion = null,
            advertisedProperties = advertised,
        )

        assertEquals(Support.Absent, capability.of(FujiPropertyCode.Clarity).support)
        assertTrue(capability.isBlocked(FujiPropertyCode.Clarity))
        assertEquals(Support.Present, capability.of(FujiPropertyCode.Color).support)
        assertFalse(capability.isBlocked(FujiPropertyCode.Color))
    }

    @Test
    fun `a body that advertises no recipe codes is not treated as having none`() {
        // Some bodies do not enumerate vendor properties. Believing that list would block every
        // write on a camera that works, so DeviceInfo is discarded rather than trusted here.
        val capability = CapabilityResolver.resolve(
            deviceKey = "X-Pro3_0100",
            keyOrigin = KeyOrigin.ReportedByCamera,
            table = null,
            tableVersion = null,
            advertisedProperties = listOf(0x5001, 0xD36B),
        )

        assertTrue(capability.blocked().isEmpty())
        assertEquals(Support.Unknown, capability.of(FujiPropertyCode.Clarity).support)
    }

    @Test
    fun `table fills in support when the camera says nothing`() {
        val capability = CapabilityResolver.resolve(
            deviceKey = "X-T3_0100",
            keyOrigin = KeyOrigin.ReportedByCamera,
            table = tableMatch(
                mapOf(
                    FujiPropertyCode.Clarity to TableProperty(supported = false, variant = null, values = ValueSet.Unknown),
                    FujiPropertyCode.Color to TableProperty(supported = true, variant = null, values = ValueSet.Unknown),
                ),
            ),
            tableVersion = "1.9",
            advertisedProperties = emptyList(),
        )

        // The table's doubt is recorded, but it is not the camera's word — so it never blocks.
        assertEquals(Support.Absent, capability.of(FujiPropertyCode.Clarity).support)
        assertFalse(capability.isBlocked(FujiPropertyCode.Clarity))
        assertTrue(capability.of(FujiPropertyCode.Clarity).isDoubtful)
        assertEquals(listOf(FujiPropertyCode.Clarity), capability.doubtful())
    }

    @Test
    fun `the table cannot veto a property the camera advertises`() {
        // Regression guard. The table's flags name features; the block names property codes; the
        // two are not one to one. `BlackImageTone` is the single-axis toning of the X-T3 era and is
        // correctly false for X-H2Config2 — but 0xD193 carries the warm/cool axis of its successor
        // on that body, confirmed by reading an X-H2 set by hand. Vetoing the code on the strength
        // of the flag would hide a setting the camera has.
        val capability = CapabilityResolver.resolve(
            deviceKey = "X-H2_0200",
            keyOrigin = KeyOrigin.ReportedByCamera,
            table = tableMatch(
                mapOf(
                    FujiPropertyCode.MonoWc to TableProperty(supported = false, variant = null, values = ValueSet.Unknown),
                ),
            ),
            tableVersion = "1.9",
            advertisedProperties = everyRecipeCode,
        )

        val mono = capability.of(FujiPropertyCode.MonoWc)
        assertEquals(Support.Present, mono.support)
        assertEquals(CapabilitySource.Camera, mono.supportSource)
        assertFalse(mono.isDoubtful)
    }

    @Test
    fun `a code missing from DeviceInfo still blocks even when the table says supported`() {
        val capability = CapabilityResolver.resolve(
            deviceKey = "X-H2_0200",
            keyOrigin = KeyOrigin.ReportedByCamera,
            table = tableMatch(
                mapOf(
                    FujiPropertyCode.Clarity to TableProperty(supported = true, variant = null, values = ValueSet.Unknown),
                ),
            ),
            tableVersion = "1.9",
            advertisedProperties = everyRecipeCode - FujiPropertyCode.Clarity.code,
        )

        assertTrue(capability.isBlocked(FujiPropertyCode.Clarity))
        assertEquals(CapabilitySource.Camera, capability.of(FujiPropertyCode.Clarity).supportSource)
    }

    @Test
    fun `descriptor values beat table values`() {
        val descriptor = PtpPropDesc(
            propCode = FujiPropertyCode.FilmSimulation.code,
            dataType = PTP_TYPE_UINT16,
            writable = true,
            factoryDefault = 1,
            current = 1,
            form = PtpPropForm.Enum,
            enumValues = listOf(1, 2, 3),
        )

        val capability = CapabilityResolver.resolve(
            deviceKey = "X-H2_0200",
            keyOrigin = KeyOrigin.ReportedByCamera,
            table = tableMatch(
                mapOf(
                    FujiPropertyCode.FilmSimulation to TableProperty(
                        supported = true,
                        variant = "Std6",
                        values = ValueSet.Enumerated((1..20).toList()),
                    ),
                ),
            ),
            tableVersion = "1.9",
            advertisedProperties = everyRecipeCode,
            descriptors = mapOf(FujiPropertyCode.FilmSimulation.code to descriptor),
        )

        val resolved = capability.of(FujiPropertyCode.FilmSimulation)
        assertEquals(ValueSet.Enumerated(listOf(1, 2, 3)), resolved.values)
        assertEquals(CapabilitySource.Camera, resolved.valuesSource)
    }

    @Test
    fun `a read-only descriptor does not block the property`() {
        // Dynamic Range legitimately reports read-only while D Range Priority is active. Blocking
        // on that flag would permanently hide a setting because of a temporary interlock.
        val descriptor = PtpPropDesc(
            propCode = FujiPropertyCode.DynamicRange.code,
            dataType = PTP_TYPE_UINT16,
            writable = false,
            factoryDefault = 100,
            current = 65535,
            form = PtpPropForm.None,
        )

        val capability = CapabilityResolver.resolve(
            deviceKey = "X-H2_0200",
            keyOrigin = KeyOrigin.ReportedByCamera,
            table = null,
            tableVersion = null,
            advertisedProperties = everyRecipeCode,
            descriptors = mapOf(FujiPropertyCode.DynamicRange.code to descriptor),
        )

        assertFalse(capability.isBlocked(FujiPropertyCode.DynamicRange))
    }

    @Test
    fun `unknown value sets allow everything`() {
        assertTrue(ValueSet.Unknown.allows(Int.MIN_VALUE))
        assertTrue(ValueSet.Unknown.allows(99999))
    }

    @Test
    fun `range respects its step`() {
        val range = ValueSet.Range(2500, 10000, 10)

        assertTrue(range.allows(2500))
        assertTrue(range.allows(5600))
        assertFalse(range.allows(5605))
        assertFalse(range.allows(2490))
        assertEquals(10000, range.clamp(12000))
        assertEquals(2500, range.clamp(1000))
        assertEquals(5600, range.clamp(5604))
    }
}
