package anc.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull

class DatastoreTest : FunSpec({

    test("get and set are case-insensitive") {
        val ds = Datastore()
        ds["RHOSTS"] = "192.168.1.1"
        ds["rhosts"] shouldBe "192.168.1.1"
        ds["Rhosts"] shouldBe "192.168.1.1"
    }

    test("getString returns string value") {
        val ds = Datastore()
        ds["KEY"] = "hello"
        ds.getString("key") shouldBe "hello"
    }

    test("getString returns default when missing") {
        val ds = Datastore()
        ds.getString("missing", "default") shouldBe "default"
    }

    test("getInt coerces Number") {
        val ds = Datastore()
        ds["PORT"] = 4444
        ds.getInt("port") shouldBe 4444
    }

    test("getInt coerces String") {
        val ds = Datastore()
        ds["PORT"] = "8080"
        ds.getInt("PORT") shouldBe 8080
    }

    test("getInt returns default when missing") {
        val ds = Datastore()
        ds.getInt("missing", 9999) shouldBe 9999
    }

    test("getLong coerces value") {
        val ds = Datastore()
        ds["TIMEOUT"] = 30000L
        ds.getLong("timeout") shouldBe 30000L
    }

    test("getBoolean coerces String true") {
        val ds = Datastore()
        ds["SSL"] = "true"
        ds.getBoolean("SSL").shouldBeTrue()
    }

    test("getBoolean coerces Boolean directly") {
        val ds = Datastore()
        ds["SSL"] = false
        ds.getBoolean("SSL").shouldBeFalse()
    }

    test("contains is case-insensitive") {
        val ds = Datastore()
        ds["LHOST"] = "0.0.0.0"
        ds.contains("lhost").shouldBeTrue()
        ds.contains("LHOST").shouldBeTrue()
        ds.contains("missing").shouldBeFalse()
    }

    test("get returns null for missing key") {
        val ds = Datastore()
        ds["missing"].shouldBeNull()
    }

    test("applyDefaults sets missing keys from option defaults") {
        val ds = Datastore()
        val opts = Options()
        opts.optional<Int>("PORT", "port number", default = 443)
        opts.optional<String>("HOST", "target host", default = "localhost")
        ds.applyDefaults(opts)
        ds.getInt("PORT") shouldBe 443
        ds.getString("HOST") shouldBe "localhost"
    }

    test("applyDefaults does not overwrite existing values") {
        val ds = Datastore()
        ds["PORT"] = 8080
        val opts = Options()
        opts.optional<Int>("PORT", "port number", default = 443)
        ds.applyDefaults(opts)
        ds.getInt("PORT") shouldBe 8080
    }

    test("toMap returns all entries") {
        val ds = Datastore()
        ds["A"] = "1"
        ds["B"] = "2"
        val map = ds.toMap()
        map["A"] shouldBe "1"
        map["B"] shouldBe "2"
    }
})
