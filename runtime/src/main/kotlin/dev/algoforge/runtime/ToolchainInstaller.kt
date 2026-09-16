package dev.algoforge.runtime

import dev.algoforge.core.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class ToolchainInstaller(
    private val paths: RuntimePaths,
    private val processRunner: ProcessRunner = ProcessRunner(),
) {
    fun install(language: Language, workspace: File): Flow<ProcessEvent> = flow {
        val packages = packagesFor(language)
        val prefix = ProotCommandFactory(paths).prefix(workspace)
        processRunner.execute(
            command = prefix + listOf(
                "/usr/bin/apt-get",
                "-o",
                "Dpkg::Options::=--force-confold",
                "update",
            ),
            workingDirectory = workspace,
            environment = baseEnvironment(),
            timeoutMillis = 10 * 60_000,
        ).collect { emit(it) }

        processRunner.execute(
            command = prefix + listOf(
                "/usr/bin/apt-get",
                "-y",
                "-o",
                "Dpkg::Options::=--force-confold",
                "install",
            ) + packages,
            workingDirectory = workspace,
            environment = baseEnvironment(),
            timeoutMillis = 30 * 60_000,
        ).collect { emit(it) }
    }

    fun packagesFor(language: Language): List<String> = when (language) {
        Language.PYTHON -> listOf("python", "python-pip")
        Language.CPP, Language.C -> listOf("clang", "lldb", "make", "cmake")
        Language.JAVA -> listOf("openjdk-17", "ecj")
    }

    private fun baseEnvironment(): Map<String, String> = mapOf(
        "HOME", "/root",
        "TMPDIR", "/tmp",
        "TERM", "dumb",
        "PROOT_TMP_DIR", paths.root.resolve("tmp").absolutePath,
        "PROOT_NO_SECCOMP", "1",
    )
}
