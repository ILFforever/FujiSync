package com.ilfforever.fujisync.ui.library

import android.content.Context
import android.net.Uri
import com.ilfforever.fujisync.data.local.LocalStore
import com.ilfforever.fujisync.data.ptp.CameraPresetName
import com.ilfforever.fujisync.data.remote.FxwRecipe
import com.ilfforever.fujisync.domain.model.canonicalFilmSimLabel
import com.ilfforever.fujisync.di.ApplicationScope
import com.ilfforever.fujisync.ui.LibraryUiState
import com.ilfforever.fujisync.ui.UiTimings
import com.ilfforever.fujisync.ui.model.DuplicateDialogState
import com.ilfforever.fujisync.ui.model.DuplicateMatch
import com.ilfforever.fujisync.ui.model.DuplicateMatchKind
import com.ilfforever.fujisync.ui.model.SaveAllReport
import com.ilfforever.fujisync.ui.model.SaveAllSkipped
import com.ilfforever.fujisync.ui.model.LibraryGroupStyle
import com.ilfforever.fujisync.ui.model.LibraryGroupUiModel
import com.ilfforever.fujisync.ui.model.LibraryRecipeSource
import com.ilfforever.fujisync.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.SampleData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_LIBRARY_GROUP_ID = "group-library"
private const val MAX_REFERENCE_IMAGES = 20
private const val DISCOVER_IMAGE_CONNECT_TIMEOUT_MS = 10_000
private const val DISCOVER_IMAGE_READ_TIMEOUT_MS = 15_000
private const val MAX_DISCOVER_IMAGE_BYTES = 8L * 1024L * 1024L


object LibraryRecipeName {
    const val FALLBACK = "Untitled Recipe"

    fun sanitize(raw: String): String =
        CameraPresetName.sanitizeOrFallback(raw, FALLBACK)

    fun sanitizeForMatching(raw: String): String =
        CameraPresetName.sanitize(raw).lowercase(Locale.US)
}

/**
 * Singleton holder for all library mutations (CRUD, duplicates, groups, favorites, persistence).
 *
 * Uses an [ApplicationScope] [CoroutineScope] that lives as long as the process.
 * This is intentional: a @Singleton outlives any single Activity or ViewModel, so its
 * coroutine scope must also be process-scoped. The [SupervisorJob] ensures one failed
 * child coroutine doesn't cancel siblings. When the Android process is killed, all
 * coroutines are terminated by the runtime — no explicit cancellation is needed.
 */
