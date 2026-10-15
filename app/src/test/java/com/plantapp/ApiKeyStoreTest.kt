package com.plantapp

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.KeyGenerator

class ApiKeyStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private fun file() = File(folder.root, "private/api-key.enc")

    @Test fun encryptedKeySurvivesNewStoreInstanceWithoutPlaintextOnDisk() {
        val file = file(); val key = key(); val secret = "test-only-sample-api-key"
        ApiKeyStore(file) { key }.save(" $secret ")
        assertEquals(secret, ApiKeyStore(file) { key }.load())
        assertFalse(String(file.readBytes(), Charsets.ISO_8859_1).contains(secret))
    }
    @Test fun repeatedSavesUseDifferentNoncesAndUpdateStoredValue() {
        val file = file(); val key = key(); val store = ApiKeyStore(file) { key }
        store.save("test-key"); val first = file.readBytes()
        store.save("test-key"); assertFalse(first.contentEquals(file.readBytes()))
        store.save("replacement"); assertEquals("replacement", ApiKeyStore(file) { key }.load())
    }
    @Test fun forgetAndEmptySaveRemoveThePersistedKey() {
        val file = file(); val key = key(); val store = ApiKeyStore(file) { key }
        assertEquals("", store.load())
        store.save("test-key"); store.clear(); store.clear()
        assertFalse(file.exists()); assertEquals("", ApiKeyStore(file) { key }.load())
        store.save("test-key"); store.save("  "); assertFalse(store.exists())
    }
    @Test fun tamperingAndWrongEncryptionKeyAreRejectedWithoutOverwritingData() {
        val file = file(); val key = key(); val store = ApiKeyStore(file) { key }; store.save("test-key")
        val original = file.readBytes()
        assertThrows(Exception::class.java) { ApiKeyStore(file) { key() }.load() }
        assertArrayEquals(original, file.readBytes())
        val damaged = original.clone(); damaged[damaged.lastIndex] = (damaged.last().toInt() xor 1).toByte(); file.writeBytes(damaged)
        assertThrows(Exception::class.java) { store.load() }
        assertArrayEquals(damaged, file.readBytes())
        store.save("recovered"); assertEquals("recovered", store.load())
    }
}
