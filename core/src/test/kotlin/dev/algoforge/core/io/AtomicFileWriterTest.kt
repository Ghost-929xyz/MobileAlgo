package dev.algoforge.core.io

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class AtomicFileWriterTest {
    @Test
    fun replacesExistingFileAndLeavesNoTemporaryFiles() {
        val directory = Files.createTempDirectory("algoforge-atomic")
        try {
            val target = directory.resolve("data.txt")
            Files.writeString(target, "old", StandardCharsets.UTF_8)

            AtomicFileWriter.writeText(target, "new", StandardCharsets.UTF_8)

            assertEquals("new", Files.readString(target, StandardCharsets.UTF_8))
            val temporaryFiles = Files.list(directory).use { paths ->
                paths.filter { it.fileName.toString().endsWith(".tmp") }.count()
            }
            assertEquals(0, temporaryFiles)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}