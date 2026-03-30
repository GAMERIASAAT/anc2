package anc.modules

import anc.core.Framework
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking

class EncoderManagerTest : FunSpec({

    lateinit var fw: Framework

    beforeTest {
        Framework.reset()
        fw = Framework.initialize(ModuleRegistry.all)
    }

    test("encoderManager lists registered encoders") {
        fw.encoderManager.count shouldNotBe 0
    }

    test("create x86/xor_additive returns an Encoder") {
        fw.encoderManager.create("x86/xor_additive") shouldNotBe null
    }

    test("create unknown encoder returns null") {
        fw.encoderManager.create("x86/nonexistent") shouldBe null
    }

    test("encode with unknown path returns original bytes") {
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val result = runBlocking { fw.encoderManager.encode(data, "x86/nonexistent") }
        result.toList() shouldBe data.toList()
    }

    test("encode with xor_additive returns different bytes") {
        val data = byteArrayOf(0x41, 0x42, 0x43, 0x44)
        val result = runBlocking { fw.encoderManager.encode(data, "x86/xor_additive") }
        result.toList() shouldNotBe data.toList()
    }

    test("encodeChain with empty list returns original bytes") {
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val result = runBlocking { fw.encoderManager.encodeChain(data, emptyList()) }
        result.toList() shouldBe data.toList()
    }

    test("encoded output includes decoder stub prefix") {
        val data = byteArrayOf(0x90.toByte(), 0x90.toByte(), 0x90.toByte(), 0x90.toByte())
        val result = runBlocking { fw.encoderManager.encode(data, "x86/xor_additive") }
        // Output = stub + encoded payload, so must be larger than input
        result.size shouldNotBe 0
        (result.size > data.size) shouldBe true
    }
})
