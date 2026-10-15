package com.plantapp

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.util.UUID

data class PlantResult(val match: PlantMatch, val summary: String?)
data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val recognizedAtMillis: Long = System.currentTimeMillis(),
    val results: List<PlantResult>,
    val photoName: String? = null,
)

/** Private, bounded history with optional photos. Never contains API credentials. */
class HistoryStore(private val file: File) {
    private val photos = File(file.parentFile, "history-photos")
    private fun validPhotoName(name: String) = name.matches(Regex("[a-f0-9-]{36}\\.jpg"))
    companion object { const val MAX_ENTRIES = 100 }

    @Synchronized fun load(): List<HistoryEntry> {
        if (!file.exists()) return emptyList()
        val root = JSONObject(file.readText(Charsets.UTF_8))
        require(root.getInt("version") == 1) { "Unsupported history format" }
        val entries = root.getJSONArray("entries")
        return (0 until entries.length()).map { index ->
            val entry = entries.getJSONObject(index)
            val results = entry.getJSONArray("results")
            require(results.length() in 1..3)
            HistoryEntry(entry.getString("id"), entry.getLong("recognizedAtMillis"),
                (0 until results.length()).map { resultIndex ->
                    val result = results.getJSONObject(resultIndex)
                    val score = result.getDouble("confidence")
                    val scientificName = result.getString("scientificName")
                    require(scientificName.isNotBlank() && score.isFinite() && score in 0.0..1.0)
                    PlantResult(PlantMatch(scientificName, result.getString("commonName"),
                        result.getString("family"), score,
                        result.optJSONArray("alternativeNames")?.let { names -> (0 until names.length()).map { names.getString(it) } }.orEmpty(), result.optBoolean("namesEnriched", false)),
                        if (result.isNull("summary")) null else result.getString("summary"))
                }, if (entry.isNull("photoName")) null else entry.getString("photoName").also { require(validPhotoName(it)) })
                .also { require(it.id.isNotBlank() && it.recognizedAtMillis > 0) }
        }.sortedByDescending { it.recognizedAtMillis }.take(MAX_ENTRIES)
    }

    @Synchronized fun add(entry: HistoryEntry, sourcePhoto: File? = null): List<HistoryEntry> {
        require(entry.id.isNotBlank() && entry.recognizedAtMillis > 0 && entry.results.size in 1..3)
        require(entry.results.all { it.match.scientificName.isNotBlank() && it.match.confidence.isFinite() && it.match.confidence in 0.0..1.0 })
        // Loading first preserves damaged/unknown history instead of silently overwriting it.
        val oldEntries = load()
        val previous = oldEntries.firstOrNull { it.id == entry.id }
        val photoName = if (sourcePhoto == null) entry.photoName ?: previous?.photoName else UUID.randomUUID().toString() + ".jpg"
        require(photoName == null || validPhotoName(photoName))
        // A late description update must preserve names fetched concurrently for this record.
        val savedEntry = entry.copy(photoName = photoName, results = entry.results.map { result ->
            val oldMatch = previous?.results?.firstOrNull { it.match.scientificName == result.match.scientificName }?.match
            result.copy(match = result.match.copy(
                alternativeNames = (result.match.alternativeNames + oldMatch?.alternativeNames.orEmpty()).distinct(),
                namesEnriched = result.match.namesEnriched || oldMatch?.namesEnriched == true))
        })
        val entries = (oldEntries.filterNot { it.id == entry.id } + savedEntry)
            .sortedByDescending { it.recognizedAtMillis }.take(MAX_ENTRIES)
        val targetPhoto = if (sourcePhoto != null) File(photos, requireNotNull(photoName)) else null
        try {
            if (targetPhoto != null) {
                require(sourcePhoto!!.isFile && sourcePhoto.length() > 0)
                writePrivateFile(targetPhoto, sourcePhoto.readBytes())
            }
            persist(entries)
        } catch (failure: Exception) {
            targetPhoto?.delete()
            throw failure
        }
        val retained = entries.mapNotNull { it.photoName }.toSet()
        photos.listFiles()?.filter { it.isFile && validPhotoName(it.name) && it.name !in retained }?.forEach { it.delete() }
        return entries
    }

    private fun persist(entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { item ->
            val results = JSONArray()
            item.results.forEach { result ->
                results.put(JSONObject().put("scientificName", result.match.scientificName)
                    .put("commonName", result.match.commonName).put("family", result.match.family)
                    .put("confidence", result.match.confidence).put("summary", result.summary ?: JSONObject.NULL)
                    .put("alternativeNames", JSONArray(result.match.alternativeNames)).put("namesEnriched", result.match.namesEnriched))
            }
            val json = JSONObject().put("id", item.id).put("recognizedAtMillis", item.recognizedAtMillis).put("results", results)
            item.photoName?.let { json.put("photoName", it) }
            array.put(json)
        }
        writePrivateFile(file, JSONObject().put("version", 1).put("entries", array).toString().toByteArray(Charsets.UTF_8))
    }

    @Synchronized fun remove(id: String): List<HistoryEntry> {
        val entries = load()
        val removed = entries.firstOrNull { it.id == id } ?: return entries
        val remaining = entries.filterNot { it.id == id }
        removed.photoName?.takeIf { name -> remaining.none { it.photoName == name } }?.let { name ->
            Files.deleteIfExists(File(photos, name).toPath())
        }
        persist(remaining)
        return remaining
    }

    @Synchronized fun updateAlternativeNames(names: Map<String, List<String>>): List<HistoryEntry> {
        val entries = load()
        val updated = entries.map { entry -> entry.copy(results = entry.results.map { result ->
            val extra = names[result.match.scientificName].orEmpty()
            result.copy(match = result.match.copy(alternativeNames = (result.match.alternativeNames + extra).map(String::trim).filter(String::isNotEmpty).distinct(), namesEnriched = result.match.namesEnriched || names.containsKey(result.match.scientificName)))
        }) }
        if (updated != entries) persist(updated)
        return updated
    }

    fun photoFile(entry: HistoryEntry): File? = entry.photoName?.takeIf(::validPhotoName)?.let { File(photos, it) }?.takeIf { it.isFile }

    @Synchronized fun clear(): List<HistoryEntry> {
        // Delete photos first so a failed cleanup can be retried from the history screen.
        photos.listFiles()?.filter { it.isFile }?.forEach { Files.deleteIfExists(it.toPath()) }
        Files.deleteIfExists(file.toPath())
        return emptyList()
    }
}
