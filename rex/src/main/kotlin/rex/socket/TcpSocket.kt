package rex.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Coroutine-friendly TCP socket wrapper. Uses java.net.Socket which is
 * available on Android API 26+.
 */
class TcpSocket private constructor(private val socket: Socket) {
    val inputStream: InputStream get() = socket.getInputStream()
    val outputStream: OutputStream get() = socket.getOutputStream()
    val isConnected: Boolean get() = socket.isConnected && !socket.isClosed
    val remoteAddress: String get() = socket.inetAddress?.hostAddress ?: ""
    val remotePort: Int get() = socket.port

    suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        socket.getOutputStream().apply {
            write(data)
            flush()
        }
    }

    suspend fun receive(bufSize: Int = 4096): ByteArray = withContext(Dispatchers.IO) {
        val buf = ByteArray(bufSize)
        val n = socket.getInputStream().read(buf)
        if (n < 0) ByteArray(0) else buf.copyOf(n)
    }

    suspend fun readLine(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        val stream = socket.getInputStream()
        var b: Int
        while (stream.read().also { b = it } != -1) {
            val c = b.toChar()
            if (c == '\n') break
            if (c != '\r') sb.append(c)
        }
        sb.toString()
    }

    fun close() = runCatching { socket.close() }

    companion object {
        /** Wrap an already-connected [java.net.Socket] in a [TcpSocket]. */
        fun wrap(socket: Socket): TcpSocket = TcpSocket(socket)

        suspend fun connect(host: String, port: Int, timeoutMs: Int = 10_000): TcpSocket =
            withContext(Dispatchers.IO) {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                TcpSocket(socket)
            }

        suspend fun isOpen(host: String, port: Int, timeoutMs: Int = 1_000): Boolean =
            runCatching {
                withContext(Dispatchers.IO) {
                    Socket().use { s ->
                        s.connect(InetSocketAddress(host, port), timeoutMs)
                        true
                    }
                }
            }.getOrDefault(false)
    }
}
