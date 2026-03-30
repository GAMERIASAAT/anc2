package anc.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class FrameworkTest : FunSpec({

    beforeEach { Framework.reset() }
    afterEach { Framework.reset() }

    fun fakeModule(path: String = "test/fake"): AncModule = object : AncModule() {
        override val name = "Fake"
        override val description = "A fake module"
        override val moduleType = ModuleType.AUXILIARY
        override val modulePath = path
        override suspend fun run() {}
    }

    test("getInstance throws before initialize") {
        shouldThrow<IllegalStateException> {
            Framework.getInstance()
        }
    }

    test("initialize returns Framework instance") {
        val fw = Framework.initialize()
        fw.shouldNotBeNull()
    }

    test("getInstance returns same instance as initialize") {
        val fw = Framework.initialize()
        Framework.getInstance() shouldBe fw
    }

    test("initialize is idempotent - second call returns same instance") {
        val fw1 = Framework.initialize()
        val fw2 = Framework.initialize()
        fw1 shouldBe fw2
    }

    test("reset clears singleton") {
        Framework.initialize()
        Framework.reset()
        shouldThrow<IllegalStateException> {
            Framework.getInstance()
        }
    }

    test("initialize registers provided modules") {
        val fw = Framework.initialize(listOf({ fakeModule("scan/a") }, { fakeModule("scan/b") }))
        fw.moduleManager.count shouldBe 2
    }

    test("moduleManager is accessible after initialization") {
        val fw = Framework.initialize()
        fw.moduleManager.shouldNotBeNull()
    }

    test("sessionManager is accessible after initialization") {
        val fw = Framework.initialize()
        fw.sessionManager.shouldNotBeNull()
    }

    test("eventBus is accessible after initialization") {
        val fw = Framework.initialize()
        fw.eventBus.shouldNotBeNull()
    }

    test("scope is accessible after initialization") {
        val fw = Framework.initialize()
        fw.scope.shouldNotBeNull()
    }
})
