package com.plantapp

import org.junit.Assert.*
import org.junit.Test

class HistorySearchTest {
    private val daisy = HistoryEntry("daisy", 2, listOf(PlantResult(
        PlantMatch("Bellis perennis", "Százszorszép", "Asteraceae", 0.9, listOf("Common daisy", "English daisy", "százszorszép")), null)))
    private val rose = HistoryEntry("rose", 1, listOf(PlantResult(PlantMatch("Rosa canina", "Gyepűrózsa", "Rosaceae", 0.9), null)))
    private val entries = listOf(daisy, rose)
    @Test fun searchesHungarianEnglishAndLatinNamesIgnoringCaseAndAccents() {
        listOf("SZAZSZORSZEP", "common DAISY", "bellis", " english   daisy ", "perennis").forEach {
            assertEquals(listOf(daisy), searchHistory(entries, it))
        }
        assertEquals(listOf(rose), searchHistory(entries, "gyepurozsa"))
        assertEquals(listOf(rose), searchHistory(entries, "RÓSA"))
    }
    @Test fun emptyQueryRestoresOrderAndUnmatchedQueryIsEmpty() {
        assertEquals(entries, searchHistory(entries, "  "))
        assertTrue(searchHistory(entries, "nem létező növény").isEmpty())
        assertTrue(searchHistory(emptyList(), "rose").isEmpty())
    }
    @Test fun searchesSecondaryCandidatesButNotDescriptionOrFamily() {
        val entry = daisy.copy(results = daisy.results + rose.results)
        assertEquals(listOf(entry), searchHistory(listOf(entry), "Rosa canina"))
        assertTrue(searchHistory(entries, "Asteraceae").isEmpty())
        assertTrue(searchHistory(listOf(daisy.copy(results = listOf(daisy.results.first().copy(summary = "rose")))), "rose").isEmpty())
    }
}
