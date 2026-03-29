package anc.core.session.handler

import anc.base.Logging
import anc.core.session.Session
import anc.core.session.ShellSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rex.socket.TcpSocket

/**
 * Connects outbound to a bind shell already listening on the target.
 */
class BindHandler(
    override val lhost: String = "0.0.0.0",
    override val lport: Int = 0,
    val rhost: String,
    val rport: Int
) : Handler {
    override var onSession: ((Session) -> Unit)? = null
    override var isRunning: Boolean = false
        private set

    override suspend fun start() = withContext(Dispatchers.IO) {
        isRunning = true
        Logging.i("Connecting to bind shell at $rhost:$rport")
        val socket = TcpSocket.connect(rhost, rport)
        val session: Session = ShellSession(socket, "$rhost:$rport")
        onSession?.invoke(session)
        isRunning = false
    }

    override fun stop() { isRunning = false }
}
