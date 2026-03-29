package anc.core

import anc.core.session.Session
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SessionManager(private val framework: Framework) {
    private val sessions = ConcurrentHashMap<Int, Session>()
    private val nextId = AtomicInteger(1)

    fun register(session: Session): Int {
        val id = nextId.getAndIncrement()
        session.id = id
        sessions[id] = session
        framework.eventBus.tryEmit(
            EventBus.Event.SessionOpened(id, session.type, session.remoteAddress)
        )
        return id
    }

    fun get(id: Int): Session? = sessions[id]

    fun all(): List<Session> = sessions.values.toList().sortedBy { it.id }

    fun close(id: Int) {
        sessions.remove(id)?.let { session ->
            session.close()
            framework.eventBus.tryEmit(EventBus.Event.SessionClosed(id))
        }
    }

    fun closeAll() = sessions.keys.toList().forEach { close(it) }

    val count: Int get() = sessions.size
}
