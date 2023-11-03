package org.ktlib.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.ktlib.typeRef

class InMemoryCacheTests : StringSpec({

    data class Data(val value: String)

    "connected works" {
        InMemoryCache.connected shouldBe true
    }
    
    "reading missing key returns null" {
        InMemoryCache.get("missing") shouldBe null
    }

    "reading set key" {
        InMemoryCache.set("myKey", "myValue")

        InMemoryCache.get("myKey") shouldBe "myValue"
    }

    "deleting key" {
        InMemoryCache.set("anotherKey", "value")

        InMemoryCache.delete("anotherKey")

        InMemoryCache.get("anotherKey") shouldBe null
    }

    "updating key" {
        InMemoryCache.set("toUpdate", "oldValue")

        InMemoryCache.update("toUpdate", "newValue")

        InMemoryCache.get("toUpdate") shouldBe "newValue"
    }

    "get as" {
        InMemoryCache.set("data", Data("aValue"))

        val data = InMemoryCache.getAs<Data>("data", typeRef())

        data?.value shouldBe "aValue"
    }

    "get as inline" {
        InMemoryCache.set("data", Data("aValue"))

        val data: Data? = InMemoryCache.getAs("data")

        data?.value shouldBe "aValue"
    }
})