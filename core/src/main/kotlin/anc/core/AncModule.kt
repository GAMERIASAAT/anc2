package anc.core

import rex.arch.Arch
import rex.arch.Platform

enum class ModuleType(val path: String) {
    EXPLOIT("exploits"),
    AUXILIARY("auxiliary"),
    POST("post"),
    PAYLOAD("payloads"),
    ENCODER("encoders"),
    NOP("nops"),
    EVASION("evasion")
}

/**
 * Abstract base for every AncKit module.
 * Subclasses must implement [name], [description], [moduleType], and [run].
 */
abstract class AncModule {
    abstract val name: String
    abstract val description: String
    abstract val moduleType: ModuleType

    open val authors: List<String> = emptyList()
    open val references: List<Reference> = emptyList()
    open val rank: Rank = Rank.NORMAL
    open val platform: List<Platform> = emptyList()
    open val arch: List<Arch> = emptyList()

    val datastore = Datastore()
    open val options = Options()

    /** Dot-separated path under the module type root, e.g. "scanner/portscan/tcp" */
    abstract val modulePath: String

    val fullName: String get() = "${moduleType.path}/$modulePath"

    lateinit var framework: Framework
        internal set

    /** Entry point — always called on a background dispatcher. */
    abstract suspend fun run()

    open fun check(): CheckResult = CheckResult.Unknown

    sealed class CheckResult {
        object Safe : CheckResult()
        object Vulnerable : CheckResult()
        object Unknown : CheckResult()
        data class Error(val message: String) : CheckResult()
    }

    protected fun print(message: String) {
        framework.eventBus.tryEmit(EventBus.Event.ModuleOutput(fullName, message))
    }

    protected fun printGood(message: String) = print("[+] $message")
    protected fun printBad(message: String) = print("[-] $message")
    protected fun printStatus(message: String) = print("[*] $message")
    protected fun printWarning(message: String) = print("[!] $message")

    override fun toString() = fullName
}
