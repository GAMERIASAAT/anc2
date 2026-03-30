package rex.crypto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class Rc4Test : FunSpec({

    // Known test vectors from RFC 6229 / public domain RC4 test suite

    test("encrypt known vector - key='Key' plaintext='Plaintext'") {
        val key       = "Key".toByteArray(Charsets.US_ASCII)
        val plaintext = "Plaintext".toByteArray(Charsets.US_ASCII)
        val expected  = "bbf316e8d940af0ad3"  // well-known RC4 output
        Rc4.crypt(plaintext, key).toHex() shouldBe expected
    }

    test("encrypt known vector - key='Wiki' plaintext='pedia'") {
        val key       = "Wiki".toByteArray(Charsets.US_ASCII)
        val plaintext = "pedia".toByteArray(Charsets.US_ASCII)
        val expected  = "1021bf0420"
        Rc4.crypt(plaintext, key).toHex() shouldBe expected
    }

    test("encrypt then decrypt returns original plaintext") {
        val key       = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val plaintext = "Hello, RC4!".toByteArray()
        val ciphertext = Rc4.crypt(plaintext, key)
        val decrypted  = Rc4.crypt(ciphertext, key)
        decrypted.toString(Charsets.UTF_8) shouldBe "Hello, RC4!"
    }

    test("empty plaintext returns empty ciphertext") {
        val result = Rc4.crypt(ByteArray(0), byteArrayOf(0x01, 0x02))
        result.size shouldBe 0
    }

    test("empty key throws") {
        shouldThrow<IllegalArgumentException> {
            Rc4.crypt(byteArrayOf(0x01), ByteArray(0))
        }
    }

    test("different keys produce different ciphertext") {
        val pt   = "same plaintext".toByteArray()
        val key1 = byteArrayOf(0x01, 0x02, 0x03)
        val key2 = byteArrayOf(0x04, 0x05, 0x06)
        Rc4.crypt(pt, key1).toHex() shouldNotBe Rc4.crypt(pt, key2).toHex()
    }

    test("hex key convenience overload works") {
        val key       = "4B6579"  // hex for "Key"
        val plaintext = "Plaintext".toByteArray(Charsets.US_ASCII)
        val expected  = Rc4.crypt(plaintext, "Key".toByteArray(Charsets.US_ASCII))
        Rc4.crypt(plaintext, key).toList() shouldBe expected.toList()
    }

    test("stateful RC4 maintains keystream across calls") {
        val key = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val plaintext = "Hello World".toByteArray()

        // One-shot
        val oneShot = Rc4.crypt(plaintext, key)

        // Chunked via stateful
        val stateful = Rc4.stateful(key)
        val part1 = stateful.process(plaintext.copyOfRange(0, 5))
        val part2 = stateful.process(plaintext.copyOfRange(5, plaintext.size))
        val chunked = part1 + part2

        chunked.toList() shouldBe oneShot.toList()
    }
})

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
