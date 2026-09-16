package dev.algoforge.runtime

import dev.algoforge.core.CommandPhase
import dev.algoforge.core.RunPlan
import java.io.File

class RuntimeExecutor(
    private val paths: RuntimePaths,
    private val processRunner: ProcessRunner = ProcessRunner(),
) {
    suspend fun run(plan: RunPlan): ProcessResult {
        val workspace = File(plan.workspaceDirectory)
        val mapper = GuestPathMapper(workspace)
        val prefix = ProotCommandFactory(paths).prefix(workspace)
        plan.compilePhase?.let { phase ->
            val compileResult = executePhase(phase, workspace, mapper, prefix, standardInput = "")
            if (compileResult.exitCode != 0 || compileResult.timedOut) return compileResult
        }
        return executePhase(plan.runPhase, workspace, mapper, prefix, plan.standardInput)
    }

    suspend fun runPhase(plan: RunPlan, phase: CommandPhase, standardInput: String): ProcessResult {
        val workspace = File(plan.workspaceDirectory)
        val mapper = GuestPathMapper(workspace)
        val prefix = ProotCommandFactory(paths).prefix(workspace)
        return executePhase(phase, workspace, mapper, prefix, standardInput)
    }

    private suspend fun executePhase(
        phase: CommandPhase,
        workspace: File,
        mapper: GuestPathMapper,
        prefix: List<String>,
        standardInput: String,
    ): ProcessResult {
        val translated = phase.command.map(mapper::toGuest)
        val environment = buildMap {
            put("HOME", "/root")
            put("TMPDIR", "/tmp")
            put("LANG", "C.UTF-8")
            put("LC_ALL", "C.UTF-8")
            put("PROOT_TMP_DIR", paths.root.resolve("tmp").absolutePath)
            put("PROOT_NO_SECCOMP", "1")
            putAll(phase.environment)
        }
        return processRunner.capture(
            command = prefix + translated,
            workingDirectory = workspace,
            environment = environment,
            standardInput = standardInput,
            timeoutMillis = phase.timeoutMillis,
        )
    }
}
