package anc.modules

import anc.core.AncModule
import anc.modules.auxiliary.scanner.HttpVersion
import anc.modules.auxiliary.scanner.PortScanner
import anc.modules.encoders.x86.XorAdditive
import anc.modules.exploits.multi.handler.GenericPayloadHandler
import anc.modules.nops.arm.NopArm
import anc.modules.nops.x86.NopX86
import anc.modules.payloads.singles.android.ReverseShell
import anc.modules.payloads.singles.linux.ReverseShellX86

/**
 * Compile-time module registry.
 * Add every module class here so the Framework can discover them on both JVM and Android.
 */
object ModuleRegistry {
    val all: List<() -> AncModule> = listOf(
        // Auxiliary
        { PortScanner() },
        { HttpVersion() },
        // Exploits
        { GenericPayloadHandler() },
        // Payloads
        { ReverseShellX86() },
        { ReverseShell() },
        // Encoders
        { XorAdditive() },
        // NOPs
        { NopX86() },
        { NopArm() }
    )
}
