package anc.core

import java.util.concurrent.ConcurrentHashMap

/**
 * Key/value store holding per-module option values.
 * Keys are case-insensitive (stored uppercase).
 */
class Datastore {
    private val data = ConcurrentHashMap<String, Any>()

    operator fun get(key: String): Any? = data[key.uppercase()]

    operator fun set(key: String, value: Any) {
        data[key.uppercase()] = value
    }

    fun getString(key: String, default: String = ""): String =
        data[key.uppercase()]?.toString() ?: default

    fun getInt(key: String, default: Int = 0): Int =
        (data[key.uppercase()] as? Number)?.toInt()
            ?: data[key.uppercase()]?.toString()?.toIntOrNull()
            ?: default

    fun getLong(key: String, default: Long = 0L): Long =
        (data[key.uppercase()] as? Number)?.toLong()
            ?: data[key.uppercase()]?.toString()?.toLongOrNull()
            ?: default

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        data[key.uppercase()] as? Boolean
            ?: data[key.uppercase()]?.toString()?.toBooleanStrictOrNull()
            ?: default

    fun contains(key: String): Boolean = data.containsKey(key.uppercase())

    fun toMap(): Map<String, Any> = data.toMap()

    /** Apply option defaults from an Options descriptor if not already set. */
    fun applyDefaults(options: Options) {
        options.all().forEach { opt ->
            if (!contains(opt.name) && opt.default != null) {
                set(opt.name, opt.default)
            }
        }
    }

    override fun toString(): String = data.entries
        .sortedBy { it.key }
        .joinToString("\n") { (k, v) -> "  %-20s => %s".format(k, v) }
}
