package rex.proto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain

class SmbTest : FunSpec({

    test("buildNegotiateRequest has NetBIOS session header (type 0x00)") {
        val packet = Smb.buildNegotiateRequest()
        packet[0] shouldBe 0x00.toByte()   // NetBIOS session message
    }

    test("buildNegotiateRequest contains SMB magic bytes after NetBIOS header") {
        val packet = Smb.buildNegotiateRequest()
        // SMB magic is at offset 4 (after 4-byte NetBIOS header)
        packet[4] shouldBe 0xFF.toByte()
        packet[5] shouldBe 'S'.code.toByte()
        packet[6] shouldBe 'M'.code.toByte()
        packet[7] shouldBe 'B'.code.toByte()
    }

    test("buildNegotiateRequest SMB command byte is 0x72 (Negotiate)") {
        val packet = Smb.buildNegotiateRequest()
        packet[8] shouldBe 0x72.toByte()
    }

    test("buildNegotiateRequest is non-empty") {
        Smb.buildNegotiateRequest().size shouldNotBe 0
    }

    test("buildNegotiateRequest contains NT LM 0.12 dialect bytes") {
        val packet = Smb.buildNegotiateRequest()
        val str = String(packet, Charsets.US_ASCII)
        str shouldNotBe ""
        // NT LM 0.12 is the SMBv1 indicator
        packet.toList().windowed("NT LM 0.12".length) { window ->
            String(window.toByteArray(), Charsets.US_ASCII)
        } shouldContain "NT LM 0.12"
    }

    test("negotiate to closed port returns null gracefully") {
        // Port 44500 should be closed on localhost; negotiate should return null, not throw
        val result = Smb.negotiate("127.0.0.1", port = 44500, timeoutMs = 500)
        result shouldBe null
    }

    test("isOpen returns false for closed port") {
        Smb.isOpen("127.0.0.1", timeoutMs = 500) shouldBe false
    }
})
