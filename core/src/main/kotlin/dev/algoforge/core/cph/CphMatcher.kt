package dev.algoforge.core.cph

object CphMatcher {
    fun normalize(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lineSequence()
            .map { it.trimEnd() }
            .toList()
            .dropLastWhile { it.isEmpty() }
            .joinToString("\n")
    }

    fun matches(expected: String, actual: String): Boolean = normalize(expected) == normalize(actual)
}