@Singleton
class LibraryStateHolder @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val localStore: LocalStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    var maxReferenceImages: Int = MAX_REFERENCE_IMAGES

    fun load() {
        scope.launch {
            val library = runCatching {
                withContext(Dispatchers.IO) { localStore.loadLibrary() }
            }.onFailure {
                _state.update { s -> s.copy(loadError = "Your recipe library could not be loaded. The data file may be corrupted.") }
            }.getOrNull()

            val prefs = withContext(Dispatchers.IO) { localStore.loadLibraryViewPrefs() }

            if (library != null) {
                _state.update {
                    it.copy(
                        recipes = library.recipes.map { r -> r.normalizedLibraryRecipe() },
                        groups = library.groups,
                        groupStyles = library.styles,
                        sort = prefs.first,
                        groupedView = prefs.second,
                    )
                }
            }
        }
    }

    // ── Save / duplicate ──────────────────────────────────────────────

    fun addRecipe(recipe: RecipeUiModel, source: LibraryRecipeSource): Boolean {
        return addRecipeWithDuplicateCheck(recipe, source)
    }

    fun addRecipeDraft(recipe: RecipeUiModel): Boolean {
        return addRecipeWithDuplicateCheck(recipe, source = null)
    }

    private fun addRecipeWithDuplicateCheck(recipe: RecipeUiModel, source: LibraryRecipeSource?): Boolean {
        val topMatch = findDuplicate(recipe)
        if (topMatch != null) {
            _state.update { it.copy(duplicateDialog = DuplicateDialogState(recipe, source, topMatch)) }
            return false
        }
        doAddRecipe(recipe, source)
        return true
    }

    fun handleDuplicateSaveAsNew() {
        val dialog = _state.value.duplicateDialog ?: return
        _state.update { it.copy(duplicateDialog = null) }
        doAddRecipe(dialog.incomingRecipe, dialog.source)
    }

    fun handleDuplicateUpdateExisting(libraryId: String) {
        val dialog = _state.value.duplicateDialog ?: return
        val existing = _state.value.recipes.firstOrNull { it.id == libraryId }
        val existingGroupIds = existing?.groupIds.orEmpty()
        val updated = dialog.incomingRecipe
            .toLibraryRecipe(dialog.source)
            .copy(
                id = libraryId,
                groupIds = existingGroupIds,
                sourceCameraName = dialog.source?.cameraName ?: existing?.sourceCameraName,
                sourceCameraModel = dialog.source?.cameraModel ?: existing?.sourceCameraModel,
                sourceUsbId = dialog.source?.usbId ?: existing?.sourceUsbId,
            )
        _state.update {
            it.copy(
                duplicateDialog = null,
                recipes = it.recipes.map { r -> if (r.id == libraryId) updated else r },
            )
        }
        persist()
        confirmSave()
    }

    fun dismissDuplicate() {
        _state.update { it.copy(duplicateDialog = null) }
    }

    fun referenceUrisFor(libraryId: String): List<String> =
        _state.value.recipes.firstOrNull { it.id == libraryId }?.referenceImageUris.orEmpty()

    fun appendReferenceImage(libraryId: String, imageUri: String) {
        _state.update {
            it.copy(
                recipes = it.recipes.map { r ->
                    if (r.id == libraryId) {
                        val merged = (r.referenceImageUris + imageUri).distinct().take(maxReferenceImages)
                        r.copy(referenceImageUris = merged)
                    } else r
                },
            )
        }
        persist()
    }

    private fun doAddRecipe(recipe: RecipeUiModel, source: LibraryRecipeSource?) {
        val saved = recipe.toLibraryRecipe(source)
        _state.update { it.copy(recipes = listOf(saved) + it.recipes) }
        persist()
        confirmSave()
    }

    fun updateRecipe(recipe: RecipeUiModel) {
        _state.update {
            it.copy(
                recipes = it.recipes.map { existing ->
                    if (existing.id == recipe.libraryId) {
                        existing.copy(
                            name = LibraryRecipeName.sanitize(recipe.name),
                            sim = recipe.sim,
                            pills = recipe.pills,
                            description = recipe.description,
                            effects = recipe.effects,
                            tone = recipe.tone,
                            wb = recipe.wb,
                            referenceImageUris = recipe.referenceImageUris,
                            groupIds = recipe.groupIds,
                            favorite = recipe.favorite,
                            isoMin = recipe.isoMin,
                            isoMax = recipe.isoMax,
                            exposureCompMin = recipe.exposureCompMin,
                            exposureCompMax = recipe.exposureCompMax,
                            sensorGens = recipe.sensorGens,
                        )
                    } else {
                        existing
                    }
                },
            )
        }
        persist()
    }

    fun saveNewRecipe(saved: LibraryRecipeUiModel) {
        _state.update { it.copy(recipes = listOf(saved.normalizedLibraryRecipe()) + it.recipes) }
        persist()
        confirmSave()
    }

    fun cloneRecipe(recipe: RecipeUiModel): LibraryRecipeUiModel? {
        val libraryId = recipe.libraryId ?: return null
        val existing = _state.value.recipes.firstOrNull { it.id == libraryId } ?: return null
        val clone = existing.copy(
            id = "lib-${UUID.randomUUID()}",
            name = existing.name,
        )
        _state.update { it.copy(recipes = listOf(clone) + it.recipes) }
        persist()
        return clone
    }

    fun deleteRecipes(recipeIds: Set<String>) {
        if (recipeIds.isEmpty()) return
        _state.update { it.copy(recipes = it.recipes.filterNot { r -> r.id in recipeIds }) }
        persist()
    }

    // ── Group management ──────────────────────────────────────────────

    fun changeRecipeGroup(recipeId: String, groupId: String) {
        val nextGroupId = groupId.ifBlank { null }
        _state.update {
            it.copy(
                recipes = it.recipes.map { r ->
                    if (r.id == recipeId)
                        r.copy(groupIds = nextGroupId?.let { id -> r.groupIds.toggle(id) } ?: emptyList())
                    else r
                },
            )
        }
        persist()
    }

    fun changeRecipesGroup(recipeIds: Set<String>, groupId: String) {
        if (recipeIds.isEmpty()) return
        val nextGroupId = groupId.ifBlank { null }
        _state.update {
            it.copy(
                recipes = it.recipes.map { r ->
                    if (r.id in recipeIds)
                        r.copy(groupIds = nextGroupId?.let { id -> (r.groupIds + id).distinct() } ?: emptyList())
                    else r
                },
            )
        }
        persist()
    }

    fun createGroupForRecipe(recipeId: String, name: String) {
        val groupName = name.trim().ifBlank { return }
        val groupId = "group-${UUID.randomUUID()}"
        _state.update { it.copy(groups = it.groups + LibraryGroupUiModel(id = groupId, name = groupName)) }
        changeRecipeGroup(recipeId, groupId)
    }

    fun createGroup(name: String) {
        val groupName = name.trim().ifBlank { return }
        val groupId = "group-${UUID.randomUUID()}"
        _state.update { it.copy(groups = it.groups + LibraryGroupUiModel(id = groupId, name = groupName)) }
        persist()
    }

    fun createGroupForRecipes(recipeIds: Set<String>, name: String) {
        if (recipeIds.isEmpty()) return
        val groupName = name.trim().ifBlank { return }
        val groupId = "group-${UUID.randomUUID()}"
        _state.update { it.copy(groups = it.groups + LibraryGroupUiModel(id = groupId, name = groupName)) }
        changeRecipesGroup(recipeIds, groupId)
    }

    fun changeGroupStyle(groupId: String, style: LibraryGroupStyle) {
        _state.update { it.copy(groupStyles = it.groupStyles + (groupId to style)) }
        persist()
    }

    fun renameGroup(groupId: String, newName: String) {
        val nextName = newName.trim().ifBlank { return }
        _state.update {
            it.copy(groups = it.groups.map { g -> if (g.id == groupId) g.copy(name = nextName) else g })
        }
        persist()
    }

    fun deleteGroup(groupId: String) {
        if (groupId == DEFAULT_LIBRARY_GROUP_ID) return
        _state.update {
            it.copy(
                recipes = it.recipes.map { r -> if (groupId in r.groupIds) r.copy(groupIds = r.groupIds - groupId) else r },
                groups = it.groups.filterNot { g -> g.id == groupId },
                groupStyles = it.groupStyles - groupId,
            )
        }
        persist()
    }

    fun setGroupImage(groupId: String, uri: Uri) {
        val current = _state.value.groupStyles[groupId] ?: LibraryGroupStyle()
        _state.update { it.copy(groupStyles = it.groupStyles + (groupId to current.copy(imageUri = uri.toString()))) }
        persist()
    }

    // ── Reference images ──────────────────────────────────────────────

    fun applyReferenceImages(libraryId: String, localUris: List<String>) {
        _state.update {
            it.copy(
                recipes = it.recipes.map { r ->
                    if (r.id == libraryId)
                        r.copy(referenceImageUris = r.referenceImageUris.appendReferenceImages(localUris))
                    else r
                },
            )
        }
        persist()
    }

    fun reorderReferenceImages(libraryId: String, fromIndex: Int, toIndex: Int) {
        _state.update {
            it.copy(
                recipes = it.recipes.map { r ->
                    if (r.id == libraryId) {
                        val uris = r.referenceImageUris.toMutableList()
                        if (fromIndex in uris.indices && toIndex in uris.indices) {
                            uris.add(toIndex, uris.removeAt(fromIndex))
                        }
                        r.copy(referenceImageUris = uris)
                    } else r
                },
            )
        }
        persist()
    }

    fun removeReferenceImage(libraryId: String, uriString: String) {
        _state.update {
            it.copy(
                recipes = it.recipes.map { r ->
                    if (r.id == libraryId) r.copy(referenceImageUris = r.referenceImageUris - uriString) else r
                },
            )
        }
        scope.launch(Dispatchers.IO) { localStore.deleteReferenceImageFile(uriString) }
        persist()
    }

    // ── Favorites ─────────────────────────────────────────────────────

    fun toggleFavorite(recipeId: String) {
        _state.update {
            it.copy(recipes = it.recipes.map { r -> if (r.id == recipeId) r.copy(favorite = !r.favorite) else r })
        }
        persist()
    }

    fun renameCameraSource(oldName: String, newName: String) {
        if (oldName == newName) return
        val affected = _state.value.recipes.count { it.sourceCameraName == oldName }
        if (affected == 0) return
        _state.update {
            it.copy(recipes = it.recipes.map { r ->
                if (r.sourceCameraName == oldName) r.copy(sourceCameraName = newName) else r
            })
        }
        persist()
    }

    // ── Sort + view mode ──────────────────────────────────────────────

    fun setSort(sort: String) {
        _state.update { it.copy(sort = sort) }
        persistViewPrefs(sort = sort, groupedView = _state.value.groupedView)
    }

    fun setGroupedView(on: Boolean) {
        _state.update { it.copy(groupedView = on) }
        persistViewPrefs(sort = _state.value.sort, groupedView = on)
    }

    private fun persistViewPrefs(sort: String, groupedView: Boolean) {
        scope.launch(Dispatchers.IO) {
            localStore.saveLibraryViewPrefs(sort, groupedView)
        }
    }

    // ── Sample / Discover ─────────────────────────────────────────────

    fun loadSampleLibrary() {
        val existingIds = _state.value.recipes.map { it.id }.toSet()
        val existingGroupIds = _state.value.groups.map { it.id }.toSet()
        _state.update {
            it.copy(
                recipes = SampleData.library
                    .filter { r -> r.id !in existingIds }
                    .map { it.normalizedLibraryRecipe() } + it.recipes,
                groups = it.groups + SampleData.libraryGroups.filter { g -> g.id !in existingGroupIds },
            )
        }
        persist()
    }

    fun exportData(): LocalStore.LibraryData {
        val snapshot = _state.value
        return LocalStore.LibraryData(
            recipes = snapshot.recipes,
            groups = snapshot.groups,
            styles = snapshot.groupStyles,
        )
    }

    fun replaceAll(data: LocalStore.LibraryData) {
        _state.update {
            it.copy(
                recipes = data.recipes.map { recipe -> recipe.normalizedLibraryRecipe() },
                groups = data.groups,
                groupStyles = data.styles,
                duplicateDialog = null,
            )
        }
        persist()
    }

    fun mergeData(data: LocalStore.LibraryData): BackupMergeReport {
        var report = BackupMergeReport()
        _state.update { current ->
            val groupIdMap = mutableMapOf<String, String>()
            val groups = current.groups.toMutableList()
            val styles = current.groupStyles.toMutableMap()

            data.groups.forEach { incoming ->
                val existing = groups.firstOrNull { it.id == incoming.id }
                val targetId = when {
                    existing == null -> incoming.id
                    existing.name == incoming.name -> existing.id
                    else -> "group-${UUID.randomUUID()}"
                }
                groupIdMap[incoming.id] = targetId
                if (groups.none { it.id == targetId }) {
                    groups.add(incoming.copy(id = targetId))
                    report = report.copy(groupsImported = report.groupsImported + 1)
                }
                data.styles[incoming.id]?.let { style -> styles[targetId] = style }
            }

            val recipes = current.recipes.toMutableList()
            data.recipes.map { incoming ->
                incoming
                    .copy(groupIds = incoming.groupIds.map { groupId -> groupIdMap[groupId] ?: groupId })
                    .normalizedLibraryRecipe()
            }.forEach { incoming ->
                val duplicate = recipes.any { existing ->
                    existing.id == incoming.id ||
                        (existing.sim == incoming.sim &&
                            existing.effects == incoming.effects &&
                            existing.tone == incoming.tone &&
                            existing.wb == incoming.wb)
                }
                if (duplicate) {
                    report = report.copy(recipesSkipped = report.recipesSkipped + 1)
                } else {
                    val id = if (recipes.any { it.id == incoming.id }) "lib-${UUID.randomUUID()}" else incoming.id
                    recipes.add(0, incoming.copy(id = id))
                    report = report.copy(recipesImported = report.recipesImported + 1)
                }
            }

            current.copy(
                recipes = recipes,
                groups = groups,
                groupStyles = styles,
                duplicateDialog = null,
            )
        }
        persist()
        return report
    }

    suspend fun saveFromDiscover(recipe: FxwRecipe, name: String, includePhotos: Boolean = true) {
        val referenceUris = if (includePhotos) downloadDiscoverImages(recipe) else emptyList()
        val saved = recipe.toLibraryRecipeUiModel(name, referenceUris)
        _state.update { it.copy(recipes = listOf(saved) + it.recipes) }
        persist()
        confirmSave()
    }

    private suspend fun downloadDiscoverImages(recipe: FxwRecipe): List<String> =
        withContext(Dispatchers.IO) {
            val recipeDir = File(appContext.filesDir, "references/${recipe.slug}")
                .also { if (!it.exists()) it.mkdirs() }
            coroutineScope {
                recipe.imageUrls
                    .take(maxReferenceImages)
                    .mapIndexed { idx, url ->
                        async {
                            downloadDiscoverImage(url, File(recipeDir, "image_$idx.jpg"))
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
            }
        }

    private fun downloadDiscoverImage(url: String, file: File): String? =
        runCatching {
            val parsed = URL(url)
            if (parsed.protocol != "https") return@runCatching null

            val connection = parsed.openConnection() as HttpURLConnection
            connection.connectTimeout = DISCOVER_IMAGE_CONNECT_TIMEOUT_MS
            connection.readTimeout = DISCOVER_IMAGE_READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "FujiRecipes/1.0 Android")

            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                val contentLength = connection.contentLengthLong
                if (contentLength > MAX_DISCOVER_IMAGE_BYTES) return@runCatching null

                val tmp = File(file.parentFile, "${file.name}.tmp")
                var total = 0L
                connection.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_DISCOVER_IMAGE_BYTES) {
                                tmp.delete()
                                return@runCatching null
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                if (!tmp.renameTo(file)) {
                    tmp.copyTo(file, overwrite = true)
                    tmp.delete()
                }
                Uri.fromFile(file).toString()
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

    // ── Persistence ───────────────────────────────────────────────────

    private fun persist() {
        val snapshot = _state.value
        scope.launch(Dispatchers.IO) {
            localStore.saveLibrary(snapshot.recipes, snapshot.groups, snapshot.groupStyles)
        }
    }

    private fun confirmSave() {
        _state.update { it.copy(saveConfirmed = true) }
        scope.launch {
            delay(UiTimings.SAVE_CONFIRMATION_MS)
            _state.update { it.copy(saveConfirmed = false) }
        }
    }

    // ── Duplicate detection ───────────────────────────────────────────

    fun findDuplicate(recipe: RecipeUiModel, similarityThreshold: Float = 0.65f): DuplicateMatch? =
        findDuplicateIn(recipe, _state.value.recipes, similarityThreshold)

    private fun findDuplicateIn(
        recipe: RecipeUiModel,
        against: List<LibraryRecipeUiModel>,
        similarityThreshold: Float = 0.65f,
    ): DuplicateMatch? =
        against
            .mapNotNull { existing ->
                when {
                    recipe.isDuplicateSettings(existing) -> DuplicateMatch(existing, DuplicateMatchKind.ExactSettings)
                    recipe.isSameName(existing) -> DuplicateMatch(existing, DuplicateMatchKind.SameName)
                    recipe.isSimilarName(existing) || recipe.isSimilarSettings(existing, similarityThreshold) -> DuplicateMatch(existing, DuplicateMatchKind.Similar)
                    else -> null
                }
            }
            .minByOrNull { it.kind.ordinal }

    fun addRecipesBatch(recipes: List<RecipeUiModel>, source: LibraryRecipeSource): SaveAllReport {
        val accumulator = mutableListOf<LibraryRecipeUiModel>()
        val skippedList = mutableListOf<SaveAllSkipped>()
        val initial = _state.value.recipes
        for (recipe in recipes) {
            val match = findDuplicateIn(recipe, initial + accumulator)
            if (match != null) {
                skippedList.add(SaveAllSkipped(
                    slot = recipe.slot,
                    name = LibraryRecipeName.sanitize(recipe.name),
                    sim = recipe.sim,
                    matchKind = match.kind,
                    matchedName = match.libraryRecipe.name,
                ))
            } else {
                accumulator.add(recipe.toLibraryRecipe(source))
            }
        }
        if (accumulator.isNotEmpty()) {
            _state.update { it.copy(recipes = accumulator + it.recipes) }
            persist()
            confirmSave()
        }
        return SaveAllReport(saved = accumulator.size, skipped = skippedList)
    }

    private fun RecipeUiModel.isDuplicateSettings(existing: LibraryRecipeUiModel): Boolean =
        sim == existing.sim && effects == existing.effects && tone == existing.tone && wb == existing.wb

    private fun RecipeUiModel.isSameName(existing: LibraryRecipeUiModel): Boolean {
        val a = LibraryRecipeName.sanitizeForMatching(name)
        val b = LibraryRecipeName.sanitizeForMatching(existing.name)
        return a.isNotEmpty() && b.isNotEmpty() && a == b
    }

    private fun RecipeUiModel.isSimilarName(existing: LibraryRecipeUiModel): Boolean {
        val a = LibraryRecipeName.sanitizeForMatching(name)
        val b = LibraryRecipeName.sanitizeForMatching(existing.name)
        if (a.isEmpty() || b.isEmpty()) return false
        return a.contains(b) || b.contains(a) || wordOverlapRatio(a, b) >= 0.6f
    }

    private fun RecipeUiModel.isSimilarSettings(existing: LibraryRecipeUiModel, threshold: Float = 0.65f): Boolean {
        if (sim != existing.sim) return false
        val myAll = effects + tone + wb
        val theirAll = existing.effects + existing.tone + existing.wb
        val uniqueKeys = (myAll.keys + theirAll.keys).toSet()
        if (uniqueKeys.isEmpty()) return false
        val matchCount = uniqueKeys.count { key -> myAll[key] != null && myAll[key] == theirAll[key] }
        return matchCount.toFloat() / uniqueKeys.size >= threshold
    }

    private fun wordOverlapRatio(a: String, b: String): Float {
        val wordsA = a.split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
        val wordsB = b.split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
        val union = (wordsA + wordsB).size
        if (union == 0) return 1f
        return wordsA.intersect(wordsB).size.toFloat() / union
    }

    // ── Mapping helpers ───────────────────────────────────────────────

    fun RecipeUiModel.toLibraryRecipe(source: LibraryRecipeSource?): LibraryRecipeUiModel =
        LibraryRecipeUiModel(
            id = "lib-${UUID.randomUUID()}",
            name = LibraryRecipeName.sanitize(name),
            sim = sim,
            pills = pills,
            saved = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd", Locale.US)),
            description = description,
            effects = effects,
            tone = tone,
            wb = wb,
            sourceCameraName = source?.cameraName,
            sourceCameraModel = source?.cameraModel,
            sourceUsbId = source?.usbId,
            referenceImageUris = referenceImageUris,
            groupIds = groupIds,
            favorite = favorite,
            isoMin = isoMin,
            isoMax = isoMax,
            exposureCompMin = exposureCompMin,
            exposureCompMax = exposureCompMax,
            sensorGens = sensorGens,
        )

    private fun FxwRecipe.toLibraryRecipeUiModel(name: String, referenceUris: List<String> = emptyList()): LibraryRecipeUiModel {
        val normalized = params.normalizedFxwParams()
        val effectKeys = setOf("Dynamic Range", "D Range Priority", "Grain Effect", "Color Chrome", "Color Chrome FX Blue", "Smooth Skin")
        val toneKeys = setOf("Highlight Tone", "Shadow Tone", "Color", "Sharpness", "High ISO NR", "Clarity")
        val wbKeys = setOf("White Balance", "WB Shift R", "WB Shift B")
        val (isoMin, isoMax) = parseIsoRange(params["ISO"] ?: "")
        val (ecMin, ecMax) = parseExposureCompRange(params["Exposure Compensation"] ?: "")
        return LibraryRecipeUiModel(
            id = "lib-${UUID.randomUUID()}",
            name = LibraryRecipeName.sanitize(name),
            sim = filmSim.canonicalFilmSimLabel(),
            pills = pillLabels(),
            saved = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd", Locale.US)),
            effects = normalized.filterKeys { it in effectKeys },
            tone = normalized.filterKeys { it in toneKeys },
            wb = normalized.filterKeys { it in wbKeys },
            referenceImageUris = referenceUris,
            sourceUrl = postUrl,
            sourceLabel = "Fuji X Weekly",
            isoMin = isoMin,
            isoMax = isoMax,
            exposureCompMin = ecMin,
            exposureCompMax = ecMax,
        )
    }

    fun LibraryRecipeUiModel.normalizedLibraryRecipe(): LibraryRecipeUiModel =
        copy(
            name = LibraryRecipeName.sanitize(name),
            effects = effects.normalizedEffectsSection(),
            wb = wb.normalizedWhiteBalanceSection(),
        )

internal fun Map<String, String>.normalizedEffectsSection(): Map<String, String> =
    mapValues { (key, value) ->
        when (key) {
            "Dynamic Range" -> value.normalizedDynamicRangeLabel()
            "D Range Priority" -> value.normalizedDRangePriorityLabel()
            else -> value
        }
    }

    private fun List<String>.appendReferenceImages(uris: List<String>): List<String> =
        (this + uris).distinct().take(maxReferenceImages)

    private fun List<String>.toggle(groupId: String): List<String> =
        if (groupId in this) this - groupId else this + groupId
}

data class BackupMergeReport(
    val recipesImported: Int = 0,
    val recipesSkipped: Int = 0,
    val groupsImported: Int = 0,
)

// ── WB normalization (shared utility) ────────────────────────────────

internal fun Map<String, String>.normalizedFxwParams(): Map<String, String> {
    val normalized = linkedMapOf<String, String>()
    fun putClean(key: String, value: String) {
        val cleanValue = value.trim()
        if (cleanValue.isNotBlank()) normalized[key] = cleanValue
    }
    forEach { (rawKey, rawValue) ->
        val key = rawKey.trim()
        val value = rawValue.trim()
        when (key) {
            "Color Chrome Effect" -> putClean("Color Chrome", value)
            "Dynamic Range" -> putClean("Dynamic Range", value.normalizedDynamicRangeLabel())
            "Highlight" -> putClean("Highlight Tone", value)
            "Shadow" -> putClean("Shadow Tone", value)
            "Noise Reduction" -> putClean("High ISO NR", value)
            "Dynamic Range Priority", "DR Priority", "DRP" -> putClean("D Range Priority", value.normalizedDRangePriorityLabel())
            "D Range Priority" -> putClean("D Range Priority", value.normalizedDRangePriorityLabel())
            "White Balance" -> {
                val parsed = parseWhiteBalanceValue(value)
                putClean("White Balance", parsed.balance.normalizedWhiteBalanceLabel())
                parsed.shiftR?.let { putClean("WB Shift R", it) }
                parsed.shiftB?.let { putClean("WB Shift B", it) }
            }
            else -> putClean(key, value)
        }
    }
    return normalized
}

internal fun Map<String, String>.normalizedWhiteBalanceSection(): Map<String, String> {
    val whiteBalance = this["White Balance"] ?: return this
    val parsed = parseWhiteBalanceValue(whiteBalance)
    val normalizedBalance = parsed.balance.normalizedWhiteBalanceLabel()
    if (parsed.shiftR == null && parsed.shiftB == null && normalizedBalance == whiteBalance) return this
    return buildMap {
        put("White Balance", normalizedBalance)
        parsed.shiftR?.let { put("WB Shift R", it) }
        parsed.shiftB?.let { put("WB Shift B", it) }
        this@normalizedWhiteBalanceSection.forEach { (key, value) ->
            if (key != "White Balance" && key != "WB Shift R" && key != "WB Shift B") put(key, value)
        }
    }
}

internal data class ParsedWhiteBalance(val balance: String, val shiftR: String? = null, val shiftB: String? = null)

internal fun parseWhiteBalanceValue(value: String): ParsedWhiteBalance {
    val shiftMatch = Regex("""([+\-−]?\d+)\s*Red.*?([+\-−]?\d+)\s*Blue""", RegexOption.IGNORE_CASE)
        .find(value) ?: return ParsedWhiteBalance(balance = value)
    val balance = value
        .substringBefore(shiftMatch.value).trim().trimEnd(',', '&')
        .ifBlank { value.substringBefore(",").trim() }
    return ParsedWhiteBalance(balance = balance, shiftR = shiftMatch.groupValues[1], shiftB = shiftMatch.groupValues[2])
}

internal fun String.normalizedWhiteBalanceLabel(): String = when (trim()) {
    "Auto (White Priority)" -> "Auto White Priority"
    "Auto (Ambience)" -> "Ambience Priority"
    "Tungsten" -> "Incandescent"
    else -> trim()
}

internal fun String.normalizedDRangePriorityLabel(): String = when (trim().lowercase()) {
    "off" -> "Off"
    "strong" -> "Strong"
    "weak" -> "Weak"
    "auto" -> "Auto"
    else -> trim()
}

internal fun String.normalizedDynamicRangeLabel(): String {
    val compact = trim()
        .replace(" ", "")
        .uppercase()
        .replace('O', '0')
    return when (compact) {
        "DR100", "100", "100%" -> "DR100%"
        "DR200", "200", "200%" -> "DR200%"
        "DR400", "400", "400%" -> "DR400%"
        "DRAUTO", "AUTO", "DR0" -> "DR Auto"
        else -> trim()
    }
}

// Parses ISO strings like "Auto, up to ISO 6400", "ISO 200-6400", "640", "200 to 3200"
internal fun parseIsoRange(raw: String): Pair<Int?, Int?> {
    if (raw.isBlank()) return null to null
    val numbers = Regex("\\d+").findAll(raw).map { it.value.toInt() }.filter { it in 50..51200 }.toList()
    return when {
        numbers.size >= 2 -> numbers.first() to numbers.last()
        numbers.size == 1 -> {
            val lower = raw.lowercase()
            if (lower.contains("up to") || lower.contains("max")) null to numbers.first()
            else numbers.first() to null
        }
        else -> null to null
    }
}

// Parses exposure comp strings like "+1/3 to +1 (typically)", "-1 to +2", "+2/3"
internal fun parseExposureCompRange(raw: String): Pair<Float?, Float?> {
    if (raw.isBlank()) return null to null
    val pattern = Regex("[+\\-−]?\\d+(?:/\\d+)?(?:\\.\\d+)?")
    val values = pattern.findAll(raw).mapNotNull { match ->
        val s = match.value.replace('−', '-')
        if ('/' in s) {
            val parts = s.split('/')
            val num = parts[0].toFloatOrNull() ?: return@mapNotNull null
            val den = parts[1].toFloatOrNull()?.takeIf { it != 0f } ?: return@mapNotNull null
            kotlin.math.round(num / den * 10f) / 10f
        } else {
            s.toFloatOrNull()
        }
    }.filter { it in -5f..5f }.toList()
    return when {
        values.size >= 2 -> values.first() to values.last()
        values.size == 1 -> values.first() to values.first()
        else -> null to null
    }
}
