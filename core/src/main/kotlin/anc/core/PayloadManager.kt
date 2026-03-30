package anc.core

import anc.core.payload.Payload
import rex.arch.Arch
import rex.arch.Platform

/**
 * Specialized view over [ModuleManager] for payload modules.
 * Provides typed lookup and filtering by platform / arch.
 */
class PayloadManager(private val framework: Framework) {

    /** All registered payload modules as metadata. */
    fun all(): List<ModuleMetadata> =
        framework.moduleManager.byType(ModuleType.PAYLOAD)

    /**
     * Instantiate a payload by its full path (e.g. "payloads/singles/linux/shell_reverse_tcp").
     * Also accepts the short form without the "payloads/" prefix.
     */
    fun create(path: String): Payload? {
        val normalised = if (path.startsWith("payloads/")) path else "payloads/$path"
        return framework.moduleManager.create(normalised) as? Payload
    }

    /** Filter payloads by target platform. */
    fun forPlatform(platform: Platform): List<ModuleMetadata> =
        all().filter { meta ->
            val mod = framework.moduleManager.create(meta.fullName) as? Payload
            mod?.platform?.contains(platform) == true || mod?.platform.isNullOrEmpty()
        }

    /** Filter payloads by architecture. */
    fun forArch(arch: Arch): List<ModuleMetadata> =
        all().filter { meta ->
            val mod = framework.moduleManager.create(meta.fullName) as? Payload
            mod?.arch?.contains(arch) == true || mod?.arch.isNullOrEmpty()
        }

    /** Filter by both platform and arch. */
    fun forPlatformAndArch(platform: Platform, arch: Arch): List<ModuleMetadata> =
        forPlatform(platform).filter { meta ->
            val mod = framework.moduleManager.create(meta.fullName) as? Payload
            mod?.arch?.contains(arch) == true || mod?.arch.isNullOrEmpty()
        }

    val count: Int get() = all().size
}
