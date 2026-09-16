package dev.algoforge.ide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.algoforge.core.cph.CphProblem
import dev.algoforge.core.cph.TestReport
import dev.algoforge.core.cph.TestVerdict

@Composable
fun TestsScreen(
    state: IdeUiState,
    onProblemChange: ((CphProblem) -> CphProblem) -> Unit,
    onAddTest: () -> Unit,
    onRemoveTest: (Int) -> Unit,
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
                    Text("CPH 测试集", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.problem.name,
                        onValueChange = { value -> onProblemChange { it.copy(name = value) } },
                        label = { Text("题目名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.problem.url,
                        onValueChange = { value -> onProblemChange { it.copy(url = value) } },
                        label = { Text("题目链接") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.problem.timeLimitSeconds?.toString().orEmpty(),
                            onValueChange = { value ->
                                onProblemChange { it.copy(timeLimitSeconds = value.toDoubleOrNull()) }
                            },
                            label = { Text("时间 / 秒") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.problem.memoryLimitMegabytes?.toString().orEmpty(),
                            onValueChange = { value ->
                                onProblemChange { it.copy(memoryLimitMegabytes = value.toIntOrNull()) }
                            },
                            label = { Text("内存 / MB") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAddTest) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("添加用例")
                        }
                    }
                }
            }
        }

        itemsIndexed(state.problem.tests) { index, test ->
            Card {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("用例 ${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemoveTest(index) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "删除用例")
                        }
                    }
                    OutlinedTextField(
                        value = test.input,
                        onValueChange = { value ->
                            onProblemChange { problem ->
                                problem.copy(tests = problem.tests.mapIndexed { current, item ->
                                    if (current == index) item.copy(input = value) else item
                                })
                            }
                        },
                        label = { Text("stdin") },
                        textStyle = codeTextStyle(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    )
                    OutlinedTextField(
                        value = test.output,
                        onValueChange = { value ->
                            onProblemChange { problem ->
                                problem.copy(tests = problem.tests.mapIndexed { current, item ->
                                    if (current == index) item.copy(output = value) else item
                                })
                            }
                        },
                        label = { Text("期望输出") },
                        textStyle = codeTextStyle(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    )
                }
            }
        }

        if (state.reports.isNotEmpty()) {
            item { Text("测试结果", style = MaterialTheme.typography.titleMedium) }
            items(state.reports, key = { it.index }) { report -> TestReportCard(report) }
        }
    }
}

@Composable
private fun TestReportCard(report: TestReport) {
    val color = when (report.verdict) {
        TestVerdict.PASSED -> MaterialTheme.colorScheme.secondaryContainer
        TestVerdict.WRONG_ANSWER, TestVerdict.RUNTIME_ERROR, TestVerdict.COMPILE_ERROR -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(colors = CardDefaults.cardColors(containerColor = color)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "#${report.index + 1} · ${report.verdict.name} · ${report.durationMillis} ms",
                fontWeight = FontWeight.SemiBold,
            )
            if (report.actualOutput.isNotBlank() && report.verdict != TestVerdict.PASSED) {
                MonoBlock("实际输出", report.actualOutput)
            }
            if (report.standardError.isNotBlank()) {
                MonoBlock("stderr", report.standardError)
            }
        }
    }
}
