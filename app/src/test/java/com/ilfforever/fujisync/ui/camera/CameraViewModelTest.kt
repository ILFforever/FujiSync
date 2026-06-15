package com.ilfforever.fujisync.ui.camera

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import app.cash.turbine.test
import com.ilfforever.fujisync.data.local.LocalStore
import com.ilfforever.fujisync.data.usb.CameraHeartbeat
import com.ilfforever.fujisync.data.usb.CameraSessionManager
import com.ilfforever.fujisync.data.usb.CameraUsbMode
import com.ilfforever.fujisync.data.usb.FujiRecipeCamera
import com.ilfforever.fujisync.data.usb.FujiUsbDevice
import com.ilfforever.fujisync.data.usb.UsbPtpConnection
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.repository.CameraRepository
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockContext: android.content.Context = mockk(relaxed = true)
    private val usbManager: UsbManager = mockk(relaxed = true)
    private val repository: CameraRepository = mockk(relaxed = true)
    private val connectionFactory: UsbPtpConnection = mockk(relaxed = true)
    private val sessionManager: CameraSessionManager = mockk(relaxed = true)
    private val localStore: LocalStore = mockk(relaxed = true)
    private val heartbeat: CameraHeartbeat = mockk(relaxed = true)
    private val mockDevice: UsbDevice = mockk(relaxed = true)

    private lateinit var vm: CameraViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { mockContext.getString(any()) } returns ""
        every { mockContext.getString(any(), any()) } returns ""
        every { mockContext.getString(com.ilfforever.fujisync.R.string.error_usb_permission_denied) } returns "USB permission denied."
        every { mockContext.getString(com.ilfforever.fujisync.R.string.error_connect_before_rearrange) } returns "Connect camera before rearranging recipes."
        every { mockContext.getString(com.ilfforever.fujisync.R.string.error_connect_before_backup) } returns "Connect a camera before backing up slots."
        every { mockContext.getString(com.ilfforever.fujisync.R.string.error_connect_before_restore) } returns "Connect camera before restoring a set."
        every { repository.scanUsb() } returns emptyList()
        every { heartbeat.usbMutex } returns kotlinx.coroutines.sync.Mutex()
        every { heartbeat.slots } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        every { heartbeat.alive } returns kotlinx.coroutines.flow.MutableStateFlow(false)
        coEvery { localStore.loadSlotBackupSets() } returns emptyList()
        coEvery { localStore.loadCameraLabels() } returns emptyMap()
        coEvery { localStore.loadCameraModels() } returns emptyMap()
        coEvery { localStore.loadCameraFirmwares() } returns emptyMap()
        vm = CameraViewModel(mockContext, usbManager, repository, connectionFactory, sessionManager, localStore, heartbeat, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun ptpDevice() = FujiUsbDevice(
        device = mockDevice, deviceName = "Fuji", productName = "X-T5",
        vendorId = 0x04CB, productId = 0x02E1, mode = CameraUsbMode.Ptp,
    )

    // ── refreshDevices ────────────────────────────────────────────────

    @Test
    fun `refreshDevices with no PTP device sets connected false`() {
        every { repository.scanUsb() } returns listOf(FujiUsbDevice(mockDevice, "Fuji", "X-T5", 0x04CB, 0x02E1, CameraUsbMode.CardReader))
        vm.refreshDevices()
        assertFalse(vm.state.value.connected)
    }

    // ── onReconnect ───────────────────────────────────────────────────

    // NOTE: onReconnect enters a coroutine with Dispatchers.IO which cannot be intercepted
    // without injecting a CoroutineDispatcher. Full async path tests require dispatcher injection.
    // TODO: Add dispatcher injection to CameraViewModel for full async testability.

    // ── onUsbPermissionResult ─────────────────────────────────────────

    @Test
    fun `onUsbPermissionResult denied shows error`() {
        vm.onUsbPermissionResult(mockDevice, granted = false)
        assertEquals("USB permission denied.", vm.state.value.scanError)
        assertFalse(vm.writeBusy.value)
    }

    // ── handleWrite ───────────────────────────────────────────────────

    @Test
    fun `handleWrite with no camera connected sets writeBusy false`() {
        every { repository.scanUsb() } returns emptyList()
        val recipe = RecipeUiModel(slot = "C1", name = "Test", sim = "Provia", pills = emptyList())

        vm.handleWrite(recipe)

        assertFalse(vm.writeBusy.value)
    }

    @Test
    fun `handleWrite with permission but no device no-ops`() {
        every { repository.scanUsb() } returns listOf(ptpDevice())
        every { usbManager.hasPermission(mockDevice) } returns true
        // Not connected, so write short-circuits
        val recipe = RecipeUiModel(slot = "C1", name = "Portra", sim = "Classic Chrome", pills = emptyList())
        vm.handleWrite(recipe)
        assertFalse(vm.writeBusy.value)
    }

    @Test
    fun `handleWrite with no connected camera resets writeBusy`() {
        every { repository.scanUsb() } returns emptyList()
        vm.exploreDemo()
        // Disconnect
        val stateField = CameraViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.ilfforever.fujisync.ui.CameraUiState>
        stateFlow.value = stateFlow.value.copy(connected = false)

        val recipe = RecipeUiModel(slot = "C1", name = "Test", sim = "Provia", pills = emptyList())
        vm.handleWrite(recipe)

        assertFalse(vm.writeBusy.value)
    }

    // ── handleWriteToSlot ─────────────────────────────────────────────

    @Test
    fun `handleWriteToSlot with no PTP device sets writeBusy false`() {
        every { repository.scanUsb() } returns emptyList()
        val recipe = RecipeUiModel(slot = "", name = "Test", sim = "Provia", pills = emptyList())

        vm.handleWriteToSlot(recipe, "C3")

        assertFalse(vm.writeBusy.value)
    }

    // ── handleRearrangeCameraSlots ────────────────────────────────────

    @Test
    fun `rearrange with wrong slot count does nothing`() {
        val slots = listOf(RecipeUiModel(slot = "C1", name = "A", sim = "Provia", pills = emptyList()))
        vm.handleRearrangeCameraSlots(slots)
        // Should not crash and writeBusy should remain false
        assertFalse(vm.writeBusy.value)
    }

    @Test
    fun `rearrange with no camera connected shows error`() {
        every { repository.scanUsb() } returns emptyList()
        vm.exploreDemo() // sets connected + slots

        val nextSlots = CameraSlot.entries.map { RecipeUiModel(slot = it.label, name = "New", sim = "Velvia", pills = emptyList()) }
        // Disconnect camera for the rearrange call
        vm.clearScanError()
        every { repository.scanUsb() } returns emptyList()
        // Need to explicitly set connected=false for rearrange check
        // Use exploreDemo slots but simulate disconnect
        val stateField = CameraViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.ilfforever.fujisync.ui.CameraUiState>
        stateFlow.value = stateFlow.value.copy(connected = false)

        vm.handleRearrangeCameraSlots(nextSlots)

        assertTrue(vm.state.value.scanError!!.contains("Connect camera"))
    }

    // ── dismissRearrangeValidation ────────────────────────────────────

    @Test
    fun `dismissRearrangeValidation clears flag`() {
        vm.exploreDemo()
        // Manually trigger the flag via reflection since there's no public setter
        val stateField = CameraViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.ilfforever.fujisync.ui.CameraUiState>
        flow.value = flow.value.copy(isRearrangeValidation = true)

        vm.dismissRearrangeValidation()

        assertFalse(vm.state.value.isRearrangeValidation)
    }

    // ── setSelectedSlotIdx ────────────────────────────────────────────

    @Test
    fun `setSelectedSlotIdx updates state`() {
        vm.setSelectedSlotIdx(3)
        assertEquals(3, vm.state.value.selectedSlotIdx)
    }

    // ── setShowImageTuner ─────────────────────────────────────────────

    @Test
    fun `setShowImageTuner updates state`() {
        vm.setShowImageTuner(true)
        assertTrue(vm.state.value.showImageTuner)
        vm.setShowImageTuner(false)
        assertFalse(vm.state.value.showImageTuner)
    }

    // ── Camera labels ─────────────────────────────────────────────────

    @Test
    fun `renameCameraLabel updates labels map`() {
        vm.renameCameraLabel("SERIAL1", "My X-T5")
        assertEquals("My X-T5", vm.state.value.cameraLabels["SERIAL1"])
    }

    @Test
    fun `renameCameraLabel with blank serial does nothing`() {
        vm.renameCameraLabel("", "Test")
        assertTrue(vm.state.value.cameraLabels.isEmpty())
    }

    @Test
    fun `renameCameraLabel with blank label defaults to My Camera`() {
        vm.renameCameraLabel("S1", "   ")
        assertEquals("My Camera", vm.state.value.cameraLabels["S1"])
    }

    @Test
    fun `deleteCamera removes from all maps`() {
        val stateField = CameraViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.ilfforever.fujisync.ui.CameraUiState>
        stateFlow.value = stateFlow.value.copy(
            cameraLabels = mapOf("S1" to "Cam1"),
            cameraModels = mapOf("S1" to "X-H2"),
            cameraFirmwares = mapOf("S1" to "5.20"),
        )

        vm.deleteCamera("S1")

        assertFalse(vm.state.value.cameraLabels.containsKey("S1"))
        assertFalse(vm.state.value.cameraModels.containsKey("S1"))
        assertFalse(vm.state.value.cameraFirmwares.containsKey("S1"))
    }

    @Test
    fun `resetCameraLabel restores model name`() {
        val stateField = CameraViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.ilfforever.fujisync.ui.CameraUiState>
        stateFlow.value = stateFlow.value.copy(
            cameraLabels = mapOf("S1" to "Renamed"),
            cameraModels = mapOf("S1" to "X-T5"),
        )

        vm.resetCameraLabel("S1")

        assertEquals("X-T5", vm.state.value.cameraLabels["S1"])
    }

    // ── addMockCamera ─────────────────────────────────────────────────

    @Test
    fun `addMockCamera adds a new camera`() {
        val before = vm.state.value.cameraLabels.size
        vm.addMockCamera()
        assertEquals(before + 1, vm.state.value.cameraLabels.size)
    }

    // ── exploreDemo ───────────────────────────────────────────────────

    @Test
    fun `exploreDemo sets connected and sample slots`() {
        vm.exploreDemo()
        assertTrue(vm.state.value.connected)
        assertEquals("X-H2S", vm.state.value.cameraModel)
        assertTrue(vm.state.value.slots.isNotEmpty())
    }

    // ── Slot backup ───────────────────────────────────────────────────

    @Test
    fun `handleBackupSlots with no camera shows error`() {
        every { repository.scanUsb() } returns emptyList()
        vm.handleBackupSlots("My Backup")
        assertTrue(vm.state.value.scanError!!.contains("Connect a camera"))
    }

    @Test
    fun `handleSelectSlotBackup updates selected backup`() {
        val stateField = CameraViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.ilfforever.fujisync.ui.CameraUiState>
        val meta = com.ilfforever.fujisync.ui.model.SlotBackupMeta(id = "b1", label = "Backup 1", savedAt = "Jun 1")
        val slots = listOf(RecipeUiModel(slot = "C1", name = "R1", sim = "Provia", pills = emptyList()))
        val set = com.ilfforever.fujisync.ui.model.SlotBackupSet(meta = meta, slots = slots)
        stateFlow.value = stateFlow.value.copy(slotBackupSets = listOf(set))

        vm.handleSelectSlotBackup("b1")

        assertEquals(meta, vm.state.value.slotBackupMeta)
        assertEquals(slots, vm.state.value.slotBackupSlots)
    }

    // ── handleRestoreSlots ────────────────────────────────────────────

    @Test
    fun `handleRestoreSlots with no connection shows error`() = runTest {
        vm.exploreDemo()
        advanceUntilIdle()

        // Simulate: have backup slots but camera disconnected
        val stateField = CameraViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.ilfforever.fujisync.ui.CameraUiState>
        stateFlow.value = stateFlow.value.copy(
            connected = false,
            slotBackupSlots = listOf(RecipeUiModel(slot = "C1", name = "R", sim = "Provia", pills = emptyList())),
        )
        every { repository.scanUsb() } returns emptyList()

        vm.handleRestoreSlots()
        advanceUntilIdle()

        assertEquals("Connect camera before restoring a set.", vm.state.value.scanError)
    }

    // ── clearScanError ────────────────────────────────────────────────

    @Test
    fun `clearScanError clears the error`() {
        val stateField = CameraViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.ilfforever.fujisync.ui.CameraUiState>
        stateFlow.value = stateFlow.value.copy(scanError = "Some error")

        vm.clearScanError()

        assertNull(vm.state.value.scanError)
    }
}
