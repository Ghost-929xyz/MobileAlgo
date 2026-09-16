package dev.algoforge.runtime

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RuntimeState(
    val schemaVersion: Int = 1,
    val abi: String,
    val version: String,
    val provider: String,
    val installedAtEpochMillis: Long,
)

object RuntimeStateCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(state: RuntimeState): String = json.encodeToString(RuntimeState.serializer(), state)

    fun decode(text: String): RuntimeState = json.decodeFromString(RuntimeState.serializer(), text)
}
