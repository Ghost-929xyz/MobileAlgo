package dev.algoforge.runtime

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RuntimeIndex(
    val schemaVersion: Int = 1,
    val generatedAt: String = "",
    val abis: Map<String, RuntimeAbi> = emptyMap(),
)

@Serializable
data class RuntimeAbi(
    val provider: String = "termux-proot",
    val displayName: String = "",
    val proot: RuntimeArtifact,
    val bootstrap: RuntimeArtifact,
)

data class ResolvedRuntime(
    val abi: String,
    val runtime: RuntimeAbi,
)

@Serializable
data class RuntimeArtifact(
    val version: String,
    val url: String,
    val sha256: String,
    val size: Long = -1,
    val license: String = "",
    @SerialName("archiveFormat") val archiveFormat: String = "tarGz",
    val entry: String? = null,
    val stripComponents: Int = 0,
) {
    fun hasPinnedChecksum(): Boolean = sha256.length == 64 && sha256.all { it in "0123456789abcdefABCDEF" }

    fun isConfigured(): Boolean = url.isNotBlank() && !url.contains("REPLACE_") && hasPinnedChecksum()
}

fun RuntimeIndex.resolveFor(supportedAbis: List<String>): ResolvedRuntime? {
    supportedAbis.forEach { abi ->
        abis[abi]?.let { runtime -> return ResolvedRuntime(abi, runtime) }
    }
    return null
}

class RuntimeIndexLoader(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = true
    }

    fun load(): RuntimeIndex {
        val text = context.assets.open(INDEX_ASSET).bufferedReader().use { it.readText() }
        return json.decodeFromString(RuntimeIndex.serializer(), text)
    }

    companion object {
        const val INDEX_ASSET = "runtime-index.json"
    }
}
