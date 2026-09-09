package com.ilfforever.fujisync.ui.dev

import androidx.lifecycle.ViewModel
import com.ilfforever.fujisync.data.capability.CameraCapability
import com.ilfforever.fujisync.data.capability.CapabilityResolver
import com.ilfforever.fujisync.data.capability.KeyOrigin
import com.ilfforever.fujisync.data.capability.XrfcCapabilityTable
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Resolves a capability for a body that is not attached, straight out of the shipped table.
 *
 * Deliberately table-only: no DeviceInfo list is supplied, so nothing here is ever camera-sourced
 * and the bench shows the *weakest* case — what the app knows about a body it has only read about.
 * A real connection can only ever do better than this.
 */
@HiltViewModel
class CapabilityBenchViewModel @Inject constructor(
    private val table: XrfcCapabilityTable,
) : ViewModel() {

    val tableLoaded: Boolean get() = table.isLoaded
    val tableVersion: String? get() = table.version

    fun capabilityFor(deviceKey: String): CameraCapability = CapabilityResolver.resolve(
        deviceKey = deviceKey,
        keyOrigin = KeyOrigin.ReportedByCamera,
        table = table.lookup(deviceKey),
        tableVersion = table.version,
        advertisedProperties = emptyList(),
    )
}
