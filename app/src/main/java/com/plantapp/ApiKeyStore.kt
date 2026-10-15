package com.plantapp

import java.io.File
import java.nio.file.Files
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Only authenticated ciphertext is persisted. The encryption key is supplied by Android Keystore. */
class ApiKeyStore(private val file: File, private val key: () -> SecretKey) {
    fun load(): String {
        if (!file.exists()) return ""
        val bytes = file.readBytes()
        require(bytes.size >= 29 && bytes[0] == 1.toByte()) { "Invalid encrypted key format" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        return String(cipher.doFinal(bytes.copyOfRange(13, bytes.size)), Charsets.UTF_8)
    }
    fun save(value: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) { clear(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        check(cipher.iv.size == 12)
        writePrivateFile(file, byteArrayOf(1) + cipher.iv + cipher.doFinal(normalized.toByteArray(Charsets.UTF_8)))
    }
    fun clear() { Files.deleteIfExists(file.toPath()) }
    fun exists() = file.exists()
}
