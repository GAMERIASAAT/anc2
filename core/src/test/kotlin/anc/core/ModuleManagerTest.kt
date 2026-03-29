package anc.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import rex.arch.Platform

class ModuleManagerTest : FunSpec({

    beforeEach { Framework.reset() }
    afterEach { Framework.reset() }

    fun makeFramework() = Framework.initialize()

    fun fakeModule(
        n: String = "Test Module",
        path: String = "test/fake",
        type: ModuleType = ModuleType.AUXILIARY,
        desc: String = "A test module"
    ): AncModule = object : AncModule() {
        override val name = n
        override val description = desc
        override val moduleType = type
        override val modulePath = path
    }

    test("register increments count") {
        val fw = makeFramework()
        fw.moduleManager.count shouldBe 0
        fw.moduleManager.register { fakeModule() }
        fw.moduleManager.count shouldBe 1
    }

    test("create returns fresh instance") {
        val fw = makeFramework()
        fw.moduleManager.register { fakeModule(path = "scanner/test") }
        val mod = fw.moduleManager.create("auxiliary/scanner/test")
        mod.shouldNotBeNull()
        mod.modulePath shouldBe "scanner/test"
    }

    test("create returns null for unknown module") {
        val fw = makeFramework()
        fw.moduleManager.create("auxiliary/nonexistent").shouldBeNull()
    }

    test("create sets framework reference") {
        val fw = makeFramework()
        fw.moduleManager.register { fakeModule(path = "scanner/test") }
        val mod = fw.moduleManager.create("auxiliary/scanner/test")!!
        mod.framework shouldBe fw
    }

    test("allMeta returns sorted list") {
        val fw = makeFramework()
        fw.moduleManager.register { fakeModule(path = "z/last") }
        fw.moduleManager.register { fakeModule(path = "a/first") }
        val all = fw.moduleManager.allMeta()
        all shouldHaveSize 2
        all[0].fullName shouldBe "auxiliary/a/first"
        all[1].fullName shouldBe "auxiliary/z/last"
    }

    test("search finds by fullName substring") {
        val fw = makeFramework()
        fw.moduleManager.register { fakeModule(path = "scanner/portscan") }
        fw.moduleManager.register { fakeModule(path = "gather/dns") }
        val results = fw.moduleManager.search("portscan")
        results shouldHaveSize 1
        results[0].fullName shouldBe "auxiliary/scanner/portscan"
    }

    test("search finds by description") {
        val fw = makeFramework()
        fw.moduleManager.register { fakeModule(path = "a/b", desc = "checks TCP connectivity") }
        fw.moduleManager.register { fakeModule(path = "c/d", desc = "gather DNS info") }
        val results = fw.moduleManager.search("TCP connectivity")
        results shouldHaveSize 1
    }

    test("search is case-insensitive") {
        val fw = makeFramework()
        fw.moduleManager.register { fakeModule(path = "scanner/portscan") }
        fw.moduleManager.search("PORTSCAN") shouldHaveSize 1
        fw.moduleManager.search("PortScan") shouldHaveSize 1
    }

    test("byType filters correctly") {
        val fw = makeFramework()
        fw.moduleManager.register { fakeModule(type = ModuleType.EXPLOIT, path = "windows/smb/test") }
        fw.moduleManager.register { fakeModule(type = ModuleType.AUXILIARY, path = "scanner/test") }
        fw.moduleManager.register { fakeModule(type = ModuleType.AUXILIARY, path = "scanner/test2") }
        fw.moduleManager.byType(ModuleType.EXPLOIT) shouldHaveSize 1
        fw.moduleManager.byType(ModuleType.AUXILIARY) shouldHaveSize 2
        fw.moduleManager.byType(ModuleType.POST).shouldBeEmpty()
    }

    test("meta returns module metadata") {
        val fw = makeFramework()
        fw.moduleManager.register { fakeModule(path = "scanner/test", desc = "test desc") }
        val meta = fw.moduleManager.meta("auxiliary/scanner/test")
        meta.shouldNotBeNull()
        meta.description shouldBe "test desc"
        meta.moduleType shouldBe ModuleType.AUXILIARY
    }

    test("meta returns null for unknown module") {
        val fw = makeFramework()
        fw.moduleManager.meta("auxiliary/nonexistent").shouldBeNull()
    }
})
