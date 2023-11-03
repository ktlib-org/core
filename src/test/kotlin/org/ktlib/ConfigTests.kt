package org.ktlib

import io.kotest.matchers.shouldBe
import org.ktlib.test.ObjectMockSpec

class ConfigTests : ObjectMockSpec({
    objectMocks(Config::class)

    "Int config" {
        config<Int>("anInt") shouldBe 5
    }

    "String config" {
        config<String>("aString") shouldBe "string value here"
    }

    "Long config" {
        config<Long>("aLong") shouldBe 4904509409509458
    }

    "Double config" {
        config<Double>("aDouble") shouldBe 78.33
    }

    "Boolean config" {
        config<Boolean>("aBoolean") shouldBe true
    }

    "Nested config" {
        config<String>("nested.stringValue") shouldBe "Here"
    }

    "Secret config" {
        config<String>("mySecret.value") shouldBe "Hello"
    }

    "Number list config" {
        configList<Int>("aNumberList") shouldBe listOf(1, 4, 7)
    }

    "String List config" {
        configList<String>("aStringList") shouldBe listOf("a", "b", "c")
    }

    "Override in secret" {
        config<String>("overrideInSecret") shouldBe "Overridden"
    }
})
