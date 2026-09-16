package dev.algoforge.core

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RunPlanFactoryTest {
    @Test
    fun createsTwoPhaseCppPlan() {
        val plan = RunPlanFactory.create(
            language = Language.CPP,
            source = Path.of("/workspace/main.cpp"),
            workspace = Path.of("/workspace"),
            outputDirectory = Path.of("/workspace/.algoforge/out"),
        )

        val compile = assertNotNull(plan.compilePhase)
        assertEquals("/usr/bin/clang++", compile.executable)
        assertEquals("/workspace/.algoforge/out/main", plan.runPhase.executable)
    }

    @Test
    fun pythonHasNoCompilePhase() {
        val plan = RunPlanFactory.create(
            language = Language.PYTHON,
            source = Path.of("/workspace/main.py"),
            workspace = Path.of("/workspace"),
            outputDirectory = Path.of("/workspace/.algoforge/out"),
        )

        assertEquals(null, plan.compilePhase)
        assertEquals("/usr/bin/python3", plan.runPhase.executable)
    }
}
