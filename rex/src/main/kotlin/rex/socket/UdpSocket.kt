package rex.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Coroutine-friendly UDP socket wrapper. Uses java.net.DatagramSocket
 * which is available on Android API 26+.
 */
class UdpSocket private constructor(private val socket: DatagramSocket) : AutoCloseable {

    val localPort: Int get() = socket.localPort
    val isClosed: Boolean get() = socket.isClosed

    suspend fun send(data: ByteArray, host: String, port: Int) = withContext(Dispatchers.IO) {
        val addr = InetAddress.getByName(host)
        socket.send(DatagramPacket(data, data.size, addr, port))
    }

    /** Returns the received payload and the sender's address. */
    suspend fun receive(bufSize: Int = 4096): Pair<ByteArray, InetSocketAddress> =
        withContext(Dispatchers.IO) {
            val buf = ByteArray(bufSize)
            val packet = DatagramPacket(buf, buf.size)
            socket.receive(packet)
            packet.data.copyOf(packet.length) to InetSocketAddress(packet.address, packet.port)
        }

    override fun close() = runCatching { socket.close() }.let { Unit }

    companion object {
        /**
         * Open a UDP socket bound to [localPort] (0 = OS-assigned).
         * [timeoutMs] controls how long [receive] blocks before throwing SocketTimeoutException.
         */
        fun open(localPort: Int = 0, timeoutMs: Int = 5_000): UdpSocket {
            val s = DatagramSocket(localPort)
            s.soTimeout = timeoutMs
            return UdpSocket(s)
        }

        /** Fire-and-forget: open a socket, send one datagram, close. */
        suspend fun sendOnce(data: ByteArray, host: String, port: Int) {
            open().use { it.send(data, host, port) }
        }

        /** Send [data] and wait for a single response datagram. */
        suspend fun sendReceive(
            data: ByteArray,
            host: String,
            port: Int,
            timeoutMs: Int = 5_000,
            bufSize: Int = 4096
        ): ByteArray = open(timeoutMs = timeoutMs).use { sock ->
            sock.send(data, host, port)
            sock.receive(bufSize).first
        }
    }
}
