package dev.algoforge.runtime

import java.io.File

class ProotCommandFactory(
    private val paths: RuntimePaths,
) {
    fun prefix(hostWorkspace: File, workingDirectoryGuest: String = "/workspace"): List<String> {
        require(paths.proot.isFile) { "Runtime is not installed: ${paths.proot}" }
        require(paths.rootfs.isDirectory) { "Runtime rootfs is missing: ${paths.rootfs}" }
        return buildList {
            add(paths.proot.absolutePath)
            add("--kill-on-exit")
            add("-0")
            add("-r")
            add(paths.rootfs.absolutePath)
            add("-w")
            add(workingDirectoryGuest)
            add("-b")
            add("/dev:/dev")
            add("-b")
            add("/proc:/proc")
            add("-b")
            add("/sys:/sys")
            add("-b")
            add("${hostWorkspace.canonicalPath}:/workspace")
        }
    }
}
