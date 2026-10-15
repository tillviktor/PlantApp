package com.plantapp

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HistoryStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private fun entry(id: String, time: Long, summary: String? = "Magyar leírás: őszirózsa 🌿") = HistoryEntry(id, time,
        listOf(PlantResult(PlantMatch("Bellis perennis", "Százszorszép", "Asteraceae", 0.95), summary),
            PlantResult(PlantMatch("Rosa canina", "Gyepűrózsa", "Rosaceae", 0.1), null)))
    private fun file() = File(folder.root, "private/history.json")

    @Test fun persistsAllResultsAcrossNewStoreInstances() {
        val file = file()
        val original = entry("first", 1000)
        assertTrue(HistoryStore(file).load().isEmpty())
        HistoryStore(file).add(original)
        assertEquals(listOf(original), HistoryStore(file).load())
        val text = file.readText()
        assertFalse(text.contains("apiKey"))
        assertFalse(text.contains("photo"))
        assertTrue(file.parentFile!!.listFiles()!!.none { it.name.endsWith(".tmp") })
    }
    @Test fun newestFirstAndDeduplicatesRepeatedSave() {
        val store = HistoryStore(file())
        store.add(entry("newer", 2000))
        store.add(entry("older", 1000))
        val updated = entry("newer", 2000, "Frissített leírás")
        assertEquals(listOf(updated, entry("older", 1000)), store.add(updated))
        assertEquals(listOf(updated, entry("older", 1000)), store.load())
    }
    @Test fun retainsOnlyLatestHundredEvenAfterReload() {
        val store = HistoryStore(file())
        (1..105).forEach { store.add(entry("$it", it.toLong())) }
        val loaded = HistoryStore(file()).load()
        assertEquals(100, loaded.size)
        assertEquals("105", loaded.first().id)
        assertEquals("6", loaded.last().id)
    }
    @Test fun clearIsDurableAndIdempotent() {
        val file = file(); val store = HistoryStore(file)
        store.add(entry("one", 1))
        assertTrue(store.clear().isEmpty())
        assertFalse(file.exists())
        assertTrue(HistoryStore(file).load().isEmpty())
        assertTrue(store.clear().isEmpty())
        assertEquals(1, store.add(entry("new", 2)).size)
    }
    @Test fun corruptHistoryIsNotOverwrittenAndCanBeCleared() {
        val file = file(); file.parentFile!!.mkdirs(); file.writeText("broken history")
        val store = HistoryStore(file)
        assertThrows(Exception::class.java) { store.load() }
        assertThrows(Exception::class.java) { store.add(entry("one", 1)) }
        assertEquals("broken history", file.readText())
        store.clear()
        assertEquals(1, store.add(entry("two", 2)).size)
    }
    @Test fun rejectsUnknownSchemaWithoutOverwriting() {
        val file = file(); file.parentFile!!.mkdirs(); val original = """{"version":99,"entries":[]}"""; file.writeText(original)
        assertThrows(IllegalArgumentException::class.java) { HistoryStore(file).add(entry("one", 1)) }
        assertEquals(original, file.readText())
    }
    @Test fun invalidNewEntryDoesNotDamageExistingHistory() {
        val file = file(); val store = HistoryStore(file); val original = entry("valid", 1); store.add(original)
        assertThrows(IllegalArgumentException::class.java) { store.add(HistoryEntry("invalid", 2, emptyList())) }
        assertEquals(listOf(original), HistoryStore(file).load())
    }
    @Test fun failedWriteLeavesOriginalHistoryReadable() {
        val file = file(); val store = HistoryStore(file); val original = entry("valid", 1); store.add(original)
        val missingDirectory = File(file, "child.json")
        assertThrows(Exception::class.java) { HistoryStore(missingDirectory).add(entry("invalid", 2)) }
        assertEquals(listOf(original), store.load())
    }
}
