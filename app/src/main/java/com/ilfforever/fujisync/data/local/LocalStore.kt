package com.ilfforever.fujisync.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.ilfforever.fujisync.ui.model.AppSettings
import com.ilfforever.fujisync.ui.model.LibraryGroupStyle
import com.ilfforever.fujisync.ui.model.LibraryGroupUiModel
import com.ilfforever.fujisync.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.SlotBackupMeta
import com.ilfforever.fujisync.ui.model.SlotBackupSet
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val REFERENCE_MAX_DIM = 1920
private const val REFERENCE_JPEG_QUALITY = 85

class LocalStore(context: Context) {
    private val dir = context.filesDir
    private val contentResolver = context.contentResolver
    private val mutex = Mutex()

    // ── Library ───────────────────────────────────────────────────────

    suspend fun saveLibrary(
        recipes: List<LibraryRecipeUiModel>,
        groups: List<LibraryGroupUiModel>,
        styles: Map<String, LibraryGroupStyle>,
    ) = mutex.withLock {
        write("library_recipes.json", recipesToJson(recipes).toString())
        write("library_groups.json", groupsToJson(groups).toString())
        write("library_styles.json", stylesToJson(styles).toString())
    }

    suspend fun loadLibrary(): LibraryData? = mutex.withLock {
        val recipesFile = File(dir, "library_recipes.json")
        if (!recipesFile.exists()) return@withLock null
        runCatching {
            LibraryData(
                recipes = recipesFromJson(JSONArray(recipesFile.readText())),
                groups = File(dir, "library_groups.json").let { f ->
                    if (f.exists()) groupsFromJson(JSONArray(f.readText())) else emptyList()
                },
                styles = File(dir, "library_styles.json").let { f ->
                    if (f.exists()) stylesFromJson(JSONObject(f.readText())) else emptyMap()
                },
            )
        }.onFailure { Log.e("LocalStore", "Library parse failed — file may be corrupt", it) }
         .getOrThrow()
    }

    // ── Slot backup ───────────────────────────────────────────────────

    suspend fun saveSlotBackup(slots: List<RecipeUiModel>) = mutex.withLock {
        write("slot_backup.json", slotsToJson(slots).toString())
    }

    suspend fun saveSlotBackupSet(meta: SlotBackupMeta, slots: List<RecipeUiModel>) = mutex.withLock {
        val id = meta.id.ifBlank { "slot-backup-${UUID.randomUUID()}" }
        val normalizedMeta = meta.copy(id = id)
        val sets = loadSlotBackupSetsLocked().filterNot { it.meta.id == id } +
            SlotBackupSet(normalizedMeta, slots)
        write("slot_backups.json", slotBackupSetsToJson(sets).toString())
    }

    suspend fun loadSlotBackup(): List<RecipeUiModel>? = mutex.withLock {
        val file = File(dir, "slot_backup.json")
        if (!file.exists()) return@withLock null
        runCatching { slotsFromJson(JSONArray(file.readText())) }.getOrNull()
    }

    suspend fun hasSlotBackup(): Boolean = mutex.withLock {
        loadSlotBackupSetsLocked().isNotEmpty()
    }

    suspend fun saveSlotBackupMeta(label: String, savedAt: String) = mutex.withLock {
        write("slot_backup_meta.json", org.json.JSONObject().apply {
            put("label", label)
            put("savedAt", savedAt)
        }.toString())
    }

    suspend fun deleteSlotBackup() = mutex.withLock {
        File(dir, "slot_backup.json").delete()
        File(dir, "slot_backup_meta.json").delete()
        File(dir, "slot_backups.json").delete()
    }

    suspend fun deleteSlotBackup(id: String) = mutex.withLock {
        val remaining = loadSlotBackupSetsLocked().filterNot { it.meta.id == id }
        write("slot_backups.json", slotBackupSetsToJson(remaining).toString())
        if (remaining.isEmpty()) {
            File(dir, "slot_backups.json").delete()
        }
    }

