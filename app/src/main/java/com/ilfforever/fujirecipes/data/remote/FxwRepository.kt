package com.ilfforever.fujirecipes.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FxwRepository {
    private const val CACHE_DIR = "discover_fxw"
    private const val CACHE_VERSION = 3
    private const val FRESH_MS = 6L * 60L * 60L * 1000L
    private const val MAX_MEMORY_PAGES = 5

    private val memoryPages = java.util.concurrent.ConcurrentHashMap<Int, CachedPage>()

    data class PageResult(
        val recipes: List<FxwRecipe>,
        val hasMore: Boolean,
        val source: Source,
    )

    enum class Source { Network, Memory, Disk, StaleDisk, NotModified }

    suspend fun loadPage(context: Context, page: Int, forceRefresh: Boolean = false): PageResult =
        withContext(Dispatchers.IO) {
            val cacheFile = pageCacheFile(context, page)
            val diskPage = readPage(cacheFile)
            val memoryPage = memoryPages[page]

            if (!forceRefresh) {
                memoryPage?.takeIf { it.isFresh() }?.let {
                    return@withContext it.toResult(Source.Memory)
                }
                diskPage?.takeIf { it.isFresh() }?.let {
                    cachePage(page, it)
                    return@withContext it.toResult(Source.Disk)
                }
            }

            val validator = memoryPage ?: diskPage
            runCatching {
                FxwApi.fetchRecipes(
                    page = page,
                    etag = validator?.etag,
                    lastModified = validator?.lastModified,
                )
            }.fold(
                onSuccess = { fetched ->
                    if (fetched.notModified && validator != null) {
                        val refreshed = validator.copy(
                            cachedAt = System.currentTimeMillis(),
                            etag = fetched.etag ?: validator.etag,
                            lastModified = fetched.lastModified ?: validator.lastModified,
                        )
                        cachePage(page, refreshed)
                        writePage(cacheFile, refreshed)
                        refreshed.toResult(Source.NotModified)
                    } else {
                        val next = CachedPage(
                            recipes = fetched.recipes,
                            hasMore = fetched.hasMore,
                            etag = fetched.etag,
                            lastModified = fetched.lastModified,
                            cachedAt = System.currentTimeMillis(),
                        )
                        cachePage(page, next)
                        writePage(cacheFile, next)
                        next.toResult(Source.Network)
                    }
                },
                onFailure = { error ->
                    if (validator != null) {
                        validator.toResult(Source.StaleDisk)
                    } else {
                        throw error
                    }
                },
            )
        }

    private data class CachedPage(
        val recipes: List<FxwRecipe>,
        val hasMore: Boolean,
        val etag: String?,
        val lastModified: String?,
        val cachedAt: Long,
    ) {
        fun isFresh(now: Long = System.currentTimeMillis()): Boolean = now - cachedAt <= FRESH_MS

        fun toResult(source: Source): PageResult = PageResult(
            recipes = recipes,
            hasMore = hasMore,
            source = source,
        )
    }

    private fun pageCacheFile(context: Context, page: Int): File {
        val dir = File(context.cacheDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "page-$page.json")
    }

    private fun cachePage(page: Int, cachedPage: CachedPage) {
        memoryPages[page] = cachedPage
        if (memoryPages.size <= MAX_MEMORY_PAGES) return

        val pagesToRemove = memoryPages.entries
            .sortedBy { it.value.cachedAt }
            .take(memoryPages.size - MAX_MEMORY_PAGES)
            .map { it.key }

        pagesToRemove.forEach { memoryPages.remove(it) }
    }

    private fun readPage(file: File): CachedPage? =
        runCatching {
            if (!file.exists()) return null
            val root = JSONObject(file.readText())
            if (root.optInt("version") != CACHE_VERSION) return null
            CachedPage(
                recipes = root.getJSONArray("recipes").toRecipeList(),
                hasMore = root.optBoolean("hasMore"),
                etag = root.optString("etag").takeIf { it.isNotBlank() },
                lastModified = root.optString("lastModified").takeIf { it.isNotBlank() },
                cachedAt = root.optLong("cachedAt"),
            )
        }.getOrNull()

    private fun writePage(file: File, page: CachedPage) {
        runCatching {
            val root = JSONObject()
                .put("version", CACHE_VERSION)
                .put("cachedAt", page.cachedAt)
                .put("hasMore", page.hasMore)
                .put("etag", page.etag.orEmpty())
                .put("lastModified", page.lastModified.orEmpty())
                .put("recipes", page.recipes.toJsonArray())
            file.writeText(root.toString())
        }.onFailure { error ->
            Log.w("FxwRepository", "Failed to write FXW cache ${file.name}: ${error.message}")
        }
    }

    private fun List<FxwRecipe>.toJsonArray(): JSONArray {
        val arr = JSONArray()
        forEach { recipe ->
            val variantsArr = JSONArray()
            recipe.variants.forEach { v ->
                variantsArr.put(JSONObject().put("label", v.label).put("params", JSONObject(v.params)))
            }
            arr.put(
                JSONObject()
                    .put("id", recipe.id)
                    .put("slug", recipe.slug)
                    .put("title", recipe.title)
                    .put("postUrl", recipe.postUrl)
                    .put("imageUrls", JSONArray(recipe.imageUrls))
                    .put("date", recipe.date)
                    .put("params", JSONObject(recipe.params))
                    .put("articleText", recipe.articleText)
                    .put("variants", variantsArr)
                    .put("xTransGen", recipe.xTransGen.orEmpty()),
            )
        }
        return arr
    }

    private fun JSONArray.toRecipeList(): List<FxwRecipe> =
        (0 until length()).map { idx ->
            val obj = getJSONObject(idx)
            val variants = obj.optJSONArray("variants")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val v = arr.getJSONObject(i)
                    FxwRecipeVariant(label = v.optString("label"), params = v.getJSONObject("params").toStringMap())
                }
            }.orEmpty()
            FxwRecipe(
                id = obj.getInt("id"),
                slug = obj.getString("slug"),
                title = obj.getString("title"),
                postUrl = obj.getString("postUrl"),
                imageUrls = obj.optJSONArray("imageUrls")?.toStringList().orEmpty(),
                date = obj.getString("date"),
                params = obj.getJSONObject("params").toStringMap(),
                articleText = obj.optString("articleText"),
                variants = variants,
                xTransGen = obj.optString("xTransGen").takeIf { it.isNotBlank() },
            )
        }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).mapNotNull { idx -> optString(idx).takeIf { it.isNotBlank() } }

    private fun JSONObject.toStringMap(): Map<String, String> =
        keys().asSequence().associateWith { key -> optString(key) }
}
