package anc.core

/**
 * Typed module option.
 */
data class Option<T : Any>(
    val name: String,
    val type: Class<T>,
    val description: String,
    val required: Boolean,
    val default: T? = null,
    val choices: List<T> = emptyList()
) {
    @Suppress("UNCHECKED_CAST")
    fun coerce(raw: String): T = when (type) {
        String::class.java                               -> raw as T
        Int::class.java, java.lang.Integer::class.java  -> raw.toInt() as T
        Long::class.java, java.lang.Long::class.java    -> raw.toLong() as T
        Boolean::class.java, java.lang.Boolean::class.java -> raw.toBooleanStrict() as T
        else                                             -> raw as T
    }
}

/**
 * Ordered collection of module options.
 */
class Options {
    private val map = LinkedHashMap<String, Option<*>>()

    fun <T : Any> register(option: Option<T>): Options {
        map[option.name.uppercase()] = option
        return this
    }

    operator fun get(name: String): Option<*>? = map[name.uppercase()]
    fun all(): List<Option<*>> = map.values.toList()

    fun validate(datastore: Datastore): List<String> = map.values
        .filter { it.required && datastore[it.name] == null && it.default == null }
        .map { "${it.name} is required" }
}

// Builder helpers
inline fun <reified T : Any> Options.required(
    name: String,
    description: String,
    default: T? = null,
    choices: List<T> = emptyList()
) = register(Option(name, T::class.java, description, true, default, choices))

inline fun <reified T : Any> Options.optional(
    name: String,
    description: String,
    default: T? = null,
    choices: List<T> = emptyList()
) = register(Option(name, T::class.java, description, false, default, choices))
