package dev.algoforge.core.cph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CphCodecTest {
    @Test
    fun roundTripsCphJsonAndIgnoresExtraFields() {
        val source = """
            {
              "name": "A. Sum",
              "url": "https://example.test/a",
              "timeLimit": 1.5,
              "unknownField": true,
              "tests": [{"input": "1 2\n", "output": "3"}]
            }
        """.trimIndent()

        val decoded = CphCodec.decode(source)

        assertEquals("A. Sum", decoded.name)
        assertEquals(1.5, decoded.timeLimitSeconds)
        assertEquals(1, decoded.tests.size)
        assertTrue(CphCodec.encode(decoded).contains("\"input\": \"1 2\\n\""))
    }

    @Test
    fun normalizesTrailingWhitespaceAndFinalNewlines() {
        assertTrue(CphMatcher.matches("a  \r\nb\r\n", "a\nb"))
    }
}
