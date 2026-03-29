package anc.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Framework-wide event bus backed by Kotlin Flow.
 * Consumers collect from [events]; producers call [emit].
 */
class EventBus {
    sealed class Event {
        data class ModuleLoaded(val fullName: String) : Event()
        data class ModuleStarted(val fullName: String) : Event()
        data class ModuleOutput(val fullName: String, val line: String) : Event()
        data class ModuleFinished(val fullName: String, val success: Boolean) : Event()
        data class SessionOpened(val sessionId: Int, val type: String, val remote: String) : Event()
        data class SessionClosed(val sessionId: Int) : Event()
        data class ConsoleOutput(val line: String, val isError: Boolean = false) : Event()
        data class FrameworkStatus(val message: String) : Event()
    }

    private val _events = MutableSharedFlow<Event>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun emit(event: Event) = _events.emit(event)

    /** Non-suspending fire-and-forget (drops if buffer full). */
    fun tryEmit(event: Event) { _events.tryEmit(event) }
}
