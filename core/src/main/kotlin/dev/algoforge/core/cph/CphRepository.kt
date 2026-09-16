package dev.algoforge.core.cph

import dev.algoforge.core.io.AtomicFileWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class CphRepository(
    private val workspace: Path,
) {
    private val cphDirectory: Path = workspace.resolve(".cph")

    fun list(): List<Path> {
        if (!Files.isDirectory(cphDirectory)) return emptyList()
        val paths = ArrayList<Path>()
        Files.list(cphDirectory).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .forEach(paths::add)
        }
        return paths.sorted()
    }

    fun load(path: Path): CphProblem = CphCodec.decode(String(Files.readAllBytes(path), StandardCharsets.UTF_8))

    fun save(problem: CphProblem, key: String = stableKey(problem)): Path {
        Files.createDirectories(cphDirectory)
        val target = cphDirectory.resolve("$key.json")
        AtomicFileWriter.writeText(target, CphCodec.encode(problem), StandardCharsets.UTF_8)
        return target
    }

    fun stableKey(problem: CphProblem): String {
        val identity = problem.url.ifBlank { problem.name }
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(identity.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(16)
        val slug = problem.name
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(32)
            .ifBlank { "problem" }
        return "$slug-$digest"
    }
}