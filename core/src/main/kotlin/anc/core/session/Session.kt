package anc.core.session

import kotlinx.coroutines.flow.SharedFlow

/**
 * Abstract session — an interactive channel to a target.
 */
abstract class Session {
    var id: Int = -1
    abstract val type: String
    abstract val remoteAddress: String
    abstract val isAlive: Boolean

    /** Stream of output lines from the remote end. */
    abstract val output: SharedFlow<String>

    /** Send a command/data to the remote end. */
    abstract suspend fun send(command: String)

    /** Run a single command and return its output (blocks until prompt returns). */
    abstract suspend fun execute(command: String, timeoutMs: Long = 10_000): String

    abstract fun close()

    override fun toString() = "Session #$id [$type] $remoteAddress"
}
