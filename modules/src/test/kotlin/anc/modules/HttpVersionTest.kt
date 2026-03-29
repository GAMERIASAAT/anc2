package anc.modules

import anc.core.Framework
import anc.core.ModuleType
import anc.modules.auxiliary.scanner.HttpVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class HttpVersionTest : FunSpec({

    beforeEach { Framework.reset() }
    afterEach { Framework.reset() }

    // --- Metadata ---

    test("fullName is auxiliary/scanner/http/http_version") {
        val mod = HttpVersion()
        mod.fullName shouldBe "auxiliary/scanner/http/http_version"
    }

    test("moduleType is AUXILIARY") {
        val mod = HttpVersion()
        mod.moduleType shouldBe ModuleType.AUXILIARY
    }

    test("name is HTTP Version Scanner") {
        val mod = HttpVersion()
        mod.name shouldBe "HTTP Version Scanner"
    }

    // --- Options ---

    test("RHOSTS is required") {
        val mod = HttpVersion()
        val opt = mod.options["RHOSTS"]!!
        opt.required shouldBe true
    }

    test("RPORT default is 80") {
        val mod = HttpVersion()
        val opt = mod.options["RPORT"]!!
        opt.default shouldBe 80
    }

    test("SSL default is false") {
        val mod = HttpVersion()
        val opt = mod.options["SSL"]!!
        opt.default shouldBe false
    }

    test("TIMEOUT default is 8000") {
        val mod = HttpVersion()
        val opt = mod.options["TIMEOUT"]!!
        opt.default shouldBe 8000
    }

    test("validation fails without RHOSTS") {
        val mod = HttpVersion()
        val errors = mod.options.validate(mod.datastore)
        errors.any { it.contains("RHOSTS") } shouldBe true
    }

    test("validation passes with RHOSTS set") {
        val mod = HttpVersion()
        mod.datastore["RHOSTS"] = "example.com"
        mod.datastore.applyDefaults(mod.options)
        mod.options.validate(mod.datastore) shouldHaveSize 0
    }

    test("VHOST default is empty string") {
        val mod = HttpVersion()
        val opt = mod.options["VHOST"]!!
        opt.default shouldBe ""
    }
})
