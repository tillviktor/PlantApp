package com.plantapp

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class PlantMatch(val scientificName: String, val commonName: String, val family: String, val confidence: Double, val alternativeNames: List<String> = emptyList(), val namesEnriched: Boolean = false) {
    val wikipediaUrl: String get() = "https://en.wikipedia.org/wiki/" + encode(scientificName.replace(' ', '_'))
}
fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

enum class IdentificationFailure { NO_MATCH, KEY_REJECTED, QUOTA, SERVICE }
class IdentificationException(val reason: IdentificationFailure) : Exception()

object PlantApi {
    fun parseMatches(body: String): List<PlantMatch> {
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { index ->
            val item = results.getJSONObject(index)
            val species = item.optJSONObject("species") ?: return@mapNotNull null
            val name = species.optString("scientificNameWithoutAuthor").trim()
            if (name.isBlank()) return@mapNotNull null
            PlantMatch(name, species.optJSONArray("commonNames")?.optString(0).orEmpty().ifBlank { name },
                species.optJSONObject("family")?.optString("scientificNameWithoutAuthor").orEmpty(),
                item.optDouble("score", 0.0).takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0,
                species.optJSONArray("commonNames")?.let { names -> (0 until names.length()).map { names.optString(it).trim() }.filter(String::isNotEmpty).distinct() }.orEmpty())
        }.sortedByDescending { it.confidence }.take(3)
    }

    fun identify(photo: File, apiKey: String): List<PlantMatch> {
        require(photo.isFile && photo.length() > 0) { "Take a photo first." }
        val boundary = "PlantApp-" + UUID.randomUUID()
        val connection = URL("https://my-api.plantnet.org/v2/identify/all?lang=hu&api-key=${encode(apiKey)}").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20000
            connection.readTimeout = 60000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.outputStream.buffered().use { out ->
                fun text(value: String) { out.write(value.toByteArray(Charsets.UTF_8)) }
                text("--$boundary\r\nContent-Disposition: form-data; name=\"organs\"\r\n\r\nauto\r\n")
                text("--$boundary\r\nContent-Disposition: form-data; name=\"images\"; filename=\"plant.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
                photo.inputStream().use { it.copyTo(out) }
                text("\r\n--$boundary--\r\n")
            }
            when (connection.responseCode) {
                200 -> return connection.inputStream.bufferedReader().use { parseMatches(it.readText()) }
                400, 404 -> throw IdentificationException(IdentificationFailure.NO_MATCH)
                401, 403 -> throw IdentificationException(IdentificationFailure.KEY_REJECTED)
                429 -> throw IdentificationException(IdentificationFailure.QUOTA)
                else -> throw IdentificationException(IdentificationFailure.SERVICE)
            }
        } finally { connection.disconnect() }
    }

    /** Resolve scientific names through Wikipedia redirects, then retrieve real hu/en labels and aliases. */
    fun alternativeNames(scientificNames: List<String>): Map<String, List<String>> {
        val output = mutableMapOf<String, List<String>>()
        scientificNames.distinct().chunked(40).forEach { batch ->
            val pages = fetchJson("https://en.wikipedia.org/w/api.php?action=query&redirects=1&prop=pageprops&format=json&titles=${encode(batch.joinToString("|"))}")
            val ids = parseWikiIds(pages, batch)
            if (ids.isNotEmpty()) {
                val entities = fetchJson("https://www.wikidata.org/w/api.php?action=wbgetentities&props=labels%7Caliases&languages=hu%7Cen&format=json&ids=${encode(ids.values.distinct().joinToString("|"))}")
                val labels = parseAlternativeNames(entities)
                ids.forEach { (name, id) -> labels[id]?.let { output[name] = it } }
            }
        }
        return output
    }

    internal fun parseWikiIds(body: String, names: List<String>): Map<String, String> {
        val query = JSONObject(body).optJSONObject("query") ?: return emptyMap()
        val redirects = mutableMapOf<String, String>()
        listOf("normalized", "redirects").forEach { key ->
            query.optJSONArray(key)?.let { array -> (0 until array.length()).forEach { index ->
                val item = array.getJSONObject(index)
                redirects[item.getString("from")] = item.getString("to")
            } }
        }
        val byTitle = mutableMapOf<String, String>()
        query.optJSONObject("pages")?.let { pages -> pages.keys().forEach { key ->
            val page = pages.getJSONObject(key)
            val id = page.optJSONObject("pageprops")?.optString("wikibase_item").orEmpty()
            if (id.matches(Regex("Q[0-9]+"))) byTitle[page.optString("title")] = id
        } }
        return names.mapNotNull { name ->
            var title = name
            val seen = mutableSetOf<String>()
            while (redirects.containsKey(title) && seen.add(title)) title = redirects.getValue(title)
            byTitle[title]?.let { name to it }
        }.toMap()
    }

    internal fun parseAlternativeNames(body: String): Map<String, List<String>> {
        val entities = JSONObject(body).optJSONObject("entities") ?: return emptyMap()
        return entities.keys().asSequence().mapNotNull { id ->
            val entity = entities.optJSONObject(id) ?: return@mapNotNull null
            val names = mutableListOf<String>()
            listOf("hu", "en").forEach { language ->
                entity.optJSONObject("labels")?.optJSONObject(language)?.optString("value")?.let(names::add)
                entity.optJSONObject("aliases")?.optJSONArray(language)?.let { array ->
                    (0 until minOf(array.length(), 100)).forEach { index -> array.optJSONObject(index)?.optString("value")?.let(names::add) }
                }
            }
            names.map(String::trim).filter(String::isNotEmpty).distinct().takeIf { it.isNotEmpty() }?.let { id to it }
        }.toMap()
    }

    private fun fetchJson(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10000; connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "PlantApp/0.4.0 (https://github.com/tillviktor/PlantApp)")
            check(connection.responseCode == 200) { "Name reference unavailable" }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }

    fun summary(name: String): String? {
        val connection = URL("https://en.wikipedia.org/api/rest_v1/page/summary/${encode(name.replace(' ', '_'))}").openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10000; connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "PlantApp/0.4.0 (https://github.com/tillviktor/PlantApp)")
            if (connection.responseCode != 200) null else connection.inputStream.bufferedReader().use {
                val page = JSONObject(it.readText())
                if (page.optString("type") == "disambiguation") null else page.optString("extract").takeIf(String::isNotBlank)
            }
        } finally { connection.disconnect() }
    }
}
