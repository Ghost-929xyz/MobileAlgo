package dev.algoforge.core.debug

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object DapCodec {
    private const val MAX_MESSAGE_BYTES = 8 * 1024 * 1024

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun read(input: InputStream): JsonObject? {
        val headers = linkedMapOf<String, String>()
        while (true) {
            val line = readHeaderLine(input) ?: return null
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator <= 0) continue
            headers[line.substring(0, separator).trim().lowercase()] = line.substring(separator + 1).trim()
        }

        val contentLength = headers["content-length"]?.toIntOrNull()
            ?: throw ProtocolException("DAP response is missing Content-Length")
        if (contentLength !in 0..MAX_MESSAGE_BYTES) {
            throw ProtocolException("Invalid DAP Content-Length: $contentLength")
        }

        val payload = ByteArray(contentLength)
        var offset = 0
        while (offset < payload.size) {
            val count = input.read(payload, offset, payload.size - offset)
            if (count < 0) throw EOFException("Unexpected EOF in DAP payload")
            offset += count
        }
        return json.parseToJsonElement(String(payload, StandardCharsets.UTF_8)) as? JsonObject
            ?: throw ProtocolException("DAP payload must be a JSON object")
    }

    fun write(output: OutputStream, message: JsonObject) {
        val payload = message.toString().toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${payload.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        synchronized(output) {
            output.write(header)
            output.write(payload)
            output.flush()
        }
    }

    fun request(seq: Int, command: String, arguments: JsonObject? = null): JsonObject {
        return buildJsonObject {
            put("seq", seq)
            put("type", "request")
            put("command", command)
            if (arguments != null) put("arguments", arguments)
        }
    }

    fun encode(message: JsonObject): ByteArray {
        val payload = message.toString().toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${payload.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        return header + payload
    }

    private fun readHeaderLine(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        while (true) {
            val next = input.read()
            if (next < 0) {
                if (buffer.size() == 0) return null
                throw EOFException("Unexpected EOF in DAP header")
            }
            if (next == '\n'.code) {
                val bytes = buffer.toByteArray()
                val effectiveLength = if (bytes.lastOrNull()?.toInt() == '\r'.code) bytes.size - 1 else bytes.size
                return String(bytes, 0, effectiveLength, StandardCharsets.US_ASCII)
            }
            if (buffer.size() > 16 * 1024) throw ProtocolException("DAP header line is too long")
            buffer.write(next)
        }
    }
}

class ProtocolException(message: String) : IllegalStateException(message)
