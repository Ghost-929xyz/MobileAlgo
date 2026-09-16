package dev.algoforge.core

import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceProject(
    val schemaVersion: Int = 1,
    val name: String,
    val language: LanguageId,
    val entryFile: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
enum class LanguageId {
    PYTHON,
    CPP,
    C,
    JAVA,
    ;

    fun toLanguage(): Language = when (this) {
        PYTHON -> Language.PYTHON
        CPP -> Language.CPP
        C -> Language.C
        JAVA -> Language.JAVA
    }

    companion object {
        fun from(language: Language): LanguageId = when (language) {
            Language.PYTHON -> PYTHON
            Language.CPP -> CPP
            Language.C -> C
            Language.JAVA -> JAVA
        }
    }
}

data class SourceFile(
    val relativePath: String,
    val displayName: String,
    val language: Language?,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long,
)
