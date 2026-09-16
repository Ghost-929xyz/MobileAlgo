package dev.algoforge.runtime

import android.content.Context
import android.system.Os
import dev.algoforge.core.io.AtomicFileWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.UUID

data class InstallProgress(
    val phase: String,
    val bytesCopied: Long = 0,
    val totalBytes: Long = -1,
)

class RuntimeInstaller(
    private val context: Context,
    private val paths: RuntimePaths = RuntimePaths.from(context),
) {
    suspend fun install(
        abi: String,
        index: RuntimeIndex = RuntimeIndexLoader(context).load(),
        onProgress: (InstallProgress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val runtime = index.abis[abi]
            ?: error("No runtime is configured for ABI $abi. Supported manifest ABIs: ${index.abis.keys.joinToString()}")
        validate(runtime)

        val runtimeParent = paths.root.parentFile ?: error("Runtime root has no parent: ${paths.root}")
        check(runtimeParent.mkdirs() || runtimeParent.isDirectory) {
            "Unable to create runtime parent directory: $runtimeParent"
        }
        val staging = File(runtimeParent, "${paths.root.name}.staging-${UUID.randomUUID()}")
        staging.deleteRecursively()
        check(staging.mkdirs()) { "Unable to create runtime staging directory" }
        try {
            val prootDownload = File(staging, "proot.download")
            download(runtime.proot, prootDownload, onProgress)
            val prootTarget = File(staging, "bin/proot")
            prootTarget.parentFile?.mkdirs()
            prootDownload.copyTo(prootTarget, overwrite = true)
            prootDownload.delete()
            Os.chmod(prootTarget.absolutePath, "755".toInt(8))

            val rootfsTarget = File(staging, "rootfs")
            rootfsTarget.mkdirs()
            val bootstrapDownload = File(staging, "bootstrap.tar.gz")
            download(runtime.bootstrap, bootstrapDownload, onProgress)
            extractTarGz(
                archive = bootstrapDownload,
                destination = rootfsTarget,
                stripComponents = runtime.bootstrap.stripComponents,
                onProgress = { copied ->
                    onProgress(InstallProgress("extract", copied, runtime.bootstrap.size))
                },
            )
            bootstrapDownload.delete()
            ensureTermuxLayout(rootfsTarget)

            installState(staging, abi, runtime)
            replaceAtomically(staging)
            onProgress(InstallProgress("complete"))
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    fun status(): RuntimeStatus {
        val reasons = mutableListOf<String>()
        if (!paths.proot.isFile) reasons += "proot executable is missing"
        if (!paths.rootfs.isDirectory) reasons += "rootfs is missing"
        if (!File(paths.rootfs, "usr").exists()) reasons += "guest /usr prefix is missing"
        if (!paths.stateFile.isFile) reasons += "runtime metadata is missing"
        val state = if (paths.stateFile.isFile) {
            runCatching { RuntimeStateCodec.decode(paths.stateFile.readText(StandardCharsets.UTF_8)) }.getOrNull()
        } else {
            null
        }
        if (paths.stateFile.isFile && state == null) add("runtime metadata is invalid")
        return RuntimeStatus(
            installed = reasons.isEmpty(),
            abi = state?.abi.orEmpty(),
            version = state?.version.orEmpty(),
            installedBytes = if (paths.root.exists()) paths.root.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0,
            missingReasons = reasons,
        )
    }

    fun uninstall() {
        paths.root.deleteRecursively()
    }

    private fun validate(runtime: RuntimeAbi) {
        require(runtime.provider == PROVIDER) { "Unsupported runtime provider: ${runtime.provider}" }
        require(runtime.proot.isConfigured()) {
            "proot artifact is not pinned. Update runtime-index.json with an immutable HTTPS URL, size, and SHA-256."
        }
        require(runtime.bootstrap.isConfigured()) {
            "bootstrap artifact is not pinned. Update runtime-index.json with an immutable HTTPS URL, size, and SHA-256."
        }
        require(runtime.proot.archiveFormat.equals("binary", ignoreCase = true)) {
            "The proot artifact must use archiveFormat=binary"
        }
        require(runtime.bootstrap.archiveFormat.equals("tarGz", ignoreCase = true)) {
            "The bootstrap artifact must use archiveFormat=tarGz"
        }
        require(runtime.proot.size > 0 && runtime.bootstrap.size > 0) {
            "Runtime artifact sizes must be pinned before installation"
        }
    }

    private fun download(
        artifact: RuntimeArtifact,
        target: File,
        onProgress: (InstallProgress) -> Unit,
    ) {
        if (!artifact.isConfigured()) error("Artifact ${artifact.version} is not configured")
        val url = URL(artifact.url)
        require(url.protocol.equals("https", ignoreCase = true)) { "Runtime artifacts must use HTTPS: ${artifact.url}" }
        target.parentFile?.mkdirs()

        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("User-Agent", "AlgoForge/0.1")
        }
        try {
            val response = connection.responseCode
            if (response !in 200..299) error("HTTP $response while downloading ${artifact.url}")
            require(connection.url.protocol.equals("https", ignoreCase = true)) {
                "Runtime artifact redirects to a non-HTTPS URL: ${connection.url}"
            }
            val expectedSize = artifact.size.takeIf { it > 0 } ?: connection.contentLengthLong
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        copied += count
                        require(copied <= artifact.size) {
                            "Artifact ${artifact.version} exceeded its pinned size of ${artifact.size} bytes"
                        }
                        onProgress(InstallProgress("download:${artifact.version}", copied, expectedSize))
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(artifact.sha256, ignoreCase = true)) {
                target.delete()
                error("SHA-256 mismatch for ${artifact.version}")
            }
            if (artifact.size > 0 && copied != artifact.size) {
                target.delete()
                error("Size mismatch for ${artifact.version}: expected ${artifact.size}, got $copied")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractTarGz(
        archive: File,
        destination: File,
        stripComponents: Int,
        onProgress: (Long) -> Unit,
    ) {
        val destinationPath = destination.canonicalFile.toPath()
        var extractedBytes = 0L
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(FileInputStream(archive)))).use { tar ->
            while (true) {
                val entry = tar.nextEntry as? TarArchiveEntry ?: break
                extractedBytes += entry.size.coerceAtLeast(0)
                val relative = stripLeadComponents(entry.name, stripComponents)
                if (relative.isBlank()) continue
                val target = safeResolve(destinationPath, relative)
                requireNoSymlinkParent(destinationPath, target)
                when {
                    entry.isDirectory -> Files.createDirectories(target)
                    entry.isSymbolicLink -> {
                        require(entry.linkName.isNotBlank()) { "Empty symlink target for ${entry.name}" }
                        Files.createDirectories(target.parent)
                        Files.deleteIfExists(target)
                        Files.createSymbolicLink(target, Paths.get(entry.linkName))
                    }
                    entry.isFile -> {
                        Files.createDirectories(target.parent)
                        FileOutputStream(target.toFile()).use { output -> tar.copyTo(output) }
                        val mode = (entry.mode and 0x1FF).takeIf { it != 0 } ?: DEFAULT_FILE_MODE
                        Os.chmod(target.toString(), mode)
                    }
                    else -> Unit
                }
                onProgress(extractedBytes)
            }
        }
    }

    private fun safeResolve(destination: Path, relative: String): Path {
        val normalized = destination.resolve(relative).normalize()
        require(normalized.startsWith(destination)) { "Archive entry escaped runtime root: $relative" }
        return normalized
    }

    private fun requireNoSymlinkParent(root: Path, target: Path) {
        var current = target.parent
        while (current != null && current != root) {
            require(!Files.isSymbolicLink(current)) { "Archive entry traverses a symlink: $current" }
            current = current.parent
        }
    }

    private fun stripLeadComponents(name: String, count: Int): String {
        if (count <= 0) return name
        return name.split('/').drop(count).joinToString("/")
    }

    private fun ensureTermuxLayout(rootfs: File) {
        val termuxPrefix = File(rootfs, TERMUX_PREFIX_PATH)
        val guestUsr = File(rootfs, "usr")
        if (termuxPrefix.isDirectory) {
            val link = guestUsr.toPath()
            if (Files.exists(link, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(link)) {
                guestUsr.deleteRecursively()
            }
            if (!Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
                Files.createSymbolicLink(link, Paths.get(TERMUX_PREFIX_PATH))
            }
        } else {
            guestUsr.mkdirs()
        }

        val prefixRoot = if (termuxPrefix.isDirectory) termuxPrefix else guestUsr
        listOf("bin", "lib", "libexec", "share", "tmp", "var").forEach { prefixRoot.resolve(it).mkdirs() }
        File(rootfs, "data/data/com.termux/files/home").mkdirs()
        File(rootfs, "tmp").mkdirs()
        File(rootfs, "root").mkdirs()
        File(rootfs, "dev").mkdirs()
        File(rootfs, "proc").mkdirs()
        File(rootfs, "sys").mkdirs()
    }

    private fun installState(staging: File, abi: String, runtime: RuntimeAbi) {
        val state = RuntimeState(
            schemaVersion = 1,
            abi = abi,
            version = runtime.bootstrap.version,
            provider = runtime.provider,
            installedAtEpochMillis = System.currentTimeMillis(),
        )
        AtomicFileWriter.writeText(
            File(staging, "runtime.json").toPath(),
            RuntimeStateCodec.encode(state),
            StandardCharsets.UTF_8,
        )
    }

    private fun replaceAtomically(staging: File) {
        val parent = paths.root.parentFile ?: error("Runtime root has no parent")
        parent.mkdirs()
        require(staging.parentFile == parent) { "Runtime staging directory must be a sibling of the active runtime" }
        val backup = File(parent, "${paths.root.name}.backup")
        backup.deleteRecursively()
        if (paths.root.exists() && !paths.root.renameTo(backup)) {
            error("Unable to move current runtime aside")
        }
        if (!staging.renameTo(paths.root)) {
            if (backup.exists()) backup.renameTo(paths.root)
            error("Unable to activate downloaded runtime")
        }
        backup.deleteRecursively()
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
        const val DEFAULT_FILE_MODE = 0b110100100
        const val PROVIDER = "termux-proot"
        const val TERMUX_PREFIX_PATH = "data/data/com.termux/files/usr"
    }
}
