package rex.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Coroutine-friendly TLS socket wrapper.
 */
class SslSocket private constructor(private val socket: SSLSocket) {
    val isConnected: Boolean get() = socket.isConnected && !socket.isClosed

    suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        socket.outputStream.apply { write(data); flush() }
    }

    suspend fun receive(bufSize: Int = 4096): ByteArray = withContext(Dispatchers.IO) {
        val buf = ByteArray(bufSize)
        val n = socket.inputStream.read(buf)
        if (n < 0) ByteArray(0) else buf.copyOf(n)
    }

    fun close() = runCatching { socket.close() }

    companion object {
        suspend fun connect(
            host: String,
            port: Int,
            timeoutMs: Int = 10_000,
            verifyHostname: Boolean = true
        ): SslSocket = withContext(Dispatchers.IO) {
            val factory = SSLSocketFactory.getDefault()
            val raw = factory.createSocket() as SSLSocket
            raw.connect(InetSocketAddress(host, port), timeoutMs)
            raw.startHandshake()
            SslSocket(raw)
        }
    }
}
