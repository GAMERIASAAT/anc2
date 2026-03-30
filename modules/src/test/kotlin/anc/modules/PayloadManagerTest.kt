package anc.modules

import anc.core.Framework
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import rex.arch.Platform

class PayloadManagerTest : FunSpec({

    lateinit var fw: Framework

    beforeTest {
        Framework.reset()
        fw = Framework.initialize(ModuleRegistry.all)
    }

    test("payloadManager lists registered payloads") {
        fw.payloadManager.count shouldNotBe 0
    }

    test("create linux/shell_reverse_tcp returns a Payload") {
        val p = fw.payloadManager.create("singles/linux/shell_reverse_tcp")
        p shouldNotBe null
    }

    test("create by short path without prefix works") {
        val p = fw.payloadManager.create("payloads/singles/linux/shell_reverse_tcp")
        p shouldNotBe null
    }

    test("create unknown path returns null") {
        fw.payloadManager.create("singles/nonexistent/payload") shouldBe null
    }

    test("forPlatform LINUX returns at least one payload") {
        fw.payloadManager.forPlatform(Platform.LINUX).isNotEmpty() shouldBe true
    }

    test("forPlatform ANDROID returns at least one payload") {
        fw.payloadManager.forPlatform(Platform.ANDROID).isNotEmpty() shouldBe true
    }
})
