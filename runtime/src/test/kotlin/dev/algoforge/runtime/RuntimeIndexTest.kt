package dev.algoforge.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeIndexTest {
    @Test
    fun resolvesFirstSupportedAbiInDevicePriorityOrder() {
        val index = RuntimeIndex(
            abis = mapOf(
                "arm64-v8a" to runtimeAbi("arm64"),
                "x86_64" to runtimeAbi("x86"),
            ),
        )

        val resolved = index.resolveFor(listOf("mips", "x86_64", "arm64-v8a"))

        assertEquals("x86_64", resolved?.abi)
        assertEquals("x86", resolved?.runtime?.displayName)
    }

    @Test
    fun returnsNullWhenNoArtifactMatches() {
        val index = RuntimeIndex(abis = mapOf("arm64-v8a" to runtimeAbi("arm64")))

        assertNull(index.resolveFor(listOf("x86", "x86_64")))
    }

    private fun runtimeAbi(label: String): RuntimeAbi {
        val artifact = RuntimeArtifact(
            version = "test",
            url = "https://example.test/$label",
            sha256 = "a".repeat(64),
            size = 1,
        )
        return RuntimeAbi(
            displayName = label,
            proot = artifact.copy(archiveFormat = "binary"),
            bootstrap = artifact.copy(archiveFormat = "tarGz"),
        )
    }
}