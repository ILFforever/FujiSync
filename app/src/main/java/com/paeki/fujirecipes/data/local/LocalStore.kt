package com.paeki.fujirecipes.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.paeki.fujirecipes.ui.model.AppSettings
import com.paeki.fujirecipes.ui.model.LibraryGroupStyle
import com.paeki.fujirecipes.ui.model.LibraryGroupUiModel
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.model.SlotBackupMeta
import com.paeki.fujirecipes.ui.model.SlotBackupSet
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
         .getOrNull()
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

    // ── App settings ──────────────────────────────────────────────────

    suspend fun saveSettings(settings: AppSettings) = mutex.withLock {
        write("settings.json", JSONObject().apply {
            put("showLibraryImages", settings.showLibraryImages)
            put("propertyWriteDelayMs", settings.propertyWriteDelayMs)
        }.toString())
    }

    suspend fun loadSettings(): AppSettings = mutex.withLock {
        val file = File(dir, "settings.json")
        if (!file.exists()) return@withLock AppSettings()
        runCatching {
            val o = JSONObject(file.readText())
            AppSettings(
                showLibraryImages = o.optBoolean("showLibraryImages", true),
                propertyWriteDelayMs = o.optLong("propertyWriteDelayMs", 0L),
            )
        }.getOrElse { AppSettings() }
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
        val root = JSONObject(content)
        if (root.optString("format") != "fujisync-backup") {
            error("This is not a FujiSync backup file.")
        }
        val cameras = root.optJSONObject("cameras") ?: JSONObject()
        val library = root.optJSONObject("library") ?: JSONObject()
        BackupData(
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
        put("propertyWriteDelayMs", settings.propertyWriteDelayMs)
    }

    private fun settingsFromJson(o: JSONObject): AppSettings =
        AppSettings(
            showLibraryImages = o.optBoolean("showLibraryImages", true),
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
        put("referenceImageUris", JSONArray(r.referenceImageUris))
        put("groupIds", JSONArray(r.groupIds))
        r.groupId?.let { put("groupId", it) }
        put("favorite", r.favorite)
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
        referenceImageUris = o.referenceImageUris(),
        groupIds = o.optJSONArray("groupIds")?.toStringList()
            ?: o.optString("groupId").takeIf { it.isNotBlank() }?.let(::listOf)
            ?: emptyList(),
        favorite = o.optBoolean("favorite", false),
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
        put("referenceImageUris", JSONArray(r.referenceImageUris))
        put("groupIds", JSONArray(r.groupIds))
        putOpt("groupId", r.groupId)
        putOpt("group", r.group)
        put("favorite", r.favorite)
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
        referenceImageUris = o.referenceImageUris(),
        groupIds = o.optJSONArray("groupIds")?.toStringList()
            ?: o.optString("groupId").takeIf { it.isNotBlank() }?.let(::listOf)
            ?: emptyList(),
        group = o.optString("group").ifEmpty { null },
        favorite = o.optBoolean("favorite", false),
    )

    // Helpers

    private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }

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
}
