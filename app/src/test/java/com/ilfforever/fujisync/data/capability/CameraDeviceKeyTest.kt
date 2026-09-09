package com.ilfforever.fujisync.data.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraDeviceKeyTest {

    @Test
    fun `accepts a well-formed identity string`() {
        assertEquals("X-H2_0200", CameraDeviceKey.fromReported("X-H2_0200"))
        assertEquals("GFX100RF_0100", CameraDeviceKey.fromReported(" GFX100RF_0100 "))
    }

    @Test
    fun `rejects anything that is not key-shaped`() {
        // 0xD186 is read as a string; a body that answers with something else must not have that
        // answer turned into a lookup key.
        assertNull(CameraDeviceKey.fromReported(null))
        assertNull(CameraDeviceKey.fromReported(""))
        assertNull(CameraDeviceKey.fromReported("X-H2"))
        assertNull(CameraDeviceKey.fromReported("100,100,0,0"))
        assertNull(CameraDeviceKey.fromReported("X-H2_02"))
    }

    @Test
    fun `synthesises the first generation the way Fuji's own client does`() {
        assertEquals("X-T3_0100", CameraDeviceKey.synthesize("X-T3"))
        assertEquals("X-T3_0100", CameraDeviceKey.synthesize("FUJIFILM X-T3"))
        assertNull(CameraDeviceKey.synthesize(""))
        assertNull(CameraDeviceKey.synthesize(null))
    }

    @Test
    fun `splits a key into model and generation`() {
        assertEquals("X-H2" to 200, CameraDeviceKey.split("X-H2_0200"))
        assertEquals("GFX100S" to 100, CameraDeviceKey.split("GFX100S_0100"))
        assertNull(CameraDeviceKey.split("nonsense"))
    }

    @Test
    fun `canonical model ignores case and separators but keeps distinct models apart`() {
        assertEquals(
            CameraDeviceKey.canonicalModel("X-T5"),
            CameraDeviceKey.canonicalModel("x t5"),
        )
        assertEquals("XT30II", CameraDeviceKey.canonicalModel("X-T30II"))
        assert(CameraDeviceKey.canonicalModel("X-T5") != CameraDeviceKey.canonicalModel("X-T50"))
        assert(CameraDeviceKey.canonicalModel("X-H2") != CameraDeviceKey.canonicalModel("X-H2S"))
    }
}
