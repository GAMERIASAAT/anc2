package anc.modules

import anc.core.Framework
import anc.core.ModuleType
import anc.modules.auxiliary.scanner.PortScanner
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class PortScannerTest : FunSpec({

    beforeEach { Framework.reset() }
    afterEach { Framework.reset() }

    // Helper to call private methods via reflection
    @Suppress("UNCHECKED_CAST")
    fun PortScanner.parsePorts(raw: String): List<Int> =
        javaClass.getDeclaredMethod("parsePorts", String::class.java)
            .also { it.isAccessible = true }
            .invoke(this, raw) as List<Int>

    @Suppress("UNCHECKED_CAST")
    fun PortScanner.parseHosts(raw: String): List<String> =
        javaClass.getDeclaredMethod("parseHosts", String::class.java)
            .also { it.isAccessible = true }
            .invoke(this, raw) as List<String>

    @Suppress("UNCHECKED_CAST")
    fun PortScanner.expandCidr(cidr: String): List<String> =
        javaClass.getDeclaredMethod("expandCidr", String::class.java)
            .also { it.isAccessible = true }
            .invoke(this, cidr) as List<String>

    // --- Metadata ---

    test("fullName is auxiliary/scanner/portscan/tcp") {
        val mod = PortScanner()
        mod.fullName shouldBe "auxiliary/scanner/portscan/tcp"
    }

    test("moduleType is AUXILIARY") {
        val mod = PortScanner()
        mod.moduleType shouldBe ModuleType.AUXILIARY
    }

    test("name is set") {
        val mod = PortScanner()
        mod.name shouldBe "TCP Port Scanner"
    }

    // --- Options ---

    test("RHOSTS is required") {
        val mod = PortScanner()
        val opt = mod.options["RHOSTS"]!!
        opt.required shouldBe true
    }

    test("PORTS has default value 1-1024") {
        val mod = PortScanner()
        val opt = mod.options["PORTS"]!!
        opt.default shouldBe "1-1024"
    }

    test("THREADS default is 100") {
        val mod = PortScanner()
        val opt = mod.options["THREADS"]!!
        opt.default shouldBe 100
    }

    test("validation fails without RHOSTS") {
        val mod = PortScanner()
        val errors = mod.options.validate(mod.datastore)
        errors.any { it.contains("RHOSTS") } shouldBe true
    }

    test("validation passes with RHOSTS set") {
        val mod = PortScanner()
        mod.datastore["RHOSTS"] = "192.168.1.1"
        mod.datastore.applyDefaults(mod.options)
        mod.options.validate(mod.datastore) shouldHaveSize 0
    }

    // --- Port parsing ---

    test("parsePorts handles single port") {
        val mod = PortScanner()
        mod.parsePorts("80") shouldBe listOf(80)
    }

    test("parsePorts handles comma-separated ports") {
        val mod = PortScanner()
        mod.parsePorts("80,443,8080") shouldContainAll listOf(80, 443, 8080)
    }

    test("parsePorts handles range") {
        val mod = PortScanner()
        val ports = mod.parsePorts("80-85")
        ports shouldBe listOf(80, 81, 82, 83, 84, 85)
    }

    test("parsePorts handles mixed range and singles") {
        val mod = PortScanner()
        val ports = mod.parsePorts("22,80-82,443")
        ports shouldContainAll listOf(22, 80, 81, 82, 443)
    }

    test("parsePorts handles single port range (n-n)") {
        val mod = PortScanner()
        mod.parsePorts("443-443") shouldBe listOf(443)
    }

    // --- Host parsing ---

    test("parseHosts handles single host") {
        val mod = PortScanner()
        mod.parseHosts("192.168.1.1") shouldBe listOf("192.168.1.1")
    }

    test("parseHosts handles comma-separated hosts") {
        val mod = PortScanner()
        val hosts = mod.parseHosts("192.168.1.1,10.0.0.1")
        hosts shouldContainAll listOf("192.168.1.1", "10.0.0.1")
    }

    test("parseHosts handles space-separated hosts") {
        val mod = PortScanner()
        val hosts = mod.parseHosts("192.168.1.1 10.0.0.1")
        hosts shouldContainAll listOf("192.168.1.1", "10.0.0.1")
    }

    // --- CIDR expansion ---

    test("expandCidr /24 produces 254 hosts") {
        val mod = PortScanner()
        val hosts = mod.expandCidr("192.168.1.0/24")
        hosts shouldHaveSize 254
        hosts shouldContain "192.168.1.1"
        hosts shouldContain "192.168.1.254"
    }

    test("expandCidr non-/24 returns base host") {
        val mod = PortScanner()
        val hosts = mod.expandCidr("10.0.0.0/16")
        hosts shouldBe listOf("10.0.0.0")
    }

    test("parseHosts expands CIDR notation") {
        val mod = PortScanner()
        val hosts = mod.parseHosts("192.168.1.0/24")
        hosts shouldHaveSize 254
    }
})
