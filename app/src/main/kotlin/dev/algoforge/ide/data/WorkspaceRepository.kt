package dev.algoforge.ide.data

import android.content.Context
import dev.algoforge.core.Language
import dev.algoforge.core.LanguageId
import dev.algoforge.core.ProjectTemplates
import dev.algoforge.core.SourceFile
import dev.algoforge.core.WorkspaceProject
import dev.algoforge.core.cph.CphProblem
import dev.algoforge.core.cph.CphRepository
import dev.algoforge.core.io.AtomicFileWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets

data class ProjectSnapshot(
    val project: WorkspaceProject,
    val directory: File,
    val sourceFile: File,
)

class WorkspaceRepository(
    context: Context,
) {
    private val workspacesRoot = File(context.filesDir, "workspaces")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val mutex = Mutex()

    suspend fun ensureDefaultProject(): ProjectSnapshot = mutex.withLock {
        withContext(Dispatchers.IO) {
            val existing = findExistingProject()
            if (existing != null) return@withContext loadSnapshot(existing)
            createProjectInternal(
                name = "Playground",
                language = Language.CPP,
            )
        }
    }

    suspend fun createProject(name: String, language: Language): ProjectSnapshot = mutex.withLock {
        withContext(Dispatchers.IO) { createProjectInternal(name, language) }
    }

    suspend fun readText(file: File): String = withContext(Dispatchers.IO) {
        if (file.exists()) file.readText(StandardCharsets.UTF_8) else ""
    }

    suspend fun writeText(file: File, text: String) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        AtomicFileWriter.writeText(file.toPath(), text, StandardCharsets.UTF_8)
    }

    suspend fun listSourceFiles(project: WorkspaceProject): List<SourceFile> = withContext(Dispatchers.IO) {
        val root = projectDirectory(project)
        if (!root.isDirectory) return@withContext emptyList()
        root.walkTopDown()
            .onEnter { it.name != ".algoforge" && it.name != ".cph" && !it.name.startsWith(".") }
            .filter { it.isFile && it.length() <= 2 * 1024 * 1024 }
            .map { file ->
                val relative = root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')
                SourceFile(
                    relativePath = relative,
                    displayName = file.name,
                    language = Language.fromFileName(file.name),
                    sizeBytes = file.length(),
                    lastModifiedEpochMillis = file.lastModified(),
                )
            }
            .sortedWith(compareBy<SourceFile> { it.relativePath.count { char -> char == '/' } }.thenBy { it.relativePath })
            .toList()
    }

    suspend fun readProblem(project: WorkspaceProject, sourceFile: File): CphProblem {
        return withContext(Dispatchers.IO) {
            val repository = CphRepository(projectDirectory(project))
            val preferred = repository.list().firstOrNull { path -> path.fileName.toString().startsWith(sourceFile.nameWithoutExtension) }
                ?: repository.list().firstOrNull()
            if (preferred == null) CphProblem(name = sourceFile.nameWithoutExtension)
            else repository.load(preferred)
        }
    }

    suspend fun saveProblem(project: WorkspaceProject, sourceFile: File, problem: CphProblem) {
        withContext(Dispatchers.IO) {
            val repository = CphRepository(projectDirectory(project))
            repository.save(problem, key = sourceFile.nameWithoutExtension)
        }
    }

    fun projectDirectory(project: WorkspaceProject): File = File(workspacesRoot, project.name)

    fun sourceFile(project: WorkspaceProject, relativePath: String): File = File(projectDirectory(project), relativePath)

    private fun createProjectInternal(name: String, language: Language): ProjectSnapshot {
        workspacesRoot.mkdirs()
        val safeName = sanitizeName(name)
        var directory = File(workspacesRoot, safeName)
        var suffix = 2
        while (directory.exists()) {
            directory = File(workspacesRoot, "$safeName-$suffix")
            suffix += 1
        }
        directory.mkdirs()
        File(directory, ".algoforge").mkdirs()
        File(directory, ".cph").mkdirs()
        File(directory, ".algoforge/out").mkdirs()

        val now = System.currentTimeMillis()
        val source = File(directory, language.defaultSourceName)
        source.writeText(ProjectTemplates.sourceFor(language), StandardCharsets.UTF_8)
        val project = WorkspaceProject(
            name = directory.name,
            language = LanguageId.from(language),
            entryFile = language.defaultSourceName,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        writeProjectMetadata(directory, project)
        return ProjectSnapshot(project, directory, source)
    }

    private fun loadSnapshot(directory: File): ProjectSnapshot {
        val metadata = File(directory, ".algoforge/project.json")
        val project = if (metadata.isFile) {
            json.decodeFromString(WorkspaceProject.serializer(), metadata.readText(StandardCharsets.UTF_8))
        } else {
            val language = Language.fromFileName("main.cpp") ?: Language.CPP
            WorkspaceProject(
                name = directory.name,
                language = LanguageId.from(language),
                entryFile = "main.cpp",
                createdAtEpochMillis = directory.lastModified(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }
        val source = File(directory, project.entryFile).takeIf { it.isFile }
            ?: directory.listFiles()?.firstOrNull { it.isFile && Language.fromFileName(it.name) != null }
            ?: File(directory, "main.py").also { it.writeText(ProjectTemplates.sourceFor(Language.PYTHON)) }
        return ProjectSnapshot(project, directory, source)
    }

    private fun findExistingProject(): File? {
        if (!workspacesRoot.isDirectory) return null
        return workspacesRoot.listFiles()
            ?.filter { it.isDirectory && File(it, ".algoforge/project.json").isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.firstOrNull()
    }

    private fun writeProjectMetadata(directory: File, project: WorkspaceProject) {
        val target = File(directory, ".algoforge/project.json")
        AtomicFileWriter.writeText(target.toPath(), json.encodeToString(WorkspaceProject.serializer(), project), StandardCharsets.UTF_8)
    }

    private fun sanitizeName(name: String): String {
        val safe = name.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\u4e00-\u9fff_-]+"), "-")
            .trim('-')
            .take(48)
        return safe.ifBlank { "project" }
    }
}
