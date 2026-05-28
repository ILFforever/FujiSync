package com.paeki.fujirecipes.data.usb

import com.paeki.fujirecipes.data.ptp.CameraPresetName
import com.paeki.fujirecipes.data.ptp.PtpConstants
import com.paeki.fujirecipes.data.ptp.decodeInt16Le
import com.paeki.fujirecipes.data.ptp.decodeUInt16Le
import com.paeki.fujirecipes.data.ptp.encodePtpString
import com.paeki.fujirecipes.data.ptp.parsePtpString
import com.paeki.fujirecipes.data.ptp.uint16Le
import com.paeki.fujirecipes.domain.model.CameraSlot
import com.paeki.fujirecipes.domain.model.FujiFilmSimulation
import com.paeki.fujirecipes.domain.model.FujiPropertyCode
import com.paeki.fujirecipes.domain.model.RecipePreset

class FujiRecipeCamera(
    private val connection: OpenPtpConnection,
) {
    fun readPreset(slot: CameraSlot): RecipePreset {
        selectSlot(slot)
        Thread.sleep(SLOT_SWITCH_DELAY_MS)

        val name = readPresetName().ifBlank { slot.label }
        val properties = mutableMapOf<FujiPropertyCode, Int>()

        for (code in PtpConstants.PRESET_BLOCK_START..PtpConstants.PRESET_BLOCK_END) {
            val transaction = connection.executeCommand(
                code = PtpConstants.GET_DEVICE_PROP_VALUE,
                params = listOf(code),
            )

            val property = FujiPropertyCode.fromCode(code)
            if (transaction.isOk && property != null) {
                val payload = transaction.data?.payload ?: ByteArray(0)
                properties[property] = if (property.signed) {
                    decodeInt16Le(payload)
                } else {
                    decodeUInt16Le(payload)
                }
            }
        }

        return RecipePreset(
            slot = slot,
            name = name,
            properties = properties,
        )
    }

    fun writePresetName(slot: CameraSlot, name: String): Boolean {
        selectSlot(slot)
        Thread.sleep(SLOT_SWITCH_DELAY_MS)

        val safe = CameraPresetName.sanitizeOrFallback(name, fallback = slot.label)
        val transaction = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_PRESET_NAME),
            payload = encodePtpString(safe),
        )
        Thread.sleep(PROPERTY_WRITE_DELAY_MS)
        return transaction.isOk
    }

    fun writeFilmSimulation(slot: CameraSlot, filmSimulation: FujiFilmSimulation): Boolean {
        if (!selectSlot(slot)) return false
        Thread.sleep(PROPERTY_WRITE_DELAY_MS)

        connection.executeCommand(PtpConstants.GET_DEVICE_INFO)

        val transaction = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(FujiPropertyCode.FilmSimulation.code),
            payload = uint16Le(filmSimulation.protocolValue),
        )
        Thread.sleep(PROPERTY_WRITE_DELAY_MS)
        return transaction.isOk
    }

    private fun selectSlot(slot: CameraSlot): Boolean {
        val transaction = connection.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_SLOT_SELECTOR),
            payload = uint16Le(slot.protocolValue),
        )
        return transaction.isOk
    }

    private fun readPresetName(): String {
        val transaction = connection.executeCommand(
            code = PtpConstants.GET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_PRESET_NAME),
        )
        return if (transaction.isOk) {
            parsePtpString(transaction.data?.payload ?: ByteArray(0))
        } else {
            ""
        }
    }

    private companion object {
        const val SLOT_SWITCH_DELAY_MS = 100L
        const val PROPERTY_WRITE_DELAY_MS = 150L
    }
}
