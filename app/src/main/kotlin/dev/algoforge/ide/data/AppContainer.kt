package dev.algoforge.ide.data

import android.content.Context
import dev.algoforge.runtime.CphTestExecutor
import dev.algoforge.runtime.RuntimeExecutor
import dev.algoforge.runtime.RuntimeInstaller
import dev.algoforge.runtime.RuntimePaths
import dev.algoforge.runtime.ToolchainInstaller

class AppContainer(context: Context) {
    val workspaceRepository = WorkspaceRepository(context)
    val runtimePaths = RuntimePaths.from(context)
    val runtimeInstaller = RuntimeInstaller(context, runtimePaths)
    val runtimeExecutor = RuntimeExecutor(runtimePaths)
    val toolchainInstaller = ToolchainInstaller(runtimePaths)
    val cphTestExecutor = CphTestExecutor(runtimeExecutor)
}
