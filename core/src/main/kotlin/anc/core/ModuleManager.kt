package anc.core

import anc.base.Logging
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

data class ModuleMetadata(
    val fullName: String,
    val name: String,
    val moduleType: ModuleType,
    val description: String,
    val rank: Rank,
    val authors: List<String>,
    val platforms: List<String>,
    val references: List<String>
)

class ModuleManager(private val framework: Framework) {
    private val registry = ConcurrentHashMap<String, () -> AncModule>()
    private val metaCache = ConcurrentHashMap<String, ModuleMetadata>()

    val count: Int get() = registry.size

    /** Register a module factory. Called by [ModuleRegistry] at startup. */
    fun register(factory: () -> AncModule) {
        val instance = factory()
        val meta = ModuleMetadata(
            fullName = instance.fullName,
            name = instance.name,
            moduleType = instance.moduleType,
            description = instance.description,
            rank = instance.rank,
            authors = instance.authors,
            platforms = instance.platform.map { it.label },
            references = instance.references.map { it.display }
        )
        registry[instance.fullName] = factory
        metaCache[instance.fullName] = meta
        Logging.v("Registered module: ${instance.fullName}")
    }

    fun loadBuiltinModules() {
        // ServiceLoader-based discovery for JVM; override on Android with compiled registry
        try {
            ServiceLoader.load(AncModule::class.java).forEach { module ->
                register { module }
            }
        } catch (e: Exception) {
            Logging.d("ServiceLoader discovery skipped: ${e.message}")
        }
    }

    /** Create a fresh instance of a module by its full name. */
    fun create(fullName: String): AncModule? {
        val factory = registry[fullName] ?: return null
        return factory().also { it.framework = framework }
    }

    fun allMeta(): List<ModuleMetadata> = metaCache.values.toList()
        .sortedBy { it.fullName }

    fun search(query: String): List<ModuleMetadata> {
        val q = query.lowercase()
        return allMeta().filter { meta ->
            meta.fullName.contains(q) ||
            meta.description.lowercase().contains(q) ||
            meta.authors.any { it.lowercase().contains(q) }
        }
    }

    fun byType(type: ModuleType): List<ModuleMetadata> =
        allMeta().filter { it.moduleType == type }

    fun meta(fullName: String): ModuleMetadata? = metaCache[fullName]
}
