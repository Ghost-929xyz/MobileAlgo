package dev.algoforge.core.cph

import kotlinx.serialization.json.Json

object CphCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
    }

    fun decode(text: String): CphProblem = json.decodeFromString(CphProblem.serializer(), text)

    fun encode(problem: CphProblem): String = json.encodeToString(CphProblem.serializer(), problem)
}
