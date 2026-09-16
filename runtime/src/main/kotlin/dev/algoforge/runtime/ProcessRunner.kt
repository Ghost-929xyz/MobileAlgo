package dev.algoforge.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.Reader
import java.util.concurrent.TimeUnit

sealed interface ProcessEvent {
    data class Started(val command: List<String>) : ProcessEvent
    data class Stdout(val text: String) : ProcessEvent
    data class Stderr(val text: String) : ProcessEvent
    data class Exited(val exitCode: Int, val durationMillis: Long) : ProcessEvent
    data class TimedOut(val timeoutMillis: Long) : ProcessEvent
    data class Failed(val message: String, val cause: Throwable? = null) : ProcessEvent
}

data class ProcessResult(
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
    val durationMillis: Long,
    val timedOut: Boolean,
)

class ProcessRunner(
    private val outputLimitBytes: Int = 2 * 1024 * 1024,
) {
    fun execute(
        command: List<String>,
        workingDirectory: File,
        environment: Map<String, String> = emptyMap(),
        standardInput: String = "",
        timeoutMillis: Long,
    ): Flow<ProcessEvent> = channelFlow {
        if (!workingDirectory.isDirectory) {
            send(ProcessEvent.Failed("Working directory does not exist: ${workingDirectory.absolutePath}"))
            return@channelFlow
        }

        send(ProcessEvent.Started(command))
        val startedAt = System.nanoTime()
        var running: Process? = null
        try {
            val builder = ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(false)
            builder.environment().putAll(environment)
            val process = withContext(Dispatchers.IO) { builder.start() }
            running = process

            val stdout = BoundedOutput(outputLimitBytes)
            val stderr = BoundedOutput(outputLimitBytes)
            val stdoutJob = launch(Dispatchers.IO) {
                read(process.inputStream.reader(Charsets.UTF_8)) { chunk ->
                    if (stdout.append(chunk)) send(ProcessEvent.Stdout(chunk))
                }
            }
            val stderrJob = launch(Dispatchers.IO) {
                read(process.errorStream.reader(Charsets.UTF_8)) { chunk ->
                    if (stderr.append(chunk)) send(ProcessEvent.Stderr(chunk))
                }
            }

            withContext(Dispatchers.IO) {
                writeStandardInput(process, standardInput)
            }

            val finished = waitForExit(process, timeoutMillis)
            val duration = (System.nanoTime() - startedAt) / 1_000_000
            if (!finished) {
                terminate(process)
                send(ProcessEvent.TimedOut(timeoutMillis))
            }

            stdoutJob.join()
            stderrJob.join()
            if (finished) send(ProcessEvent.Exited(process.exitValue(), duration))
        } catch (cancelled: CancellationException) {
            running?.destroyForcibly()
            throw cancelled
        } catch (error: Throwable) {
            running?.destroyForcibly()
            send(ProcessEvent.Failed(error.message ?: error::class.java.simpleName, error))
        } finally {
            running?.takeIf { it.isAlive }?.destroyForcibly()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun capture(
        command: List<String>,
        workingDirectory: File,
        environment: Map<String, String> = emptyMap(),
        standardInput: String = "",
        timeoutMillis: Long,
    ): ProcessResult = withContext(Dispatchers.IO) {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        var exitCode: Int? = null
        var duration = 0L
        var timedOut = false
        execute(command, workingDirectory, environment, standardInput, timeoutMillis).collect { event ->
            when (event) {
                is ProcessEvent.Stdout -> stdout.write(event.text.toByteArray(Charsets.UTF_8))
                is ProcessEvent.Stderr -> stderr.write(event.text.toByteArray(Charsets.UTF_8))
                is ProcessEvent.Exited -> {
                    exitCode = event.exitCode
                    duration = event.durationMillis
                }
                is ProcessEvent.TimedOut -> {
                    timedOut = true
                    duration = event.timeoutMillis
                }
                else -> Unit
            }
        }
        ProcessResult(
            exitCode = exitCode,
            stdout = stdout.toString(Charsets.UTF_8.name()),
            stderr = stderr.toString(Charsets.UTF_8.name()),
            durationMillis = duration,
            timedOut = timedOut,
        )
    }

    private suspend fun waitForExit(process: Process, timeoutMillis: Long): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis.coerceAtLeast(0))
        while (process.isAlive) {
            if (System.nanoTime() >= deadline) return false
            delay(POLL_INTERVAL_MILLIS)
        }
        return true
    }

    private suspend fun terminate(process: Process) {
        process.destroy()
        withContext(Dispatchers.IO + NonCancellable) {
            if (!process.waitFor(TERMINATION_GRACE_MILLIS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                process.waitFor(TERMINATION_GRACE_MILLIS, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun writeStandardInput(process: Process, standardInput: String) {
        try {
            if (standardInput.isNotEmpty()) {
                process.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(standardInput)
                }
            } else {
                process.outputStream.close()
            }
        } catch (error: IOException) {
            // A program is allowed to exit without consuming stdin. Broken pipe is not a failure.
            if (process.isAlive) throw error
        }
    }

    private suspend fun read(reader: Reader, onChunk: suspend (String) -> Unit) {
        try {
            val chars = CharArray(4096)
            while (true) {
                val count = reader.read(chars)
                if (count < 0) break
                if (count > 0) onChunk(String(chars, 0, count))
            }
        } finally {
            reader.close()
        }
    }

    private class BoundedOutput(private val limit: Int) {
        private var size = 0

        fun append(chunk: String): Boolean {
            val next = chunk.toByteArray(Charsets.UTF_8).size
            if (size + next > limit) return false
            size += next
            return true
        }
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 25L
        const val TERMINATION_GRACE_MILLIS = 500L
    }
}