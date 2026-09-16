package dev.algoforge.runtime

import android.content.Context
import java.io.File

data class RuntimePaths(
    val root: File,
    val rootfs: File,
    val proot: File,
    val stateFile: File,
) {
    companion object {
        fun from(context: Context): RuntimePaths {
            val root = File(context.filesDir, "runtimes/termux")
            return RuntimePaths(
                root = root,
                rootfs = File(root, "rootfs"),
                proot = File(root, "bin/proot"),
                stateFile = File(root, "runtime.json"),
            )
        }
    }
}

data class RuntimeStatus(
    val installed: Boolean,
    val abi: String = "",
    val version: String = "",
    val installedBytes: Long = 0,
    val missingReasons: List<String> = emptyList(),
)
