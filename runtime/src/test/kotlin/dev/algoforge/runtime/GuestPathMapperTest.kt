package dev.algoforge.runtime

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class GuestPathMapperTest {
    @Test
    fun mapsWorkspaceFileIntoGuestWorkspace() {
        val workspace = Files.createTempDirectory("algoforge-workspace").toFile()
        try {
            val source = workspace.resolve("src/main.cpp")
            val mapper = GuestPathMapper(workspace)

            assertEquals("/workspace/src/main.cpp", mapper.toGuest(source.absolutePath))
            assertEquals("/workspace", mapper.toGuest(workspace.absolutePath))
        } finally {
            workspace.deleteRecursively()
        }
    }

    @Test
    fun keepsPathOutsideWorkspaceUnchanged() {
        val workspace = Files.createTempDirectory("algoforge-workspace").toFile()
        val outside = workspace.parentFile.resolve("outside.txt")
        try {
            val original = outside.canonicalPath
            val mapper = GuestPathMapper(workspace)

            assertEquals(original, mapper.toGuest(original))
        } finally {
            workspace.deleteRecursively()
        }
    }
}