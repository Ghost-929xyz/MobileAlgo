package dev.algoforge.runtime

import dev.algoforge.core.Language
import java.io.File

data class DebugAdapterSpec(
    val id: String,
    val executable: String,
    val arguments: List<String>,
)

class DebugAdapterFactory(
    private val paths: RuntimePaths,
) {
    fun adapterFor(language: Language, source: File): DebugAdapterSpec? {
        val prefix = ProotCommandFactory(paths)
        val workspace = source.parentFile ?: return null
        val base = prefix.prefix(workspace)
        return when (language) {
            Language.PYTHON -> DebugAdapterSpec(
                id = "debugpy",
                executable = base.first(),
                arguments = base.drop(1) + listOf(
                    "/usr/bin/python3",
                    "-m",
                    "debugpy.adapter",
                ),
            )

            Language.CPP, Language.C -> DebugAdapterSpec(
                id = "lldb-dap",
                executable = base.first(),
                arguments = base.drop(1) + listOf(
                    "/usr/bin/lldb-dap",
                ),
            )

            Language.JAVA -> null
        }
    }
}
