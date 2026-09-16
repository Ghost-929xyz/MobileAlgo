package dev.algoforge.ide.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.algoforge.core.Language
import dev.algoforge.core.ResourceLimits
import dev.algoforge.core.RunPlanFactory
import dev.algoforge.core.SourceFile
import dev.algoforge.core.WorkspaceProject
import dev.algoforge.core.cph.CphProblem
import dev.algoforge.core.cph.CphTest
import dev.algoforge.core.cph.TestReport
import dev.algoforge.ide.AlgoForgeApplication
import dev.algoforge.ide.data.ProjectSnapshot
import dev.algoforge.runtime.InstallProgress
import dev.algoforge.runtime.ProcessEvent
import dev.algoforge.runtime.RuntimeIndexLoader
import dev.algoforge.runtime.RuntimeStatus
import dev.algoforge.runtime.resolveFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class IdeTab {
    CODE,
    TESTS,
    CONSOLE,
    ENVIRONMENT,
}

data class IdeUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val tab: IdeTab = IdeTab.CODE,
    val project: WorkspaceProject? = null,
    val sourceFile: File? = null,
    val files: List<SourceFile> = emptyList(),
    val code: String = "",
    val stdin: String = "",
    val output: String = "",
    val standardError: String = "",
    val exitCode: Int? = null,
    val durationMillis: Long = 0,
    val problem: CphProblem = CphProblem(),
    val reports: List<TestReport> = emptyList(),
    val runtimeStatus: RuntimeStatus? = null,
    val installProgress: String? = null,
    val message: String? = null,
)

class IdeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as AlgoForgeApplication).container
    private val _state = MutableStateFlow(IdeUiState())
    val state: StateFlow<IdeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { initialize() }
    }

    fun selectTab(tab: IdeTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    fun updateCode(code: String) {
        _state.update { it.copy(code = code) }
    }

    fun updateStdin(value: String) {
        _state.update { it.copy(stdin = value) }
    }

    fun showMessage(message: String) {
        _state.update { it.copy(message = message) }
    }

    fun updateProblem(transform: (CphProblem) -> CphProblem) {
        _state.update { current -> current.copy(problem = transform(current.problem)) }
        viewModelScope.launch { persistProblem() }
    }

    fun addTest() {
        updateProblem { problem -> problem.copy(tests = problem.tests + CphTest()) }
    }

    fun removeTest(index: Int) {
        updateProblem { problem ->
            problem.copy(tests = problem.tests.filterIndexed { currentIndex, _ -> currentIndex != index })
        }
    }

    fun selectFile(file: SourceFile) {
        viewModelScope.launch {
            val project = _state.value.project ?: return@launch
            saveCurrentFile()
            val hostFile = container.workspaceRepository.sourceFile(project, file.relativePath)
            val code = container.workspaceRepository.readText(hostFile)
            val problem = container.workspaceRepository.readProblem(project, hostFile)
            _state.update {
                it.copy(
                    sourceFile = hostFile,
                    code = code,
                    problem = problem,
                    reports = emptyList(),
                    tab = IdeTab.CODE,
                )
            }
        }
    }

    fun run() {
        viewModelScope.launch { executeRun(runTests = false) }
    }

    fun runTests() {
        viewModelScope.launch { executeRun(runTests = true) }
    }

    fun installRuntime() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, installProgress = "preparing") }
            try {
                val index = RuntimeIndexLoader(getApplication()).load()
                val resolved = index.resolveFor(Build.SUPPORTED_ABIS.toList())
                    ?: error("No runtime artifact is configured for supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
                container.runtimeInstaller.install(
                    abi = resolved.abi,
                    index = index,
                    onProgress = { progress -> publishProgress(progress) },
                )
                refreshRuntime()
                _state.update { it.copy(busy = false, installProgress = null, message = "Runtime installed") }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        busy = false,
                        installProgress = null,
                        message = error.message ?: "Runtime installation failed",
                    )
                }
            }
        }
    }

    fun installToolchain() {
        viewModelScope.launch {
            val snapshot = currentSnapshot() ?: return@launch
            _state.update { it.copy(busy = true, installProgress = "installing toolchain") }
            try {
                container.toolchainInstaller.install(snapshot.project.language.toLanguage(), snapshot.directory).collect { event ->
                    val text = when (event) {
                        is ProcessEvent.Started -> "running: ${event.command.joinToString(" ")}"
                        is ProcessEvent.Stdout -> event.text.trim()
                        is ProcessEvent.Stderr -> event.text.trim()
                        is ProcessEvent.Exited -> "finished with exit code ${event.exitCode}"
                        is ProcessEvent.TimedOut -> "timed out"
                        is ProcessEvent.Failed -> event.message
                    }
                    _state.update { it.copy(installProgress = text.takeLast(160)) }
                }
                _state.update { it.copy(busy = false, installProgress = null, message = "Toolchain installation finished") }
            } catch (error: Throwable) {
                _state.update { it.copy(busy = false, installProgress = null, message = error.message ?: "Toolchain installation failed") }
            }
        }
    }

    private suspend fun initialize() {
        try {
            loadSnapshot(container.workspaceRepository.ensureDefaultProject())
        } catch (error: Throwable) {
            _state.update { it.copy(loading = false, message = error.message ?: "Unable to create workspace") }
        }
    }

    private suspend fun loadSnapshot(snapshot: ProjectSnapshot) {
        val files = container.workspaceRepository.listSourceFiles(snapshot.project)
        val code = container.workspaceRepository.readText(snapshot.sourceFile)
        val problem = container.workspaceRepository.readProblem(snapshot.project, snapshot.sourceFile)
        val runtimeStatus = container.runtimeInstaller.status()
        _state.update {
            it.copy(
                loading = false,
                project = snapshot.project,
                sourceFile = snapshot.sourceFile,
                files = files,
                code = code,
                problem = problem,
                runtimeStatus = runtimeStatus,
            )
        }
    }

    private suspend fun executeRun(runTests: Boolean) {
        val snapshot = currentSnapshot() ?: return
        saveCurrentFile()
        val runtimeStatus = container.runtimeInstaller.status()
        _state.update { it.copy(runtimeStatus = runtimeStatus) }
        if (!runtimeStatus.installed) {
            _state.update { it.copy(tab = IdeTab.ENVIRONMENT, message = "Install a runtime before running code") }
            return
        }
        _state.update {
            it.copy(
                busy = true,
                tab = if (runTests) IdeTab.TESTS else IdeTab.CONSOLE,
                reports = if (runTests) it.reports else emptyList(),
                output = "",
                standardError = "",
                exitCode = null,
                message = null,
            )
        }
        val language = snapshot.project.language.toLanguage()
        val plan = RunPlanFactory.create(
            language = language,
            source = snapshot.sourceFile,
            workspace = snapshot.directory,
            outputDirectory = File(snapshot.directory, ".algoforge/out"),
            limits = limitsFrom(_state.value.problem),
            standardInput = _state.value.stdin,
        )
        try {
            if (runTests) {
                val reports = container.cphTestExecutor.run(plan, _state.value.problem.tests)
                _state.update { it.copy(reports = reports, busy = false) }
            } else {
                val result = container.runtimeExecutor.run(plan)
                _state.update {
                    it.copy(
                        output = result.stdout,
                        standardError = result.stderr,
                        exitCode = result.exitCode,
                        durationMillis = result.durationMillis,
                        busy = false,
                    )
                }
            }
        } catch (error: Throwable) {
            _state.update { it.copy(busy = false, message = error.message ?: "Execution failed") }
        }
    }

    private suspend fun saveCurrentFile() {
        val current = _state.value
        val file = current.sourceFile ?: return
        container.workspaceRepository.writeText(file, current.code)
    }

    private suspend fun persistProblem() {
        val current = _state.value
        val project = current.project ?: return
        val source = current.sourceFile ?: return
        container.workspaceRepository.saveProblem(project, source, current.problem)
    }

    private fun currentSnapshot(): ProjectSnapshot? {
        val current = _state.value
        val project = current.project ?: return null
        val source = current.sourceFile ?: return null
        return ProjectSnapshot(project, container.workspaceRepository.projectDirectory(project), source)
    }

    private fun limitsFrom(problem: CphProblem): ResourceLimits {
        val timeout = ((problem.timeLimitSeconds ?: 2.0) * 1000).toLong().coerceIn(100, 60_000)
        return ResourceLimits(
            wallTimeMillis = timeout,
            memoryMegabytes = problem.memoryLimitMegabytes?.coerceAtLeast(16),
        )
    }

    private suspend fun refreshRuntime() {
        _state.update { it.copy(runtimeStatus = container.runtimeInstaller.status()) }
    }

    private fun publishProgress(progress: InstallProgress) {
        val total = progress.totalBytes.takeIf { it > 0 }
        val suffix = if (total == null) "" else " / ${total / 1024 / 1024} MiB"
        _state.update {
            it.copy(installProgress = "${progress.phase}: ${progress.bytesCopied / 1024 / 1024} MiB$suffix")
        }
    }
}
