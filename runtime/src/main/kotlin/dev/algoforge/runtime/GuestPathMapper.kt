package dev.algoforge.runtime

import java.io.File

class GuestPathMapper(
    hostWorkspace: File,
    private val guestWorkspace: String = "/workspace",
) {
    private val host = hostWorkspace.canonicalFile.toPath()

    fun toGuest(hostPath: String): String {
        val path = File(hostPath).canonicalFile.toPath()
        if (!path.startsWith(host)) return hostPath
        val relative = host.relativize(path).toString().replace(File.separatorChar, '/')
        return if (relative.isBlank()) guestWorkspace else "$guestWorkspace/$relative"
    }
}
