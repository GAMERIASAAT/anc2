package rex.proto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch

class DnsTest : FunSpec({

    test("resolve localhost returns 127.0.0.1") {
        val ip = Dns.resolve("localhost")
        ip shouldNotBe null
        ip shouldBe "127.0.0.1"
    }

    test("resolveAll localhost returns at least one address") {
        val ips = Dns.resolveAll("localhost")
        ips.size shouldNotBe 0
    }

    test("isResolvable returns true for localhost") {
        Dns.isResolvable("localhost") shouldBe true
    }

    test("isResolvable returns false for invalid hostname") {
        Dns.isResolvable("this-host-definitely-does-not-exist.invalid") shouldBe false
    }

    test("resolve returns null for invalid hostname") {
        Dns.resolve("this-host-definitely-does-not-exist.invalid") shouldBe null
    }

    test("isPrivate returns true for loopback address") {
        Dns.isPrivate("127.0.0.1") shouldBe true
    }

    test("isPrivate returns true for RFC-1918 address") {
        Dns.isPrivate("192.168.1.1") shouldBe true
        Dns.isPrivate("10.0.0.1")    shouldBe true
        Dns.isPrivate("172.16.0.1")  shouldBe true
    }

    test("isPrivate returns false for public address") {
        Dns.isPrivate("8.8.8.8") shouldBe false
    }

    test("ptrName builds correct in-addr.arpa name") {
        Dns.ptrName("1.2.3.4") shouldBe "4.3.2.1.in-addr.arpa"
    }

    test("ptrName for Google DNS") {
        Dns.ptrName("8.8.8.8") shouldBe "8.8.8.8.in-addr.arpa"
    }
})
