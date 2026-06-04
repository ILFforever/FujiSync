package com.paeki.fujirecipes.data.local

import android.content.ContentResolver
import android.content.Context
import com.paeki.fujirecipes.ui.model.LibraryGroupStyle
import com.paeki.fujirecipes.ui.model.LibraryGroupUiModel
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun store(): LocalStore {
        val context: Context = mockk(relaxed = true)
        every { context.filesDir } returns tempFolder.root
        every { context.contentResolver } returns mockk<ContentResolver>(relaxed = true)
        return LocalStore(context)
    }

    @Test
    fun `loadLibrary returns null before library has been saved`() = runTest {
        assertNull(store().loadLibrary())
    }

    @Test
    fun `saveLibrary and loadLibrary round trip recipes groups and styles`() = runTest {
        val store = store()
        val recipe = LibraryRecipeUiModel(
            id = "lib-1",
            name = "Classic Chrome 400",
            sim = "Classic Chrome",
            pills = listOf("DR400%", "GRAIN WK/S"),
            saved = "Jun 03",
            description = "Low contrast daylight",
            effects = mapOf("Dynamic Range" to "DR400%"),
            tone = mapOf("Highlight Tone" to "-1"),
            wb = mapOf("White Balance" to "Daylight"),
            sourceCameraName = "Travel X-T5",
            sourceCameraModel = "X-T5",
            sourceUsbId = "ABC123",
            referenceImageUris = listOf("file:///ref.jpg"),
            groupIds = listOf("group-1"),
            favorite = true,
        )
        val group = LibraryGroupUiModel(id = "group-1", name = "Street")
        val style = LibraryGroupStyle(imageUri = "file:///group.jpg", icon = "Star", color = "Blue")

        store.saveLibrary(
            recipes = listOf(recipe),
            groups = listOf(group),
            styles = mapOf(group.id to style),
        )

        val loaded = store.loadLibrary()
        assertEquals(listOf(recipe), loaded?.recipes)
        assertEquals(listOf(group), loaded?.groups)
        assertEquals(mapOf(group.id to style), loaded?.styles)
    }
}
