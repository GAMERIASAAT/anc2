package rex.crypto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class Sha256Test : FunSpec({

    // FIPS 180-4 / RFC 4231 known-good test vectors

    test("SHA-256 of empty string") {
        Sha256.hex("") shouldBe
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }

    test("SHA-256 of 'abc'") {
        Sha256.hex("abc") shouldBe
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }

    test("digest returns exactly 32 bytes") {
        Sha256.digest("anything".toByteArray()).size shouldBe 32
    }

    test("hex string is 64 lowercase hex chars") {
        val h = Sha256.hex("test")
        h.length shouldBe 64
        h shouldBe h.lowercase()
    }

    test("same input always produces same digest (determinism)") {
        Sha256.hex("deterministic") shouldBe Sha256.hex("deterministic")
    }

    test("different inputs produce different digests") {
        Sha256.hex("input1") shouldNotBe Sha256.hex("input2")
    }

    test("byte-array overload matches string overload") {
        val s = "hello"
        Sha256.hex(s) shouldBe Sha256.hex(s.toByteArray(Charsets.UTF_8))
    }

    test("HMAC-SHA-256 RFC 4231 test case 1") {
        val key  = ByteArray(20) { 0x0b }
        val data = "Hi There".toByteArray(Charsets.US_ASCII)
        Sha256.hmacHex(data, key) shouldBe
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7"
    }

    test("HMAC output is 32 bytes") {
        val mac = Sha256.hmac("data".toByteArray(), "key".toByteArray())
        mac.size shouldBe 32
    }

    test("HMAC is key-dependent") {
        val data = "message".toByteArray()
        Sha256.hmacHex(data, "key1".toByteArray()) shouldNotBe
            Sha256.hmacHex(data, "key2".toByteArray())
    }

    test("verify returns true for identical digests") {
        val a = Sha256.digest("same".toByteArray())
        val b = Sha256.digest("same".toByteArray())
        Sha256.verify(a, b) shouldBe true
    }

    test("verify returns false for different digests") {
        val a = Sha256.digest("aaa".toByteArray())
        val b = Sha256.digest("bbb".toByteArray())
        Sha256.verify(a, b) shouldBe false
    }

    test("verify returns false for different lengths") {
        Sha256.verify(byteArrayOf(0x01), byteArrayOf(0x01, 0x02)) shouldBe false
    }
})
