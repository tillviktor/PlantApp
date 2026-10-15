package com.plantapp

import java.text.Normalizer
import java.util.Locale

private fun normalizedName(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")

fun searchHistory(entries: List<HistoryEntry>, query: String): List<HistoryEntry> {
    val needle = normalizedName(query)
    if (needle.isEmpty()) return entries
    return entries.filter { entry -> entry.results.any { result ->
        (listOf(result.match.scientificName, result.match.commonName) + result.match.alternativeNames)
            .any { normalizedName(it).contains(needle) }
    } }
}
