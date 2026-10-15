package com.plantapp

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal fun writePrivateFile(file: File, bytes: ByteArray) {
    val directory = requireNotNull(file.parentFile)
    check(directory.isDirectory || directory.mkdirs())
    val temporary = File.createTempFile("private-", ".tmp", directory)
    try {
        FileOutputStream(temporary).use { it.write(bytes); it.fd.sync() }
        try { Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
        catch (_: AtomicMoveNotSupportedException) { Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
    } finally { temporary.delete() }
}
