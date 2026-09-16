package dev.algoforge.core.io

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.io.IOException

/**
 * Writes a file through a same-directory temporary file and then replaces the target.
 *
 * Android filesystems do not all support ATOMIC_MOVE. The fallback still avoids exposing a
 * partially-written target, but may not be crash-atomic on those filesystems.
 */
object AtomicFileWriter {
    fun writeText(
        target: Path,
        text: String,
        charset: Charset = StandardCharsets.UTF_8,
    ) {
        val parent = target.toAbsolutePath().parent
            ?: throw IllegalArgumentException("Target has no parent: $target")
        Files.createDirectories(parent)
        val temporary = Files.createTempFile(parent, ".${target.fileName}.", ".tmp")
        try {
            Files.write(
                temporary,
                text.toByteArray(charset),
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
            )
            moveReplacing(temporary, target)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun moveReplacing(source: Path, target: Path) {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (atomicFailure: AtomicMoveNotSupportedException) {
            moveWithFallback(source, target, atomicFailure)
        } catch (unsupported: UnsupportedOperationException) {
            moveWithFallback(source, target, unsupported)
        }
    }

    private fun moveWithFallback(source: Path, target: Path, firstFailure: Throwable) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        } catch (fallbackFailure: IOException) {
            fallbackFailure.addSuppressed(firstFailure)
            throw fallbackFailure
        }
    }
}