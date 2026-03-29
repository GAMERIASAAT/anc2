package anc.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class OptionsTest : FunSpec({

    test("register and retrieve option by name (case-insensitive)") {
        val opts = Options()
        opts.required<String>("RHOSTS", "Target host(s)")
        opts["RHOSTS"].shouldNotBeNull()
        opts["rhosts"].shouldNotBeNull()
        opts["Rhosts"].shouldNotBeNull()
    }

    test("get returns null for unregistered option") {
        val opts = Options()
        opts["missing"].shouldBeNull()
    }

    test("all() returns registered options in order") {
        val opts = Options()
        opts.required<String>("RHOSTS", "Target host(s)")
        opts.optional<Int>("RPORT", "Target port", default = 80)
        opts.optional<Boolean>("SSL", "Use SSL", default = false)
        opts.all() shouldHaveSize 3
        opts.all().map { it.name } shouldBe listOf("RHOSTS", "RPORT", "SSL")
    }

    test("validate returns error for missing required option") {
        val opts = Options()
        opts.required<String>("RHOSTS", "Target host(s)")
        val ds = Datastore()
        val errors = opts.validate(ds)
        errors shouldHaveSize 1
        errors[0] shouldBe "RHOSTS is required"
    }

    test("validate passes when required option is set") {
        val opts = Options()
        opts.required<String>("RHOSTS", "Target host(s)")
        val ds = Datastore()
        ds["RHOSTS"] = "192.168.1.1"
        opts.validate(ds).shouldBeEmpty()
    }

    test("validate passes when required option has default") {
        val opts = Options()
        opts.required<Int>("RPORT", "Target port", default = 80)
        val ds = Datastore()
        opts.validate(ds).shouldBeEmpty()
    }

    test("validate returns multiple errors") {
        val opts = Options()
        opts.required<String>("RHOSTS", "Target host(s)")
        opts.required<Int>("RPORT", "Target port")
        val ds = Datastore()
        val errors = opts.validate(ds)
        errors shouldHaveSize 2
    }

    test("optional option with no value passes validation") {
        val opts = Options()
        opts.optional<String>("USERNAME", "Login username")
        val ds = Datastore()
        opts.validate(ds).shouldBeEmpty()
    }

    test("option coerce String") {
        val opts = Options()
        opts.required<String>("NAME", "A name")
        val opt = opts["NAME"]!!
        @Suppress("UNCHECKED_CAST")
        (opt as Option<String>).coerce("hello") shouldBe "hello"
    }

    test("option coerce Int") {
        val opts = Options()
        opts.required<Int>("PORT", "A port")
        val opt = opts["PORT"]!!
        @Suppress("UNCHECKED_CAST")
        (opt as Option<Int>).coerce("8080") shouldBe 8080
    }

    test("option coerce Boolean") {
        val opts = Options()
        opts.required<Boolean>("SSL", "Use SSL")
        val opt = opts["SSL"]!!
        @Suppress("UNCHECKED_CAST")
        val boolOpt = opt as Option<Boolean>
        boolOpt.coerce("true") shouldBe true
        boolOpt.coerce("false") shouldBe false
    }
})
