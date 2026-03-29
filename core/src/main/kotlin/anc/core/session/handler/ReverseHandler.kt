package anc.core.session.handler

import anc.base.Logging
import anc.core.session.Session
import anc.core.session.ShellSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ServerSocket

/**
 * Listens for reverse connections (agent calls back to us).
 * Uses java.net.ServerSocket which is available on Android API 26+.
 */
class ReverseHandler(
    override val lhost: String,
    override val lport: Int,
    private val sessionType: SessionType = SessionType.SHELL
) : Handler {
    enum class SessionType { SHELL, PHANTOM }

    override var onSession: ((Session) -> Unit)? = null
    override var isRunning: Boolean = false
        private set

    private var serverSocket: ServerSocket? = null

    override suspend fun start() = withContext(Dispatchers.IO) {
        serverSocket = ServerSocket(lport).also { ss ->
            ss.reuseAddress = true
            isRunning = true
            Logging.i("Reverse handler listening on $lhost:$lport")
            try {
                while (isRunning) {
                    val client = ss.accept()
                    val remote = "${client.inetAddress.hostAddress}:${client.port}"
                    Logging.i("Incoming connection from $remote")
                    val tcpSocket = rex.socket.TcpSocket.wrap(client)
                    val session: Session = when (sessionType) {
                        SessionType.SHELL   -> ShellSession(tcpSocket, remote)
                        SessionType.PHANTOM -> anc.core.session.PhantomSession(tcpSocket, remote)
                    }
                    onSession?.invoke(session)
                }
            } catch (_: Exception) { /* server closed */ }
        }
    }

    override fun stop() {
        isRunning = false
        runCatching { serverSocket?.close() }
    }
}
