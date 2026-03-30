package anc.modules.nops

import anc.modules.nops.arm.NopArm
import anc.modules.nops.x86.NopX86
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NopTest : FunSpec({

    // ── x86 ───────────────────────────────────────────────────────────────────

    test("NopX86 generates correct length") {
        NopX86().generate(16).size shouldBe 16
    }

    test("NopX86 default is 0x90") {
        val sled = NopX86().generate(8)
        sled.all { it == 0x90.toByte() } shouldBe true
    }

    test("NopX86 avoids bad char 0x90 and uses alternative") {
        val bad  = byteArrayOf(0x90.toByte())
        val sled = NopX86().generate(8, bad)
        sled.any { it == 0x90.toByte() } shouldBe false
        sled.size shouldBe 8
    }

    test("NopX86 throws when all candidates excluded") {
        val nop = NopX86()
        // Remove all possible bytes (256 bytes)
        val allBytes = ByteArray(256) { it.toByte() }
        shouldThrow<IllegalArgumentException> { nop.generate(1, allBytes) }
    }

    test("NopX86 polymorphic sled uses multiple distinct bytes") {
        val sled = NopX86().generatePolymorphic(20)
        sled.size shouldBe 20
        sled.toSet().size shouldNotBe 1  // not all the same byte
    }

    test("NopX86 zero length returns empty array") {
        NopX86().generate(0).size shouldBe 0
    }

    // ── ARM ───────────────────────────────────────────────────────────────────

    test("NopArm Thumb generates correct length") {
        NopArm().generateThumb(8).size shouldBe 8
    }

    test("NopArm Thumb sled is bf 00 repeated") {
        val sled = NopArm().generateThumb(4)
        sled[0] shouldBe 0x00.toByte()
        sled[1] shouldBe 0xbf.toByte()
        sled[2] shouldBe 0x00.toByte()
        sled[3] shouldBe 0xbf.toByte()
    }

    test("NopArm ARM mode generates correct length") {
        NopArm().generateArm(8).size shouldBe 8
    }

    test("NopArm ARM sled is e320f000 repeated (little-endian)") {
        val sled = NopArm().generateArm(4)
        sled[0] shouldBe 0x00.toByte()
        sled[1] shouldBe 0xf0.toByte()
        sled[2] shouldBe 0x20.toByte()
        sled[3] shouldBe 0xe3.toByte()
    }

    test("NopArm Thumb throws on non-multiple-of-2 length") {
        shouldThrow<IllegalArgumentException> { NopArm().generateThumb(3) }
    }

    test("NopArm ARM throws on non-multiple-of-4 length") {
        shouldThrow<IllegalArgumentException> { NopArm().generateArm(6) }
    }

    test("NopArm throws when pattern contains bad chars") {
        val bad = byteArrayOf(0x00.toByte())  // 0x00 is in both ARM and Thumb NOPs
        shouldThrow<IllegalArgumentException> { NopArm().generateThumb(4, bad) }
    }
})
