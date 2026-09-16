package dev.algoforge.core

enum class Language(
    val displayName: String,
    val sourceExtension: String,
    val defaultSourceName: String,
) {
    PYTHON("Python", "py", "main.py"),
    CPP("C++", "cpp", "main.cpp"),
    C("C", "c", "main.c"),
    JAVA("Java", "java", "Main.java"),
    ;

    companion object {
        fun fromFileName(fileName: String): Language? {
            val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            return when (extension) {
                "py", "pyw" -> PYTHON
                "cc", "cpp", "cxx", "c++", "hpp", "hh" -> CPP
                "c", "h" -> C
                "java" -> JAVA
                else -> null
            }
        }
    }
}
