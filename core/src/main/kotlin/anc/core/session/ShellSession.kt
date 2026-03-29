package anc.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import rex.socket.TcpSocket
import java.io.InputStream
import java.io.OutputStream

/**
 * A plain interactive shell session over a TCP socket.
 */
class ShellSession(
    private val socket: TcpSocket,
    override val remoteAddress: String
) : Session() {
    override val type = "shell"
    override val isAlive: Boolean get() = socket.isConnected

    private val _output = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 256)
    override val output: SharedFlow<String> = _output.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inputStream: InputStream get() = socket.inputStream
    private val outputStream: OutputStream get() = socket.outputStream

    init {
        // Continuously read from remote and emit lines
        scope.launch {
            val buf = StringBuilder()
            try {
                val bytes = ByteArray(1024)
                while (socket.isConnected) {
                    val n = inputStream.read(bytes)
                    if (n < 0) break
                    buf.append(String(bytes, 0, n))
                    // emit complete lines
                    while (buf.contains('\n')) {
                        val idx = buf.indexOf('\n')
                        val line = buf.substring(0, idx).trimEnd('\r')
                        _output.emit(line)
                        buf.delete(0, idx + 1)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun send(command: String) {
        outputStream.write((command + "\n").toByteArray())
        outputStream.flush()
    }

    override suspend fun execute(command: String, timeoutMs: Long): String {
        val sb = StringBuilder()
        val job = scope.launch {
            output.collect { line -> sb.appendLine(line) }
        }
        send(command)
        withTimeoutOrNull(timeoutMs) {
            kotlinx.coroutines.delay(timeoutMs)
        }
        job.cancel()
        return sb.toString().trimEnd()
    }

    override fun close() {
        scope.launch { /* flush */ }
        socket.close()
    }
}
