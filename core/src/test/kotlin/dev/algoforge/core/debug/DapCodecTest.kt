package dev.algoforge.core.debug

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DapCodecTest {
    @Test
    fun writesAndReadsContentLengthFrame() {
        val message = buildJsonObject {
            put("seq", 1)
            put("type", "request")
            put("command", "initialize")
        }

        val encoded = DapCodec.encode(message)
        val decoded = DapCodec.read(ByteArrayInputStream(encoded))

        assertNotNull(decoded)
        assertEquals("initialize", decoded["command"]?.toString()?.trim('"'))
    }

    @Test
    fun writesToStreamAndCanReadAgain() {
        val output = ByteArrayOutputStream()
        val message: JsonObject = buildJsonObject {
            put("seq", 7)
            put("type", "event")
            put("event", "stopped")
        }

        DapCodec.write(output, message)
        val decoded = DapCodec.read(ByteArrayInputStream(output.toByteArray()))

        assertEquals("stopped", decoded?.get("event")?.toString()?.trim('"'))
    }
}
