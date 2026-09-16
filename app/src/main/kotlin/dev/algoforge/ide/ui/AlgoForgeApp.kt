package dev.algoforge.ide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.algoforge.core.SourceFile
import dev.algoforge.ide.ui.theme.AlgoForgeTheme

@Composable
fun AlgoForgeApp(
    viewModel: IdeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissMessage()
    }

    AlgoForgeTheme {
        Scaffold(
            topBar = { AlgoForgeTopBar(state) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = state.tab == IdeTab.CODE,
                        onClick = { viewModel.selectTab(IdeTab.CODE) },
                        icon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                        label = { Text("代码") },
                    )
                    NavigationBarItem(
                        selected = state.tab == IdeTab.TESTS,
                        onClick = { viewModel.selectTab(IdeTab.TESTS) },
                        icon = { Icon(Icons.Outlined.FactCheck, contentDescription = null) },
                        label = { Text("测试") },
                    )
                    NavigationBarItem(
                        selected = state.tab == IdeTab.CONSOLE,
                        onClick = { viewModel.selectTab(IdeTab.CONSOLE) },
                        icon = { Icon(Icons.Outlined.Terminal, contentDescription = null) },
                        label = { Text("控制台") },
                    )
                    NavigationBarItem(
                        selected = state.tab == IdeTab.ENVIRONMENT,
                        onClick = { viewModel.selectTab(IdeTab.ENVIRONMENT) },
                        icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                        label = { Text("环境") },
                    )
                }
            },
            floatingActionButton = {
                if (state.tab != IdeTab.ENVIRONMENT) {
                    ExtendedFloatingActionButton(
                        onClick = if (state.tab == IdeTab.TESTS) viewModel::runTests else viewModel::run,
                        icon = {
                            if (state.busy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            }
                        },
                        text = { Text(if (state.tab == IdeTab.TESTS) "全部测试" else "运行") },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                else -> Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    when (state.tab) {
                        IdeTab.CODE -> CodeTab(state, viewModel::selectFile, viewModel::updateCode)
                        IdeTab.TESTS -> TestsScreen(state, viewModel::updateProblem, viewModel::addTest, viewModel::removeTest)
                        IdeTab.CONSOLE -> ConsoleScreen(state, viewModel::updateStdin, viewModel::run)
                        IdeTab.ENVIRONMENT -> EnvironmentScreen(
                            state = state,
                            onInstallRuntime = viewModel::installRuntime,
                            onInstallToolchain = viewModel::installToolchain,
                            onDebugInfo = {
                                viewModel.showMessage("调试适配器接口已保留；runtime 中安装 debugpy 或 lldb-dap 后即可接入会话。")
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlgoForgeTopBar(state: IdeUiState) {
    TopAppBar(
        title = {
            Column {
                Text(state.project?.name ?: "AlgoForge", fontWeight = FontWeight.SemiBold)
                Text(
                    text = state.sourceFile?.name?.let { "$it · ${state.project?.language?.name.orEmpty()}" }.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            val status = state.runtimeStatus
            Surface(
                color = if (status?.installed == true) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = if (status?.installed == true) "runtime 就绪" else "runtime 未安装",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
    )
}

@Composable
private fun CodeTab(
    state: IdeUiState,
    onSelectFile: (SourceFile) -> Unit,
    onCodeChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.files, key = { it.relativePath }) { file ->
                AssistChip(
                    onClick = { onSelectFile(file) },
                    label = { Text(file.displayName) },
                    leadingIcon = {
                        if (file.relativePath == state.sourceFile?.name) {
                            Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                )
            }
        }
        HorizontalDivider()
        CodeEditor(
            code = state.code,
            language = state.project?.language?.toLanguage() ?: dev.algoforge.core.Language.CPP,
            onCodeChange = onCodeChange,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
