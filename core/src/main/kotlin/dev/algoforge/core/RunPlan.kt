package dev.algoforge.core

import java.nio.file.Path

data class ResourceLimits(
    val wallTimeMillis: Long = 2_000,
    val compileTimeMillis: Long = 60_000,
    val memoryMegabytes: Int? = null,
)

data class CommandPhase(
    val id: String,
    val executable: String,
    val arguments: List<String>,
    val workingDirectory: String,
    val timeoutMillis: Long,
    val environment: Map<String, String> = emptyMap(),
) {
    val command: List<String> = buildList {
        add(executable)
        addAll(arguments)
    }
}

enum class RunMode {
    RUN,
    TEST,
    DEBUG,
}

data class RunPlan(
    val language: Language,
    val mode: RunMode,
    val workspaceDirectory: String,
    val sourceFile: String,
    val outputDirectory: String,
    val compilePhase: CommandPhase?,
    val runPhase: CommandPhase,
    val limits: ResourceLimits,
    val standardInput: String = "",
)

object RunPlanFactory {
    private const val PYTHON = "/usr/bin/python3"
    private const val CLANGXX = "/usr/bin/clang++"
    private const val CLANG = "/usr/bin/clang"
    private const val JAVAC = "/usr/bin/javac"
    private const val JAVA = "/usr/bin/java"

    fun create(
        language: Language,
        source: Path,
        workspace: Path,
        outputDirectory: Path,
        limits: ResourceLimits = ResourceLimits(),
        standardInput: String = "",
        mode: RunMode = RunMode.RUN,
    ): RunPlan {
        val sourceName = source.fileName.toString()
        val workspaceText = workspace.toAbsolutePath().toString()
        val outputText = outputDirectory.toAbsolutePath().toString()
        val sourceText = source.toAbsolutePath().toString()
        val runnable = outputDirectory.resolve("main").toAbsolutePath().toString()

        val (compile, run) = when (language) {
            Language.PYTHON -> null to CommandPhase(
                id = "run",
                executable = PYTHON,
                arguments = listOf(sourceText),
                workingDirectory = workspaceText,
                timeoutMillis = limits.wallTimeMillis,
            )

            Language.CPP -> CommandPhase(
                id = "compile",
                executable = CLANGXX,
                arguments = listOf(
                    "-std=c++20",
                    "-O2",
                    "-g3",
                    "-pipe",
                    "-Wall",
                    "-Wextra",
                    sourceText,
                    "-o",
                    runnable,
                ),
                workingDirectory = workspaceText,
                timeoutMillis = limits.compileTimeMillis,
            ) to CommandPhase(
                id = "run",
                executable = runnable,
                arguments = emptyList(),
                workingDirectory = workspaceText,
                timeoutMillis = limits.wallTimeMillis,
            )

            Language.C -> CommandPhase(
                id = "compile",
                executable = CLANG,
                arguments = listOf(
                    "-std=c17",
                    "-O2",
                    "-g3",
                    "-pipe",
                    "-Wall",
                    "-Wextra",
                    sourceText,
                    "-o",
                    runnable,
                ),
                workingDirectory = workspaceText,
                timeoutMillis = limits.compileTimeMillis,
            ) to CommandPhase(
                id = "run",
                executable = runnable,
                arguments = emptyList(),
                workingDirectory = workspaceText,
                timeoutMillis = limits.wallTimeMillis,
            )

            Language.JAVA -> CommandPhase(
                id = "compile",
                executable = JAVAC,
                arguments = listOf(
                    "-encoding",
                    "UTF-8",
                    "-g",
                    "-d",
                    outputText,
                    sourceText,
                ),
                workingDirectory = workspaceText,
                timeoutMillis = limits.compileTimeMillis,
            ) to CommandPhase(
                id = "run",
                executable = JAVA,
                arguments = listOf(
                    "-XX:+UseSerialGC",
                    "-Xmx" + (limits.memoryMegabytes ?: 256) + "m",
                    "-cp",
                    outputText,
                    sourceName.removeSuffix(".java"),
                ),
                workingDirectory = workspaceText,
                timeoutMillis = limits.wallTimeMillis,
            )
        }

        return RunPlan(
            language = language,
            mode = mode,
            workspaceDirectory = workspaceText,
            sourceFile = sourceText,
            outputDirectory = outputText,
            compilePhase = compile,
            runPhase = run,
            limits = limits,
            standardInput = standardInput,
        )
    }
}
