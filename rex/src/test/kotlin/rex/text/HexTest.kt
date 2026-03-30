package rex.text

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class HexTest : FunSpec({

    // --- encode ---

    test("encode empty array returns empty string") {
        Hex.encode(ByteArray(0)) shouldBe ""
    }

    test("encode single byte") {
        Hex.encode(byteArrayOf(0x00)) shouldBe "00"
        Hex.encode(byteArrayOf(0xFF.toByte())) shouldBe "ff"
        Hex.encode(byteArrayOf(0xAB.toByte())) shouldBe "ab"
    }

    test("encode multiple bytes") {
        Hex.encode(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())) shouldBe "deadbeef"
    }

    test("encode is lowercase") {
        Hex.encode(byteArrayOf(0xAB.toByte(), 0xCD.toByte())) shouldBe "abcd"
    }

    // --- decode ---

    test("decode empty string returns empty array") {
        Hex.decode("").size shouldBe 0
    }

    test("decode two-char hex") {
        Hex.decode("ff")[0] shouldBe 0xFF.toByte()
        Hex.decode("00")[0] shouldBe 0x00.toByte()
    }

    test("decode multi-byte hex") {
        val bytes = Hex.decode("deadbeef")
        bytes[0] shouldBe 0xDE.toByte()
        bytes[1] shouldBe 0xAD.toByte()
        bytes[2] shouldBe 0xBE.toByte()
        bytes[3] shouldBe 0xEF.toByte()
    }

    test("decode handles uppercase") {
        Hex.decode("DEADBEEF")[0] shouldBe 0xDE.toByte()
    }

    test("decode strips spaces and colons") {
        val bytes = Hex.decode("de:ad be:ef")
        bytes[0] shouldBe 0xDE.toByte()
        bytes[3] shouldBe 0xEF.toByte()
    }

    test("decode strips 0x prefix") {
        val bytes = Hex.decode("0xDE0xAD")
        bytes[0] shouldBe 0xDE.toByte()
        bytes[1] shouldBe 0xAD.toByte()
    }

    test("decode throws on odd-length string") {
        shouldThrow<IllegalArgumentException> {
            Hex.decode("abc")
        }
    }

    // --- roundtrip ---

    test("encode then decode roundtrip") {
        val original = byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte())
        val encoded = Hex.encode(original)
        val decoded = Hex.decode(encoded)
        decoded.toList() shouldBe original.toList()
    }

    // --- dump ---

    test("dump empty array returns empty string") {
        Hex.dump(ByteArray(0)) shouldBe ""
    }

    test("dump contains offset") {
        val result = Hex.dump(byteArrayOf(0x41))
        result shouldContain "00000000"
    }

    test("dump shows printable chars as-is") {
        val result = Hex.dump("Hello".toByteArray())
        result shouldContain "Hello"
    }

    test("dump replaces non-printable chars with dot") {
        val result = Hex.dump(byteArrayOf(0x01, 0x41, 0x7f.toByte()))
        result shouldContain "."
        result shouldContain "A"
    }

    test("dump handles 0xFF byte without exception") {
        // 0xFF is non-printable and should map to '.' — test no ArrayIndexOutOfBounds
        Hex.dump(byteArrayOf(0xFF.toByte())) shouldContain "."
    }
})
