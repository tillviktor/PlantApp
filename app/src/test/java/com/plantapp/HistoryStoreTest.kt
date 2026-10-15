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
    @Test fun photoPersistsAfterCacheRemovalAndIsDeletedWithHistory() {
        val file = file(); val store = HistoryStore(file)
        val camera = File(folder.root, "cache.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val saved = store.add(entry("with-photo", 1), camera).first()
        camera.delete()
        val restored = HistoryStore(file)
        assertEquals(saved, restored.load().first())
        assertArrayEquals(byteArrayOf(1, 2, 3), restored.photoFile(saved)!!.readBytes())
        restored.add(saved.copy(results = listOf(saved.results.first().copy(summary = "updated"))))
        assertTrue(restored.photoFile(saved)!!.exists())
        restored.clear()
        assertNull(restored.photoFile(saved))
    }
    @Test fun oldHistoryWithoutPhotosStillLoadsAndNewPhotosArePrunedAtLimit() {
        val file = file(); val store = HistoryStore(file)
        store.add(entry("old", 1)); assertNull(HistoryStore(file).load().first().photoName)
        val camera = File(folder.root, "cache.jpg").apply { writeText("test-image") }
        val first = store.add(entry("photo", 2), camera).first()
        val firstFile = store.photoFile(first)!!
        (3..103).forEach { store.add(entry("$it", it.toLong()), camera) }
        assertFalse(firstFile.exists())
        assertEquals(100, File(file.parentFile, "history-photos").listFiles()!!.size)
    }
    @Test fun failedPhotoCopyLeavesExistingHistoryAndPhotoIntact() {
        val file = file(); val store = HistoryStore(file)
        val camera = File(folder.root, "cache.jpg").apply { writeText("photo") }
        val old = store.add(entry("old", 1), camera).first()
        assertThrows(Exception::class.java) { store.add(entry("new", 2), File(folder.root, "missing.jpg")) }
        assertEquals(listOf(old), store.load()); assertNotNull(store.photoFile(old))
    }
    @Test fun photoPathsCannotEscapePrivateDirectory() {
        val store = HistoryStore(file())
        assertThrows(IllegalArgumentException::class.java) { store.add(entry("bad", 1).copy(photoName = "../secret.jpg")) }
        assertNull(store.photoFile(entry("bad", 1).copy(photoName = "../secret.jpg")))
    }

}
