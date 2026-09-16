package dev.algoforge.ide.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.algoforge.runtime.RuntimeStatus

@Composable
fun EnvironmentScreen(
    state: IdeUiState,
    onInstallRuntime: () -> Unit,
    onInstallToolchain: () -> Unit,
    onDebugInfo: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("运行时", style = MaterialTheme.typography.titleMedium)
                    RuntimeSummary(state.runtimeStatus)
                    Text(
                        "当前 ABI: ${Build.SUPPORTED_ABIS.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.installProgress != null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(state.installProgress, style = MaterialTheme.typography.labelSmall)
                    }
                    Button(onClick = onInstallRuntime, enabled = !state.busy) {
                        Text(if (state.runtimeStatus?.installed == true) "重新安装 runtime" else "安装 runtime")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("语言工具链", style = MaterialTheme.typography.titleMedium)
                    Text("Python: python + pip")
                    Text("C/C++: clang + lldb + make + cmake")
                    Text("Java: openjdk-17 + ecj")
                    OutlinedButton(onClick = onInstallToolchain, enabled = !state.busy) {
                        Text("安装当前语言工具链")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("调试", style = MaterialTheme.typography.titleMedium)
                    Text("统一面向 DAP；Python 使用 debugpy，C/C++ 使用 lldb-dap。Java 将使用 JDI bridge。")
                    OutlinedButton(onClick = onDebugInfo) {
                        Icon(Icons.Outlined.BugReport, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("查看接入状态")
                    }
                }
            }
        }
        item {
            Text(
                "注意：首发版为侧载 PRoot provider，targetSdk=28。Google Play 上架需要切换到商店兼容 runtime。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RuntimeSummary(status: RuntimeStatus?) {
    if (status?.installed == true) {
        Text("已安装 · ${status.abi} · ${status.version}")
        Text("${status.installedBytes / 1024 / 1024} MiB", style = MaterialTheme.typography.bodySmall)
    } else {
        Text("未安装", color = MaterialTheme.colorScheme.error)
        status?.missingReasons?.forEach { reason -> Text("· $reason", style = MaterialTheme.typography.bodySmall) }
    }
}
