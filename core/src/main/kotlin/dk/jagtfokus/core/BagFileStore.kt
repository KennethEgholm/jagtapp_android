package dk.jagtfokus.core

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Writes to a temp file next to [target], then renames over it. A crash never leaves half a file. */
fun writeAtomically(target: File, bytes: ByteArray) {
    val dir = target.absoluteFile.parentFile
    dir.mkdirs()
    val temp = File(dir, target.name + ".tmp")
    try {
        temp.outputStream().use { out ->
            out.write(bytes)
            out.flush()
            out.fd.sync()
        }
        try {
            Files.move(
                temp.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    } catch (e: IOException) {
        temp.delete()
        throw e
    }
}

/**
 * One file with a rolling backup and corrupt-file preservation, shared by the
 * journal and the species list. [valid] decides what counts as a good file.
 */
internal class RollingFile(private val file: File, private val valid: (ByteArray) -> Boolean) {
    private val dir = file.absoluteFile.parentFile
    private val backup = File(dir, file.nameWithoutExtension + ".backup.json")
    private val corrupt = File(dir, file.nameWithoutExtension + ".corrupt.json")

    val exists: Boolean
        get() = file.isFile || backup.isFile

    /** Good bytes from the main file, else from the backup (which is restored). Null if neither is good. */
    fun load(): ByteArray? {
        if (file.isFile) {
            val bytes = runCatching { file.readBytes() }.getOrNull()
            if (bytes != null && valid(bytes)) return bytes
            preserveCorruptFile()
        }
        val backupBytes = runCatching { backup.readBytes() }.getOrNull()
        if (backupBytes != null && valid(backupBytes)) {
            runCatching { writeAtomically(file, backupBytes) }
            return backupBytes
        }
        return null
    }

    fun save(bytes: ByteArray) {
        if (file.isFile) {
            val existing = runCatching { file.readBytes() }.getOrNull()
            if (existing != null && valid(existing)) {
                runCatching { writeAtomically(backup, existing) }
            } else {
                preserveCorruptFile()
            }
        }
        writeAtomically(file, bytes)
    }

    private fun preserveCorruptFile() {
        runCatching { writeAtomically(corrupt, file.readBytes()) }
    }
}

/**
 * Safe local storage with rolling backup and corrupt-file preservation.
 * Port of BagFileStore.swift. The file stays on the phone; no cloud sync.
 */
class BagFileStore(fileURL: File) {
    private val rolling = RollingFile(fileURL) { runCatching { BagJournal.decode(it) }.isSuccess }

    fun load(): BagJournal =
        rolling.load()?.let { BagJournal.decode(it) } ?: BagJournal()

    fun save(journal: BagJournal) = rolling.save(BagJournal.encode(journal))
}

/**
 * Species list next to the bag file, same name as iOS: artsliste.json. Unlike
 * iOS it has the journal's rolling backup too.
 */
class SpeciesChecklistFileStore(fileURL: File) {
    private val rolling = RollingFile(fileURL) { runCatching { SpeciesChecklist.decode(it) }.isSuccess }

    val fileExists: Boolean
        get() = rolling.exists

    fun load(): SpeciesChecklist? =
        rolling.load()?.let { SpeciesChecklist.decode(it) }

    fun save(list: SpeciesChecklist) = rolling.save(SpeciesChecklist.encode(list))
}
