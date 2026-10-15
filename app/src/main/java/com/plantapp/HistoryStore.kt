package com.plantapp

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

data class PlantResult(val match: PlantMatch, val summary: String?)
data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val recognizedAtMillis: Long = System.currentTimeMillis(),
    val results: List<PlantResult>,
)

/** Private, bounded history. Contains neither API credentials nor photographs. */
class HistoryStore(private val file: File) {
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
                        result.getString("family"), score),
                        if (result.isNull("summary")) null else result.getString("summary"))
                }).also { require(it.id.isNotBlank() && it.recognizedAtMillis > 0) }
        }.sortedByDescending { it.recognizedAtMillis }.take(MAX_ENTRIES)
    }

    @Synchronized fun add(entry: HistoryEntry): List<HistoryEntry> {
        require(entry.id.isNotBlank() && entry.recognizedAtMillis > 0 && entry.results.size in 1..3)
        require(entry.results.all { it.match.scientificName.isNotBlank() && it.match.confidence.isFinite() && it.match.confidence in 0.0..1.0 })
        // Loading first preserves damaged/unknown history instead of silently overwriting it.
        val entries = (load().filterNot { it.id == entry.id } + entry)
            .sortedByDescending { it.recognizedAtMillis }.take(MAX_ENTRIES)
        val array = JSONArray()
        entries.forEach { item ->
            val results = JSONArray()
            item.results.forEach { result ->
                results.put(JSONObject().put("scientificName", result.match.scientificName)
                    .put("commonName", result.match.commonName).put("family", result.match.family)
                    .put("confidence", result.match.confidence).put("summary", result.summary ?: JSONObject.NULL))
            }
            array.put(JSONObject().put("id", item.id).put("recognizedAtMillis", item.recognizedAtMillis).put("results", results))
        }
        val directory = requireNotNull(file.parentFile)
        check(directory.isDirectory || directory.mkdirs())
        val temporary = File.createTempFile("history-", ".tmp", directory)
        try {
            FileOutputStream(temporary).use { out ->
                out.write(JSONObject().put("version", 1).put("entries", array).toString().toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            try { Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        } finally { temporary.delete() }
        return entries
    }

    @Synchronized fun clear(): List<HistoryEntry> {
        Files.deleteIfExists(file.toPath())
        return emptyList()
    }
}
