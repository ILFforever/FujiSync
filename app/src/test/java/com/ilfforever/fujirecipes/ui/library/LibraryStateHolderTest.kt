package com.ilfforever.fujirecipes.ui.library

import android.content.Context
import app.cash.turbine.test
import com.ilfforever.fujirecipes.data.local.LocalStore
import com.ilfforever.fujirecipes.data.ptp.CameraPresetName
import com.ilfforever.fujirecipes.ui.model.DuplicateMatchKind
import com.ilfforever.fujirecipes.ui.model.LibraryRecipeSource
import com.ilfforever.fujirecipes.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujirecipes.ui.model.RecipeUiModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * LibraryStateHolder owns all library mutation logic. These tests use an UnconfinedTestDispatcher
 * installed as Dispatchers.Main so that coroutines launched inside the holder's internal scope
 * (SupervisorJob + Dispatchers.Main.immediate) run eagerly — state changes are observable
 * synchronously after each call.
 *
 * LocalStore and Context are relaxed mocks; no real file I/O runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryStateHolderTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val localStore: LocalStore = mockk(relaxed = true)
    private lateinit var holder: LibraryStateHolder

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        holder = LibraryStateHolder(context, localStore, TestScope(testDispatcher))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Test data builders ────────────────────────────────────────────

    private fun recipe(
        id: String = "lib-test",
        name: String = "Test Recipe",
        sim: String = "Provia",
        effects: Map<String, String> = emptyMap(),
        tone: Map<String, String> = emptyMap(),
        wb: Map<String, String> = emptyMap(),
        groupIds: List<String> = emptyList(),
        favorite: Boolean = false,
    ) = LibraryRecipeUiModel(
        id = id, name = name, sim = sim, pills = emptyList(), saved = "Jan 01",
        effects = effects, tone = tone, wb = wb, groupIds = groupIds, favorite = favorite,
    )

    private fun incoming(
        name: String = "Test Recipe",
        sim: String = "Provia",
        effects: Map<String, String> = emptyMap(),
        tone: Map<String, String> = emptyMap(),
        wb: Map<String, String> = emptyMap(),
    ) = RecipeUiModel(
        slot = "C1", name = name, sim = sim, pills = emptyList(),
        effects = effects, tone = tone, wb = wb,
    )

    private val source = LibraryRecipeSource("My Camera", "X-T5", "ABC123")

    /** Seeds the library directly, bypassing duplicate detection. */
    private fun seed(vararg recipes: LibraryRecipeUiModel) =
        recipes.forEach { holder.saveNewRecipe(it) }

    // ── LibraryRecipeName ─────────────────────────────────────────────

    @Test
    fun `recipe name sanitize follows camera-safe rules`() {
        assertEquals("Cafe 400!!!", LibraryRecipeName.sanitize("  Café 🎞️ 400!!!  "))
        assertEquals("Nat's Recipe", LibraryRecipeName.sanitize("Nat’s Recipe"))
    }

    @Test
    fun `recipe name sanitize truncates at camera preset length`() {
        assertEquals(CameraPresetName.MAX_LENGTH, LibraryRecipeName.sanitize("A".repeat(80)).length)
    }

    @Test
    fun `recipe name sanitize falls back when nothing camera-safe remains`() {
        assertEquals("Untitled Recipe", LibraryRecipeName.sanitize("🎞️"))
    }

    // ── addRecipe ─────────────────────────────────────────────────────

    @Test
    fun `addRecipe into empty library returns true and prepends recipe`() {
        assertTrue(holder.addRecipe(incoming(name = "Velvia Street"), source))
        assertEquals("Velvia Street", holder.state.value.recipes.first().name)
        assertNull(holder.state.value.duplicateDialog)
    }

    @Test
    fun `addRecipe stores camera-safe recipe name`() {
        assertTrue(holder.addRecipe(incoming(name = "  Café 🎞️ 400!!!  "), source))

        assertEquals("Cafe 400!!!", holder.state.value.recipes.first().name)
    }

    @Test
    fun `addRecipe with exact settings duplicate shows dialog and returns false`() {
        val fx = mapOf("Grain Effect" to "Weak")
        seed(recipe(id = "existing", name = "Different Name", effects = fx))

        val result = holder.addRecipe(incoming(name = "Incoming Name", effects = fx), source)

        assertFalse(result)
        assertNotNull(holder.state.value.duplicateDialog)
        assertEquals(
            DuplicateMatchKind.ExactSettings,
            holder.state.value.duplicateDialog?.topMatch?.kind,
        )
    }

    @Test
    fun `addRecipe with same name (case-insensitive) shows dialog and returns false`() {
        seed(recipe(name = "Classic Chrome", sim = "Classic Chrome"))

        // Different sim prevents exact-settings match; same name triggers SameName match
        val result = holder.addRecipe(incoming(name = "classic chrome", sim = "Velvia"), source)

        assertFalse(result)
        assertEquals(
            DuplicateMatchKind.SameName,
            holder.state.value.duplicateDialog?.topMatch?.kind,
        )
    }

    @Test
    fun `addRecipe duplicate check compares camera-normalized names`() {
        seed(recipe(name = "Cafe 400!!!", sim = "Classic Chrome"))

        val result = holder.addRecipe(incoming(name = "Café 🎞️ 400!!!", sim = "Velvia"), source)

        assertFalse(result)
        assertEquals(
            DuplicateMatchKind.SameName,
            holder.state.value.duplicateDialog?.topMatch?.kind,
        )
    }

    @Test
    fun `addRecipe with unrelated recipe adds it and does not show dialog`() {
        seed(recipe(name = "Provia Base", sim = "Provia", effects = mapOf("Grain" to "Off")))

        val result = holder.addRecipe(
            incoming(name = "Velvia Vivid", sim = "Velvia", effects = mapOf("Color" to "+4")),
            source,
        )

        assertTrue(result)
        assertNull(holder.state.value.duplicateDialog)
        assertEquals(2, holder.state.value.recipes.size)
    }

    // ── Duplicate resolution ──────────────────────────────────────────

    @Test
    fun `handleDuplicateSaveAsNew clears dialog and adds recipe`() {
        seed(recipe(id = "old", name = "Classic Chrome", sim = "Classic Chrome"))
        holder.addRecipe(incoming(name = "Classic Chrome", sim = "Velvia"), source)

        holder.handleDuplicateSaveAsNew()

        assertNull(holder.state.value.duplicateDialog)
        assertEquals(2, holder.state.value.recipes.size)
    }

    @Test
    fun `handleDuplicateUpdateExisting replaces recipe content and preserves groupIds`() {
        val existingGroupIds = listOf("group-a", "group-b")
        seed(recipe(id = "existing", name = "Old Name", groupIds = existingGroupIds))
        holder.addRecipe(
            incoming(name = "Old Name", sim = "Velvia", tone = mapOf("Sharpness" to "+2")),
            source,
        )

        holder.handleDuplicateUpdateExisting("existing")

        assertNull(holder.state.value.duplicateDialog)
        val updated = holder.state.value.recipes.first { it.id == "existing" }
        assertEquals(existingGroupIds, updated.groupIds)
        assertEquals(mapOf("Sharpness" to "+2"), updated.tone)
    }

    @Test
    fun `dismissDuplicate clears dialog without adding or removing recipes`() {
        seed(recipe(name = "Classic Chrome", sim = "Classic Chrome"))
        holder.addRecipe(incoming(name = "Classic Chrome", sim = "Velvia"), source)
        val sizeBefore = holder.state.value.recipes.size

        holder.dismissDuplicate()

        assertNull(holder.state.value.duplicateDialog)
        assertEquals(sizeBefore, holder.state.value.recipes.size)
    }

    // ── CRUD ──────────────────────────────────────────────────────────

    @Test
    fun `deleteRecipes removes specified IDs and keeps others`() {
        seed(recipe(id = "a"), recipe(id = "b"), recipe(id = "c"))

        holder.deleteRecipes(setOf("a", "c"))

        val ids = holder.state.value.recipes.map { it.id }
        assertFalse("a" in ids)
        assertFalse("c" in ids)
        assertTrue("b" in ids)
    }

    @Test
    fun `deleteRecipes with empty set is a no-op`() {
        seed(recipe(id = "a"), recipe(id = "b"))
        holder.deleteRecipes(emptySet())
        assertEquals(2, holder.state.value.recipes.size)
    }

    @Test
    fun `cloneRecipe keeps same name and assigns a new ID`() {
        seed(recipe(id = "orig", name = "Eterna Flat"))

        holder.cloneRecipe(RecipeUiModel(slot = "", name = "Eterna Flat", sim = "Eterna", pills = emptyList(), libraryId = "orig"))

        val clone = holder.state.value.recipes.first()
        assertEquals("Eterna Flat", clone.name)
        assertNotEquals("orig", clone.id)
        assertEquals(2, holder.state.value.recipes.size)
    }

    @Test
    fun `toggleFavorite flips the flag and can be reversed`() {
        seed(recipe(id = "r1", favorite = false))

        holder.toggleFavorite("r1")
        assertTrue(holder.state.value.recipes.first { it.id == "r1" }.favorite)

        holder.toggleFavorite("r1")
        assertFalse(holder.state.value.recipes.first { it.id == "r1" }.favorite)
    }

    @Test
    fun `updateRecipe patches name sim and tone on the matching recipe only`() {
        seed(recipe(id = "r1", name = "Old Name", sim = "Provia"), recipe(id = "r2", name = "Untouched"))

        holder.updateRecipe(RecipeUiModel(
            libraryId = "r1", slot = "", name = "New Name", sim = "Velvia",
            pills = listOf("Velvia"), tone = mapOf("Sharpness" to "+1"),
        ))

        val r1 = holder.state.value.recipes.first { it.id == "r1" }
        assertEquals("New Name", r1.name)
        assertEquals("Velvia", r1.sim)
        assertEquals(mapOf("Sharpness" to "+1"), r1.tone)
        assertEquals("Untouched", holder.state.value.recipes.first { it.id == "r2" }.name)
    }

    @Test
    fun `updateRecipe stores camera-safe recipe name`() {
        seed(recipe(id = "r1", name = "Old Name", sim = "Provia"))

        holder.updateRecipe(RecipeUiModel(
            libraryId = "r1", slot = "", name = "Café 🎞️ 400!!!", sim = "Velvia",
            pills = listOf("Velvia"),
        ))

        assertEquals("Cafe 400!!!", holder.state.value.recipes.first { it.id == "r1" }.name)
    }

    @Test
    fun `saveNewRecipe stores camera-safe recipe name`() {
        holder.saveNewRecipe(recipe(name = "Café 🎞️ 400!!!"))

        assertEquals("Cafe 400!!!", holder.state.value.recipes.first().name)
    }

    // ── Groups ────────────────────────────────────────────────────────

    @Test
    fun `changeRecipeGroup adds groupId to recipe that does not have it`() {
        seed(recipe(id = "r1", groupIds = emptyList()))

        holder.changeRecipeGroup("r1", "group-x")

        assertEquals(listOf("group-x"), holder.state.value.recipes.first { it.id == "r1" }.groupIds)
    }

    @Test
    fun `changeRecipeGroup removes groupId from recipe that already has it (toggle)`() {
        seed(recipe(id = "r1", groupIds = listOf("group-x")))

        holder.changeRecipeGroup("r1", "group-x")

        assertTrue(holder.state.value.recipes.first { it.id == "r1" }.groupIds.isEmpty())
    }

    @Test
    fun `createGroup appends a new group with the given name`() {
        holder.createGroup("Street Portraits")
        assertEquals(1, holder.state.value.groups.size)
        assertEquals("Street Portraits", holder.state.value.groups.first().name)
    }

    @Test
    fun `createGroup with blank name after sanitize is a no-op`() {
        holder.createGroup("   ")
        assertTrue(holder.state.value.groups.isEmpty())
    }

    @Test
    fun `deleteGroup removes group from list and clears it from recipe groupIds`() {
        holder.createGroup("Test Group")
        val gid = holder.state.value.groups.first().id
        seed(recipe(id = "r1", groupIds = listOf(gid)))

        holder.deleteGroup(gid)

        assertFalse(holder.state.value.groups.any { it.id == gid })
        assertTrue(holder.state.value.recipes.first { it.id == "r1" }.groupIds.isEmpty())
    }

    @Test
    fun `deleteGroup with the protected DEFAULT id is a no-op`() {
        val sizeBefore = holder.state.value.groups.size
        holder.deleteGroup("group-library")
        assertEquals(sizeBefore, holder.state.value.groups.size)
    }

    // ── State flow emissions (turbine) ────────────────────────────────

    @Test
    fun `state emits updated recipes list after deleteRecipes`() = runTest(testDispatcher) {
        seed(recipe(id = "to-delete", name = "A"), recipe(id = "to-keep", name = "B"))

        holder.state.test {
            awaitItem() // current state — 2 recipes

            holder.deleteRecipes(setOf("to-delete"))

            val next = awaitItem()
            assertEquals(1, next.recipes.size)
            assertEquals("to-keep", next.recipes.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── WB normalization utilities ────────────────────────────────────

    @Test
    fun `parseWhiteBalanceValue returns balance unchanged when no shifts present`() {
        val result = parseWhiteBalanceValue("Auto")
        assertEquals("Auto", result.balance)
        assertNull(result.shiftR)
        assertNull(result.shiftB)
    }

    @Test
    fun `parseWhiteBalanceValue extracts R and B shifts from embedded string`() {
        val result = parseWhiteBalanceValue("Auto, +3 Red +1 Blue")
        assertEquals("Auto", result.balance)
        assertEquals("+3", result.shiftR)
        assertEquals("+1", result.shiftB)
    }

    @Test
    fun `parseWhiteBalanceValue handles negative shifts`() {
        val result = parseWhiteBalanceValue("Daylight, -2 Red -4 Blue")
        assertEquals("Daylight", result.balance)
        assertEquals("-2", result.shiftR)
        assertEquals("-4", result.shiftB)
    }

    @Test
    fun `normalizedWhiteBalanceLabel remaps all known aliases`() {
        assertEquals("Auto White Priority", "Auto (White Priority)".normalizedWhiteBalanceLabel())
        assertEquals("Ambience Priority", "Auto (Ambience)".normalizedWhiteBalanceLabel())
        assertEquals("Incandescent", "Tungsten".normalizedWhiteBalanceLabel())
    }

    @Test
    fun `normalizedWhiteBalanceLabel passes through unknown labels unchanged`() {
        assertEquals("Daylight", "Daylight".normalizedWhiteBalanceLabel())
        assertEquals("Kelvin", "Kelvin".normalizedWhiteBalanceLabel())
    }

    @Test
    fun `normalizedFxwParams renames all known keys`() {
        val params = mapOf(
            "Color Chrome Effect" to "Weak",
            "Highlight" to "+1",
            "Shadow" to "-1",
            "Noise Reduction" to "Normal",
            "Grain Effect" to "Strong Small",
            "Dynamic Range" to "DR400",
            "Dynamic Range Priority" to "weak",
        )
        val result = params.normalizedFxwParams()

        assertEquals("Weak", result["Color Chrome"])
        assertEquals("+1", result["Highlight Tone"])
        assertEquals("-1", result["Shadow Tone"])
        assertEquals("Normal", result["High ISO NR"])
        assertEquals("Strong Small", result["Grain Effect"])
        assertEquals("DR400%", result["Dynamic Range"])
        assertEquals("Weak", result["D Range Priority"])
        assertFalse("Color Chrome Effect" in result)
        assertFalse("Highlight" in result)
        assertFalse("Shadow" in result)
        assertFalse("Noise Reduction" in result)
        assertFalse("Dynamic Range Priority" in result)
    }

    @Test
    fun `normalizedFxwParams splits White Balance with shifts into separate WB keys`() {
        val params = mapOf("White Balance" to "Auto, +3 Red +1 Blue")
        val result = params.normalizedFxwParams()

        assertEquals("Auto", result["White Balance"])
        assertEquals("+3", result["WB Shift R"])
        assertEquals("+1", result["WB Shift B"])
    }

    @Test
    fun `normalizedWhiteBalanceSection splits embedded shift into separate WB Shift keys`() {
        val wb = mapOf("White Balance" to "Auto, +3 Red +1 Blue", "Other" to "value")
        val result = wb.normalizedWhiteBalanceSection()

        assertEquals("Auto", result["White Balance"])
        assertEquals("+3", result["WB Shift R"])
        assertEquals("+1", result["WB Shift B"])
        assertEquals("value", result["Other"])
    }

    @Test
    fun `normalizedWhiteBalanceSection returns the same object when nothing changes`() {
        val wb = mapOf("White Balance" to "Daylight")
        assertSame(wb, wb.normalizedWhiteBalanceSection())
    }
}
