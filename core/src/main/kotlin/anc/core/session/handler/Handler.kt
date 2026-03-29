package anc.core.session.handler

import anc.core.session.Session

/**
 * A handler listens for incoming agent connections and wraps them in a [Session].
 */
interface Handler {
    val lhost: String
    val lport: Int
    val isRunning: Boolean

    suspend fun start()
    fun stop()

    /** Called each time a new session is established. */
    var onSession: ((Session) -> Unit)?
}
