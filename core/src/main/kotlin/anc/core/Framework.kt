package anc.core

import anc.base.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Central framework object. Holds all managers and the coroutine scope.
 * Use [Framework.getInstance] after calling [Framework.initialize].
 */
class Framework private constructor() {
    val eventBus = EventBus()
    val moduleManager = ModuleManager(this)
    val sessionManager = SessionManager(this)

    /** Background scope for running modules and sessions. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        Logging.i("AncKit initializing...")
    }

    fun loadModules(registry: List<() -> AncModule>) {
        registry.forEach { factory -> moduleManager.register(factory) }
        Logging.i("${moduleManager.count} modules registered.")
        eventBus.tryEmit(EventBus.Event.FrameworkStatus("${moduleManager.count} modules loaded"))
    }

    companion object {
        @Volatile private var _instance: Framework? = null

        fun getInstance(): Framework = _instance
            ?: error("Framework not initialized. Call Framework.initialize() first.")

        fun initialize(moduleRegistry: List<() -> AncModule> = emptyList()): Framework {
            return _instance ?: synchronized(this) {
                _instance ?: Framework().also { fw ->
                    _instance = fw
                    fw.loadModules(moduleRegistry)
                }
            }
        }

        /** For tests — reset the singleton. */
        internal fun reset() { _instance = null }
    }
}
