package anc.base

import java.util.concurrent.ConcurrentHashMap

/**
 * Framework-wide key/value configuration store.
 * Values can be set by the host app before Framework.initialize().
 */
object Config {
    private val data = ConcurrentHashMap<String, Any>()

    // Default values
    init {
        data["console.prompt"] = "anc > "
        data["console.history_size"] = 500
        data["module.timeout_ms"] = 30_000L
        data["session.keep_alive_interval_ms"] = 15_000L
        data["rpc.port"] = 55553
        data["rpc.host"] = "127.0.0.1"
    }

    operator fun get(key: String): Any? = data[key]
    operator fun set(key: String, value: Any) { data[key] = value }

    fun getString(key: String, default: String = ""): String =
        data[key]?.toString() ?: default

    fun getInt(key: String, default: Int = 0): Int =
        (data[key] as? Number)?.toInt() ?: default

    fun getLong(key: String, default: Long = 0L): Long =
        (data[key] as? Number)?.toLong() ?: default

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        data[key] as? Boolean ?: default
}
