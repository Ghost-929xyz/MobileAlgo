package dev.algoforge.runtime

import dev.algoforge.core.RunPlan
import dev.algoforge.core.cph.CphMatcher
import dev.algoforge.core.cph.CphTest
import dev.algoforge.core.cph.TestReport
import dev.algoforge.core.cph.TestVerdict

class CphTestExecutor(
    private val executor: RuntimeExecutor,
) {
    suspend fun run(plan: RunPlan, tests: List<CphTest>): List<TestReport> {
        plan.compilePhase?.let { phase ->
            val compile = executor.runPhase(plan, phase, standardInput = "")
            if (compile.timedOut || compile.exitCode != 0) {
                return tests.mapIndexed { index, test ->
                    TestReport(
                        index = index,
                        verdict = TestVerdict.COMPILE_ERROR,
                        expectedOutput = test.output,
                        actualOutput = "",
                        standardError = compile.stderr,
                        exitCode = compile.exitCode,
                        durationMillis = compile.durationMillis,
                        message = "Compilation failed",
                    )
                }
            }
        }

        return tests.mapIndexed { index, test ->
            val result = executor.runPhase(plan, plan.runPhase, test.input)
            val verdict = when {
                result.timedOut -> TestVerdict.TIME_LIMIT_EXCEEDED
                result.exitCode != 0 -> TestVerdict.RUNTIME_ERROR
                CphMatcher.matches(test.output, result.stdout) -> TestVerdict.PASSED
                else -> TestVerdict.WRONG_ANSWER
            }
            TestReport(
                index = index,
                verdict = verdict,
                expectedOutput = test.output,
                actualOutput = result.stdout,
                standardError = result.stderr,
                exitCode = result.exitCode,
                durationMillis = result.durationMillis,
            )
        }
    }
}
