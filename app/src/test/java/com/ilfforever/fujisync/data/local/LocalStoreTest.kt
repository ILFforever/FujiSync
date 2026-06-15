package com.ilfforever.fujirecipes.data.local

import android.content.ContentResolver
import android.content.Context
import com.ilfforever.fujirecipes.ui.model.LibraryGroupStyle
import com.ilfforever.fujirecipes.ui.model.LibraryGroupUiModel
import com.ilfforever.fujirecipes.ui.model.LibraryRecipeUiModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class LocalStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun store(): LocalStore {
        val context: Context = mockk(relaxed = true)
        every { context.filesDir } returns tempFolder.root
        every { context.contentResolver } returns mockk<ContentResolver>(relaxed = true)
        return LocalStore(context)
    }

    private fun storeIn(dir: File): LocalStore {
        val context: Context = mockk(relaxed = true)
        every { context.filesDir } returns dir
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

    @Test
    fun `backupZip then restoreBackupZip preserves reference images on a fresh device`() = runTest {
        // ── Source "device" ──────────────────────────────────────────
        val srcDir = tempFolder.newFolder("src")
        val srcStore = storeIn(srcDir)

        // Two reference images + one group cover image, with known content.
        val refImagesDir = File(srcDir, "ref_images").also { it.mkdirs() }
        val img1 = File(refImagesDir, "recipe-image-1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
        val img2 = File(refImagesDir, "recipe-image-2.jpg").apply { writeBytes(byteArrayOf(10, 20, 30)) }
        val cover = File(refImagesDir, "group-cover.jpg").apply { writeBytes(byteArrayOf(99, 98, 97, 96)) }

        fun uriOf(f: File) = "file://" + f.absolutePath

        val recipe = LibraryRecipeUiModel(
            id = "lib-1",
            name = "Classic Chrome 400",
            sim = "Classic Chrome",
            pills = listOf("DR400%"),
            saved = "Jun 03",
            referenceImageUris = listOf(uriOf(img1), uriOf(img2)),
            groupIds = listOf("group-1"),
        )
        val group = LibraryGroupUiModel(id = "group-1", name = "Street")
        val style = LibraryGroupStyle(imageUri = uriOf(cover), icon = "Star", color = "Blue")
        val libraryData = LocalStore.LibraryData(
            recipes = listOf(recipe),
            groups = listOf(group),
            styles = mapOf(group.id to style),
        )

        val zipBytes = ByteArrayOutputStream().use { out ->
            srcStore.backupZip(
                outputStream = out,
                settings = com.ilfforever.fujirecipes.ui.model.AppSettings(),
                cameraLabels = emptyMap(),
                cameraModels = emptyMap(),
                cameraFirmwares = emptyMap(),
                libraryData = libraryData,
            )
            out.toByteArray()
        }

        // ── Destination "device" (no pre-existing images) ────────────
        val dstDir = tempFolder.newFolder("dst")
        val dstStore = storeIn(dstDir)

        val restored = dstStore.restoreBackupZip(ByteArrayInputStream(zipBytes))

        // Recipe URIs are remapped to the destination device and the files exist with identical content.
        val restoredRecipe = restored.library.recipes.single()
        assertEquals(2, restoredRecipe.referenceImageUris.size)
        restoredRecipe.referenceImageUris.forEach { uri ->
            assertTrue("expected file:// uri, got $uri", uri.startsWith("file://"))
            val f = File(uri.removePrefix("file://"))
            assertTrue("restored image should exist: $f", f.exists())
            assertTrue("restored image should be inside dst dir", f.absolutePath.startsWith(dstDir.absolutePath))
        }
        val r1 = File(restoredRecipe.referenceImageUris[0].removePrefix("file://"))
        val r2 = File(restoredRecipe.referenceImageUris[1].removePrefix("file://"))
        assertEquals(listOf<Byte>(1, 2, 3, 4, 5), r1.readBytes().toList())
        assertEquals(listOf<Byte>(10, 20, 30), r2.readBytes().toList())

        // Group cover image is remapped and content preserved.
        val restoredCover = File(restored.library.styles.getValue("group-1").imageUri!!.removePrefix("file://"))
        assertTrue("restored cover should exist", restoredCover.exists())
        assertEquals(listOf<Byte>(99, 98, 97, 96), restoredCover.readBytes().toList())
    }

    @Test
    fun `restoreBackupZip skips missing source images without failing`() = runTest {
        val srcDir = tempFolder.newFolder("src2")
        val srcStore = storeIn(srcDir)

        // Recipe references an image that does not exist on disk.
        val recipe = LibraryRecipeUiModel(
            id = "lib-1",
            name = "Ghost",
            sim = "Provia",
            pills = emptyList(),
            saved = "Jun 03",
            referenceImageUris = listOf("file://" + File(srcDir, "ref_images/missing.jpg").absolutePath),
        )
        val libraryData = LocalStore.LibraryData(
            recipes = listOf(recipe),
            groups = emptyList(),
            styles = emptyMap(),
        )

        val zipBytes = ByteArrayOutputStream().use { out ->
            srcStore.backupZip(
                outputStream = out,
                settings = com.ilfforever.fujirecipes.ui.model.AppSettings(),
                cameraLabels = emptyMap(),
                cameraModels = emptyMap(),
                cameraFirmwares = emptyMap(),
                libraryData = libraryData,
            )
            out.toByteArray()
        }

        val dstStore = storeIn(tempFolder.newFolder("dst2"))
        val restored = dstStore.restoreBackupZip(ByteArrayInputStream(zipBytes))

        // The recipe survives; the missing image URI is left untouched (no crash).
        assertEquals(1, restored.library.recipes.size)
        assertEquals(1, restored.library.recipes.single().referenceImageUris.size)
    }

    @Test
    fun `backup handles images that share a basename across recipes`() = runTest {
        // Reproduces the real bug: discover recipes store images as
        // references/<slug>/image_0.jpg, so basenames collide across recipes.
        val srcDir = tempFolder.newFolder("src3")
        val srcStore = storeIn(srcDir)

        val dirA = File(srcDir, "references/recipe-a").also { it.mkdirs() }
        val dirB = File(srcDir, "references/recipe-b").also { it.mkdirs() }
        val a0 = File(dirA, "image_0.jpg").apply { writeBytes(byteArrayOf(1, 1, 1)) }
        val b0 = File(dirB, "image_0.jpg").apply { writeBytes(byteArrayOf(2, 2, 2)) }

        fun uriOf(f: File) = "file://" + f.absolutePath

        val recipeA = LibraryRecipeUiModel(
            id = "a", name = "A", sim = "Provia", pills = emptyList(), saved = "x",
            referenceImageUris = listOf(uriOf(a0)),
        )
        val recipeB = LibraryRecipeUiModel(
            id = "b", name = "B", sim = "Provia", pills = emptyList(), saved = "x",
            referenceImageUris = listOf(uriOf(b0)),
        )
        val libraryData = LocalStore.LibraryData(
            recipes = listOf(recipeA, recipeB),
            groups = emptyList(),
            styles = emptyMap(),
        )

        // Must not throw "duplicate entry".
        val zipBytes = ByteArrayOutputStream().use { out ->
            srcStore.backupZip(
                outputStream = out,
                settings = com.ilfforever.fujirecipes.ui.model.AppSettings(),
                cameraLabels = emptyMap(),
                cameraModels = emptyMap(),
                cameraFirmwares = emptyMap(),
                libraryData = libraryData,
            )
            out.toByteArray()
        }

        val dstStore = storeIn(tempFolder.newFolder("dst3"))
        val restored = dstStore.restoreBackupZip(ByteArrayInputStream(zipBytes))

        // Each recipe keeps its OWN distinct image content (no cross-contamination).
        val rA = restored.library.recipes.single { it.id == "a" }
        val rB = restored.library.recipes.single { it.id == "b" }
        val fileA = File(rA.referenceImageUris.single().removePrefix("file://"))
        val fileB = File(rB.referenceImageUris.single().removePrefix("file://"))
        assertEquals(listOf<Byte>(1, 1, 1), fileA.readBytes().toList())
        assertEquals(listOf<Byte>(2, 2, 2), fileB.readBytes().toList())
    }
}
