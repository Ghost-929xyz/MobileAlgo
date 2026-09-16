package dev.algoforge.core.cph

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CphRepositoryTest {
    @Test
    fun savesAndReloadsExplicitStableKey() {
        val workspace = Files.createTempDirectory("algoforge-cph")
        try {
            val repository = CphRepository(workspace)
            val problem = CphProblem(
                name = "A. Sum",
                url = "https://example.test/a",
                tests = listOf(CphTest(input = "1 2\n", output = "3\n")),
            )

            val path = repository.save(problem, key = "main")

            assertEquals("main.json", path.fileName.toString())
            assertEquals(problem, repository.load(path))
            assertTrue(repository.list().contains(path))
        } finally {
            workspace.toFile().deleteRecursively()
        }
    }
}