    suspend fun renameSlotBackup(id: String, label: String) = mutex.withLock {
        val updated = loadSlotBackupSetsLocked().map { set ->
            if (set.meta.id == id) set.copy(meta = set.meta.copy(label = label)) else set
        }
        write("slot_backups.json", slotBackupSetsToJson(updated).toString())
    }

    suspend fun loadSlotBackupMeta(): Pair<String, String>? = mutex.withLock {
        val file = File(dir, "slot_backup_meta.json")
        if (!file.exists()) return@withLock null
        runCatching {
            val o = org.json.JSONObject(file.readText())
            Pair(o.getString("label"), o.getString("savedAt"))
        }.getOrNull()
    }

    // ── Reference images ──────────────────────────────────────────────

    suspend fun copyReferenceImage(sourceUri: Uri): String? = runCatching {
        val refDir = File(dir, "ref_images").also { it.mkdirs() }
        val dest = File(refDir, "${UUID.randomUUID()}.jpg")

        // Determine native dimensions without allocating pixels.
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
        val srcMax = maxOf(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)

        // Pick the largest power-of-2 inSampleSize that still keeps the long edge >= target.
        var sampleSize = 1
        while (srcMax / (sampleSize * 2) >= REFERENCE_MAX_DIM) sampleSize *= 2

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return@runCatching null

        // Fine-scale if still above target after the power-of-2 downsample.
        val longEdge = maxOf(decoded.width, decoded.height)
        val bitmap = if (longEdge > REFERENCE_MAX_DIM) {
            val scale = REFERENCE_MAX_DIM.toFloat() / longEdge
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt(),
                (decoded.height * scale).toInt(),
                true,
            ).also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }

