package dev.algoforge.ide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConsoleScreen(
    state: IdeUiState,
    onStdinChange: (String) -> Unit,
    onRun: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("标准输入", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.stdin,
                        onValueChange = onStdinChange,
                        textStyle = codeTextStyle(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        placeholder = { Text("运行前输入测试数据") },
                    )
                    Button(onClick = onRun, enabled = !state.busy) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.busy) "运行中" else "运行")
                    }
                }
            }
        }
        item {
            val exitText = state.exitCode?.let { "，退出码 $it" }.orEmpty()
            Text("输出 · ${state.durationMillis} ms$exitText", style = MaterialTheme.typography.titleMedium)
        }
        if (state.output.isNotBlank()) item { MonoBlock("stdout", state.output) }
        if (state.standardError.isNotBlank()) item { MonoBlock("stderr", state.standardError) }
        if (state.output.isBlank() && state.standardError.isBlank() && !state.busy) {
            item { Text("还没有输出。安装 runtime 后点击运行。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
