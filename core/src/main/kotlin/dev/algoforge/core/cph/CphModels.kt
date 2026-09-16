package dev.algoforge.core.cph

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CphProblem(
    val name: String = "Untitled problem",
    val group: String = "",
    val url: String = "",
    val interactive: Boolean = false,
    @SerialName("memoryLimit") val memoryLimitMegabytes: Int? = null,
    @SerialName("timeLimit") val timeLimitSeconds: Double? = null,
    val tests: List<CphTest> = emptyList(),
)

@Serializable
data class CphTest(
    val input: String = "",
    val output: String = "",
)

enum class TestVerdict {
    PASSED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILE_ERROR,
    SKIPPED,
}

data class TestReport(
    val index: Int,
    val verdict: TestVerdict,
    val expectedOutput: String,
    val actualOutput: String,
    val standardError: String,
    val exitCode: Int?,
    val durationMillis: Long,
    val message: String = "",
)