        dest.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, REFERENCE_JPEG_QUALITY, it) }
        bitmap.recycle()

        Uri.fromFile(dest).toString()
    }.getOrNull()

    suspend fun deleteReferenceImageFile(uriString: String) {
        runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                uri.path?.let { File(it).delete() }
            }
        }
    }

    suspend fun isExistingReferenceImage(uri: android.net.Uri, existingUris: List<String>): Boolean {
        if (existingUris.isEmpty()) return false
        val newHash = hashFileUri(uri.toString()) ?: return false
        return existingUris.any { existing -> hashFileUri(existing)?.contentEquals(newHash) == true }
    }

    suspend fun deduplicateReferenceImage(newUri: String, existingUris: List<String>): String {
        if (existingUris.isEmpty()) return newUri
        val newHash = hashFileUri(newUri) ?: return newUri
        for (existing in existingUris) {
            val existingHash = hashFileUri(existing) ?: continue
            if (newHash.contentEquals(existingHash)) {
                deleteReferenceImageFile(newUri)
                return existing
            }
        }
        return newUri
    }

    private fun hashFileUri(uriString: String): ByteArray? = runCatching {
        val uri = Uri.parse(uriString)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val input = if (uri.scheme == "file") {
            uri.path?.let { java.io.FileInputStream(java.io.File(it)) }
        } else {
            contentResolver.openInputStream(uri)
        } ?: return@runCatching null
        input.use { stream ->
            val buf = ByteArray(8192)
            var n: Int
            while (stream.read(buf).also { n = it } != -1) digest.update(buf, 0, n)
        }
        digest.digest()
    }.getOrNull()

    // ── App settings ──────────────────────────────────────────────────

    suspend fun saveSettings(settings: AppSettings) = mutex.withLock {
        write("settings.json", JSONObject().apply {
            put("showLibraryImages", settings.showLibraryImages)
            put("showCardImageCount", settings.showCardImageCount)
            put("showReferenceImageBlur", settings.showReferenceImageBlur)
            put("hapticsEnabled", settings.hapticsEnabled)
            put("favoritesOnTop", settings.favoritesOnTop)
            put("propertyWriteDelayMs", settings.propertyWriteDelayMs)
            put("smartRefSimilarityPct", settings.smartRefSimilarityPct)
            put("maxReferenceImages", settings.maxReferenceImages)
        }.toString())
    }

    suspend fun loadSettings(): AppSettings = mutex.withLock {
        val file = File(dir, "settings.json")
        if (!file.exists()) return@withLock AppSettings()
        runCatching {
            val o = JSONObject(file.readText())
            AppSettings(
                showLibraryImages = o.optBoolean("showLibraryImages", true),
                showCardImageCount = o.optBoolean("showCardImageCount", false),
                showReferenceImageBlur = o.optBoolean("showReferenceImageBlur", true),
                hapticsEnabled = o.optBoolean("hapticsEnabled", true),
                favoritesOnTop = o.optBoolean("favoritesOnTop", false),
                propertyWriteDelayMs = o.optLong("propertyWriteDelayMs", 0L),
                smartRefSimilarityPct = o.optInt("smartRefSimilarityPct", 65),
                maxReferenceImages = o.optInt("maxReferenceImages", 20),
            )
        }.getOrElse { AppSettings() }
    }

    suspend fun saveLibraryViewPrefs(sort: String, groupedView: Boolean) = mutex.withLock {
        write("library_view_prefs.json", JSONObject().apply {
            put("sort", sort)
            put("groupedView", groupedView)
        }.toString())
    }

    suspend fun loadLibraryViewPrefs(): Pair<String, Boolean> = mutex.withLock {
        val file = File(dir, "library_view_prefs.json")
        if (!file.exists()) return@withLock Pair("NEWEST", false)
        runCatching {
            val o = JSONObject(file.readText())
            Pair(o.optString("sort", "NEWEST"), o.optBoolean("groupedView", false))
        }.getOrElse { Pair("NEWEST", false) }
    }

    suspend fun loadSlotBackupSets(): List<SlotBackupSet> = mutex.withLock {
        loadSlotBackupSetsLocked()
    }

    suspend fun backupJson(
        settings: AppSettings,
        cameraLabels: Map<String, String>,
        cameraModels: Map<String, String>,
        cameraFirmwares: Map<String, String>,
        libraryData: LibraryData,
    ): String = mutex.withLock {
        JSONObject().apply {
            put("format", "fujisync-backup")
            put("version", 1)
            put("settings", settingsToJson(settings))
            put("cameras", JSONObject().apply {
                put("labels", stringMapToJson(cameraLabels))
                put("models", stringMapToJson(cameraModels))
                put("firmwares", stringMapToJson(cameraFirmwares))
            })
            put("library", JSONObject().apply {
                put("recipes", recipesToJson(libraryData.recipes))
                put("groups", groupsToJson(libraryData.groups))
                put("styles", stylesToJson(libraryData.styles))
            })
        }.toString(2)
    }

    suspend fun parseBackupJson(content: String): BackupData = mutex.withLock {
        parseBackupJsonInternal(content)
    }

    // ── Camera labels ─────────────────────────────────────────────────

    suspend fun saveCameraLabels(labels: Map<String, String>) = mutex.withLock {
        write("camera_labels.json", stringMapToJson(labels).toString())
    }

    suspend fun loadCameraLabels(): Map<String, String> = mutex.withLock {
        val file = File(dir, "camera_labels.json")
        if (!file.exists()) return@withLock emptyMap()
        runCatching {
            val obj = JSONObject(file.readText())
            mutableMapOf<String, String>().also { m -> obj.keys().forEach { k -> m[k] = obj.getString(k) } }
        }.getOrElse { emptyMap() }
    }

    suspend fun saveCameraModels(models: Map<String, String>) = mutex.withLock {
        write("camera_models.json", stringMapToJson(models).toString())
    }

    suspend fun loadCameraModels(): Map<String, String> = mutex.withLock {
        val file = File(dir, "camera_models.json")
        if (!file.exists()) return@withLock emptyMap()
        runCatching {
            val obj = JSONObject(file.readText())
            mutableMapOf<String, String>().also { m -> obj.keys().forEach { k -> m[k] = obj.getString(k) } }
        }.getOrElse { emptyMap() }
    }

    suspend fun saveCameraFirmwares(firmwares: Map<String, String>) = mutex.withLock {
        write("camera_firmwares.json", stringMapToJson(firmwares).toString())
    }

    suspend fun loadCameraFirmwares(): Map<String, String> = mutex.withLock {
        val file = File(dir, "camera_firmwares.json")
        if (!file.exists()) return@withLock emptyMap()
        runCatching {
            val obj = JSONObject(file.readText())
            mutableMapOf<String, String>().also { m -> obj.keys().forEach { k -> m[k] = obj.getString(k) } }
        }.getOrElse { emptyMap() }
    }

    // ── Internal ──────────────────────────────────────────────────────

    private fun write(name: String, content: String) {
        val dest = File(dir, name)
        val tmp = File(dir, "$name.tmp")
        try {
            tmp.writeText(content)
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }

    private fun loadSlotBackupSetsLocked(): List<SlotBackupSet> {
        val multiFile = File(dir, "slot_backups.json")
        if (multiFile.exists()) {
            return runCatching {
                slotBackupSetsFromJson(JSONArray(multiFile.readText()))
            }.getOrDefault(emptyList())
        }

        val legacySlots = File(dir, "slot_backup.json")
        if (!legacySlots.exists()) return emptyList()
        return runCatching {
            val metaFile = File(dir, "slot_backup_meta.json")
            val meta = if (metaFile.exists()) {
                val o = JSONObject(metaFile.readText())
                SlotBackupMeta(
                    label = o.optString("label").ifBlank { "C1-C7 Backup" },
                    savedAt = o.optString("savedAt").ifBlank { "" },
                    id = "slot-backup-${UUID.randomUUID()}",
                )
            } else {
                SlotBackupMeta("C1-C7 Backup", "", "slot-backup-${UUID.randomUUID()}")
            }
            listOf(SlotBackupSet(meta, slotsFromJson(JSONArray(legacySlots.readText()))))
        }.getOrDefault(emptyList())
    }

    private fun settingsToJson(settings: AppSettings): JSONObject = JSONObject().apply {
        put("showLibraryImages", settings.showLibraryImages)
        put("showCardImageCount", settings.showCardImageCount)
        put("showReferenceImageBlur", settings.showReferenceImageBlur)
        put("hapticsEnabled", settings.hapticsEnabled)
        put("favoritesOnTop", settings.favoritesOnTop)
        put("propertyWriteDelayMs", settings.propertyWriteDelayMs)
    }

    private fun settingsFromJson(o: JSONObject): AppSettings =
        AppSettings(
            showLibraryImages = o.optBoolean("showLibraryImages", true),
            showCardImageCount = o.optBoolean("showCardImageCount", false),
            showReferenceImageBlur = o.optBoolean("showReferenceImageBlur", true),
            hapticsEnabled = o.optBoolean("hapticsEnabled", true),
            favoritesOnTop = o.optBoolean("favoritesOnTop", false),
            propertyWriteDelayMs = o.optLong("propertyWriteDelayMs", 0L),
        )

    private fun stringMapToJson(map: Map<String, String>): JSONObject =
        JSONObject().also { obj -> map.forEach { (key, value) -> obj.put(key, value) } }

    // Library recipes

    private fun recipesToJson(list: List<LibraryRecipeUiModel>): JSONArray =
        JSONArray().also { arr -> list.forEach { arr.put(libraryRecipeToJson(it)) } }

    private fun libraryRecipeToJson(r: LibraryRecipeUiModel): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("sim", r.sim)
        put("pills", JSONArray(r.pills))
        put("saved", r.saved)
        put("description", r.description)
        put("effects", JSONObject(r.effects as Map<*, *>))
        put("tone", JSONObject(r.tone as Map<*, *>))
        put("wb", JSONObject(r.wb as Map<*, *>))
        putOpt("sourceCameraName", r.sourceCameraName)
        putOpt("sourceCameraModel", r.sourceCameraModel)
        putOpt("sourceUsbId", r.sourceUsbId)
        putOpt("sourceUrl", r.sourceUrl)
        putOpt("sourceLabel", r.sourceLabel)
        put("referenceImageUris", JSONArray(r.referenceImageUris))
        put("groupIds", JSONArray(r.groupIds))
        r.groupId?.let { put("groupId", it) }
        put("favorite", r.favorite)
        r.isoMin?.let { put("isoMin", it) }
        r.isoMax?.let { put("isoMax", it) }
        r.exposureCompMin?.let { put("exposureCompMin", it.toDouble()) }
        r.exposureCompMax?.let { put("exposureCompMax", it.toDouble()) }
        if (r.sensorGens.isNotEmpty()) put("sensorGens", JSONArray(r.sensorGens))
    }

    private fun recipesFromJson(arr: JSONArray): List<LibraryRecipeUiModel> =
        (0 until arr.length()).map { libraryRecipeFromJson(arr.getJSONObject(it)) }

    private fun libraryRecipeFromJson(o: JSONObject) = LibraryRecipeUiModel(
        id = o.getString("id"),
        name = o.getString("name"),
        sim = o.getString("sim"),
        pills = o.getJSONArray("pills").toStringList(),
        saved = o.getString("saved"),
        description = o.optString("description"),
        effects = o.optJSONObject("effects")?.toStringMap() ?: emptyMap(),
        tone = o.optJSONObject("tone")?.toStringMap() ?: emptyMap(),
        wb = o.optJSONObject("wb")?.toStringMap() ?: emptyMap(),
        sourceCameraName = o.optString("sourceCameraName").ifEmpty { null },
        sourceCameraModel = o.optString("sourceCameraModel").ifEmpty { null },
        sourceUsbId = o.optString("sourceUsbId").ifEmpty { null },
        sourceUrl = o.optString("sourceUrl").ifEmpty { null },
        sourceLabel = o.optString("sourceLabel").ifEmpty { null },
        referenceImageUris = o.referenceImageUris(),
        groupIds = o.optJSONArray("groupIds")?.toStringList()
            ?: o.optString("groupId").takeIf { it.isNotBlank() }?.let(::listOf)
            ?: emptyList(),
        favorite = o.optBoolean("favorite", false),
        isoMin = o.optInt("isoMin", -1).takeIf { it >= 0 },
        isoMax = o.optInt("isoMax", -1).takeIf { it >= 0 },
        exposureCompMin = o.optDouble("exposureCompMin", Double.NaN).takeIf { !it.isNaN() }?.toFloat(),
        exposureCompMax = o.optDouble("exposureCompMax", Double.NaN).takeIf { !it.isNaN() }?.toFloat(),
        sensorGens = o.optJSONArray("sensorGens")?.toIntList() ?: emptyList(),
    )

    // Groups

    private fun groupsToJson(list: List<LibraryGroupUiModel>): JSONArray =
        JSONArray().also { arr ->
            list.forEach { g ->
                arr.put(JSONObject().apply { put("id", g.id); put("name", g.name) })
            }
        }

    private fun groupsFromJson(arr: JSONArray): List<LibraryGroupUiModel> =
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            LibraryGroupUiModel(id = o.getString("id"), name = o.getString("name"))
        }

    // Group styles

    private fun stylesToJson(map: Map<String, LibraryGroupStyle>): JSONObject =
        JSONObject().also { obj ->
            map.forEach { (id, style) ->
                obj.put(id, JSONObject().apply {
                    putOpt("imageUri", style.imageUri)
                    put("icon", style.icon)
                    put("color", style.color)
                })
            }
        }

    private fun stylesFromJson(obj: JSONObject): Map<String, LibraryGroupStyle> {
        val result = mutableMapOf<String, LibraryGroupStyle>()
        obj.keys().forEach { id ->
            val s = obj.getJSONObject(id)
            result[id] = LibraryGroupStyle(
                imageUri = s.optString("imageUri").ifEmpty { null },
                icon = s.optString("icon").ifEmpty { "Folder" },
                color = s.optString("color").ifEmpty { "Gold" },
            )
        }
        return result
    }

    // Camera slots

    private fun slotsToJson(list: List<RecipeUiModel>): JSONArray =
        JSONArray().also { arr -> list.forEach { arr.put(slotToJson(it)) } }

    private fun slotToJson(r: RecipeUiModel): JSONObject = JSONObject().apply {
        putOpt("libraryId", r.libraryId)
        put("slot", r.slot)
        put("name", r.name)
        put("sim", r.sim)
        put("pills", JSONArray(r.pills))
        put("description", r.description)
        put("effects", JSONObject(r.effects as Map<*, *>))
        put("tone", JSONObject(r.tone as Map<*, *>))
        put("wb", JSONObject(r.wb as Map<*, *>))
        putOpt("saved", r.saved)
        putOpt("sourceCameraName", r.sourceCameraName)
        putOpt("sourceCameraModel", r.sourceCameraModel)
        putOpt("sourceUsbId", r.sourceUsbId)
        putOpt("sourceUrl", r.sourceUrl)
        putOpt("sourceLabel", r.sourceLabel)
        put("referenceImageUris", JSONArray(r.referenceImageUris))
        put("groupIds", JSONArray(r.groupIds))
        putOpt("groupId", r.groupId)
        putOpt("group", r.group)
        put("favorite", r.favorite)
        r.isoMin?.let { put("isoMin", it) }
        r.isoMax?.let { put("isoMax", it) }
        r.exposureCompMin?.let { put("exposureCompMin", it.toDouble()) }
        r.exposureCompMax?.let { put("exposureCompMax", it.toDouble()) }
        if (r.sensorGens.isNotEmpty()) put("sensorGens", JSONArray(r.sensorGens))
    }

    private fun slotsFromJson(arr: JSONArray): List<RecipeUiModel> =
        (0 until arr.length()).map { slotFromJson(arr.getJSONObject(it)) }

    private fun slotBackupSetsToJson(sets: List<SlotBackupSet>): JSONArray =
        JSONArray().also { arr ->
            sets.forEach { set ->
                arr.put(JSONObject().apply {
                    put("id", set.meta.id)
                    put("label", set.meta.label)
                    put("savedAt", set.meta.savedAt)
                    put("slots", slotsToJson(set.slots))
                })
            }
        }

    private fun slotBackupSetsFromJson(arr: JSONArray): List<SlotBackupSet> =
        (0 until arr.length()).mapNotNull { index ->
            runCatching {
                val o = arr.getJSONObject(index)
                SlotBackupSet(
                    meta = SlotBackupMeta(
                        label = o.getString("label"),
                        savedAt = o.optString("savedAt"),
                        id = o.optString("id").ifBlank { "slot-backup-${UUID.randomUUID()}" },
                    ),
                    slots = slotsFromJson(o.getJSONArray("slots")),
                )
            }.getOrNull()
        }

    private fun slotFromJson(o: JSONObject) = RecipeUiModel(
        libraryId = o.optString("libraryId").ifEmpty { null },
        slot = o.getString("slot"),
        name = o.getString("name"),
        sim = o.getString("sim"),
        pills = o.getJSONArray("pills").toStringList(),
        description = o.optString("description"),
        effects = o.optJSONObject("effects")?.toStringMap() ?: emptyMap(),
        tone = o.optJSONObject("tone")?.toStringMap() ?: emptyMap(),
        wb = o.optJSONObject("wb")?.toStringMap() ?: emptyMap(),
        saved = o.optString("saved").ifEmpty { null },
        sourceCameraName = o.optString("sourceCameraName").ifEmpty { null },
        sourceCameraModel = o.optString("sourceCameraModel").ifEmpty { null },
        sourceUsbId = o.optString("sourceUsbId").ifEmpty { null },
        sourceUrl = o.optString("sourceUrl").ifEmpty { null },
        sourceLabel = o.optString("sourceLabel").ifEmpty { null },
        referenceImageUris = o.referenceImageUris(),
        groupIds = o.optJSONArray("groupIds")?.toStringList()
            ?: o.optString("groupId").takeIf { it.isNotBlank() }?.let(::listOf)
            ?: emptyList(),
        group = o.optString("group").ifEmpty { null },
        favorite = o.optBoolean("favorite", false),
        isoMin = o.optInt("isoMin", -1).takeIf { it >= 0 },
        isoMax = o.optInt("isoMax", -1).takeIf { it >= 0 },
        exposureCompMin = o.optDouble("exposureCompMin", Double.NaN).takeIf { !it.isNaN() }?.toFloat(),
        exposureCompMax = o.optDouble("exposureCompMax", Double.NaN).takeIf { !it.isNaN() }?.toFloat(),
        sensorGens = o.optJSONArray("sensorGens")?.toIntList() ?: emptyList(),
    )

    // Helpers

    private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }

    private fun JSONArray.toIntList(): List<Int> = (0 until length()).map { getInt(it) }

    private fun JSONObject.referenceImageUris(): List<String> =
        optJSONArray("referenceImageUris")?.toStringList()
            ?: optString("referenceImageUri").takeIf { it.isNotBlank() }?.let(::listOf)
            ?: emptyList()

    private fun JSONObject.toStringMap(): Map<String, String> =
        mutableMapOf<String, String>().also { m -> keys().forEach { k -> m[k] = getString(k) } }

    data class LibraryData(
        val recipes: List<LibraryRecipeUiModel>,
        val groups: List<LibraryGroupUiModel>,
        val styles: Map<String, LibraryGroupStyle>,
    )

    data class BackupData(
        val settings: AppSettings,
        val cameraLabels: Map<String, String>,
        val cameraModels: Map<String, String>,
        val cameraFirmwares: Map<String, String>,
        val library: LibraryData,
    )

    // ── Zip backup (JSON + reference images) ──────────────────────────

    suspend fun backupZip(
        outputStream: java.io.OutputStream,
        settings: AppSettings,
        cameraLabels: Map<String, String>,
        cameraModels: Map<String, String>,
        cameraFirmwares: Map<String, String>,
        libraryData: LibraryData,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ) = mutex.withLock {
        // Collect every referenced image that exists on disk and assign each a
        // unique zip entry name. Basenames are NOT unique (e.g. discover recipes
        // store images as references/<slug>/image_0.jpg), so we index them and
        // record an explicit original-uri -> entry-name manifest.
        val allUris = (libraryData.recipes.flatMap { it.referenceImageUris } +
            libraryData.styles.values.mapNotNull { it.imageUri }).distinct()

        val entriesToWrite = LinkedHashMap<String, File>() // entryName -> source file
        val manifest = JSONObject()                        // original uri -> entryName
        for (uriStr in allUris) {
            val file = uriToLocalFile(uriStr) ?: continue
            if (!file.exists()) continue
            val ext = file.name.substringAfterLast('.', "jpg")
            val entryName = "images/${entriesToWrite.size}.$ext"
            entriesToWrite[entryName] = file
            manifest.put(uriStr, entryName)
        }

        val json = JSONObject().apply {
            put("format", "fujisync-backup")
            put("version", 3)
            put("settings", settingsToJson(settings))
            put("cameras", JSONObject().apply {
                put("labels", stringMapToJson(cameraLabels))
                put("models", stringMapToJson(cameraModels))
                put("firmwares", stringMapToJson(cameraFirmwares))
            })
            put("library", JSONObject().apply {
                put("recipes", recipesToJson(libraryData.recipes))
                put("groups", groupsToJson(libraryData.groups))
                put("styles", stylesToJson(libraryData.styles))
            })
            put("referenceImages", manifest)
        }.toString(2)

        val total = entriesToWrite.size + 1 // +1 for JSON
        var current = 0

        java.util.zip.ZipOutputStream(outputStream.buffered()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("backup.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            current++
            onProgress(current, total)

            for ((entryName, file) in entriesToWrite) {
                zip.putNextEntry(java.util.zip.ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                current++
                onProgress(current, total)
            }
        }
    }

    suspend fun restoreBackupZip(inputStream: java.io.InputStream): BackupData = mutex.withLock {
        val refDir = File(dir, "ref_images").also { it.mkdirs() }
        val extractedByEntry = mutableMapOf<String, String>()    // zip entry name -> new local uri
        val extractedByBasename = mutableMapOf<String, String>() // basename -> new local uri (legacy fallback)
        var jsonContent: String? = null

        java.util.zip.ZipInputStream(inputStream.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == "backup.json" -> {
                        jsonContent = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    !entry.isDirectory && (name.startsWith("images/") || name.startsWith("ref_images/")) -> {
                        // Extract under a fresh unique name so destination files never collide.
                        val ext = name.substringAfterLast('.', "jpg")
                        val dest = File(refDir, "${UUID.randomUUID()}.$ext")
                        dest.outputStream().use { out -> zip.copyTo(out) }
                        val uri = fileToUriString(dest)
                        extractedByEntry[name] = uri
                        extractedByBasename[name.substringAfterLast('/')] = uri
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val content = jsonContent ?: error("Backup zip does not contain backup.json")
        val backup = parseBackupJsonInternal(content)

        // Manifest maps each original uri to the zip entry that holds its bytes.
        val manifest = JSONObject(content).optJSONObject("referenceImages")
        fun remap(uri: String): String {
            manifest?.optString(uri)?.takeIf { it.isNotEmpty() }?.let { entryName ->
                extractedByEntry[entryName]?.let { return it }
            }
            // Legacy fallback for the early intermediate zip format (filename match).
            uriToFileName(uri)?.let { base -> extractedByBasename[base]?.let { return it } }
            return uri
        }

        val remappedRecipes = backup.library.recipes.map { recipe ->
            recipe.copy(referenceImageUris = recipe.referenceImageUris.map(::remap))
        }
        val remappedStyles = backup.library.styles.mapValues { (_, style) ->
            style.imageUri?.let { style.copy(imageUri = remap(it)) } ?: style
        }

        backup.copy(library = backup.library.copy(recipes = remappedRecipes, styles = remappedStyles))
    }

    private fun parseBackupJsonInternal(content: String): BackupData {
        val root = JSONObject(content)
        if (root.optString("format") != "fujisync-backup") {
            error("This is not a FujiSync backup file.")
        }
        val cameras = root.optJSONObject("cameras") ?: JSONObject()
        val library = root.optJSONObject("library") ?: JSONObject()
        return BackupData(
            settings = root.optJSONObject("settings")?.let(::settingsFromJson) ?: AppSettings(),
            cameraLabels = cameras.optJSONObject("labels")?.toStringMap() ?: emptyMap(),
            cameraModels = cameras.optJSONObject("models")?.toStringMap() ?: emptyMap(),
            cameraFirmwares = cameras.optJSONObject("firmwares")?.toStringMap() ?: emptyMap(),
            library = LibraryData(
                recipes = library.optJSONArray("recipes")?.let(::recipesFromJson) ?: emptyList(),
                groups = library.optJSONArray("groups")?.let(::groupsFromJson) ?: emptyList(),
                styles = library.optJSONObject("styles")?.let(::stylesFromJson) ?: emptyMap(),
            ),
        )
    }

    private fun uriToLocalFile(uriStr: String): File? =
        if (uriStr.startsWith("file://")) {
            File(java.net.URLDecoder.decode(uriStr.removePrefix("file://"), "UTF-8"))
        } else null

    private fun uriToFileName(uriStr: String): String? =
        if (uriStr.startsWith("file://")) {
            File(java.net.URLDecoder.decode(uriStr.removePrefix("file://"), "UTF-8")).name.ifBlank { null }
        } else null

    // Matches android.net.Uri.fromFile(...).toString() for the unencoded
    // filesDir paths we store (absolute path with no special characters).
    private fun fileToUriString(file: File): String = "file://" + file.absolutePath
}
