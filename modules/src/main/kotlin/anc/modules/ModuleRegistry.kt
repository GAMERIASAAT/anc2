package anc.modules

import anc.core.AncModule
import anc.modules.auxiliary.scanner.HttpVersion
import anc.modules.auxiliary.scanner.PortScanner
import anc.modules.exploits.multi.handler.GenericPayloadHandler

/**
 * Compile-time module registry.
 * Add every module class here so the Framework can discover them on both JVM and Android.
 */
object ModuleRegistry {
    val all: List<() -> AncModule> = listOf(
        { PortScanner() },
        { HttpVersion() },
        { GenericPayloadHandler() }
    )
}
