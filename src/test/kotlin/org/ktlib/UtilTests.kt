package org.ktlib

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe


class UtilTests : StringSpec({
    "resourceExists return false for nonexistent resource" {
        "blah".resourceExists() shouldBe false
    }

    "resourceExists return true for existing resource" {
        "app.yml".resourceExists() shouldBe true
    }

    "resourceAsString return content of resource" {
        "app.yml".resourceAsString()?.contains("aNumberList") shouldBe true
    }

    "resourceAsString return null for missing resource" {
        "non-exising.json".resourceAsString() shouldBe null
    }

    "can parse uuid with dashes" {
        "fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b".toUUID().toString() shouldBe "fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b"
    }

    "can parse uuid without dashes" {
        "fe8e24a68c954b6d9c192d4d9ca8fa7b".toUUID().toString() shouldBe "fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b"
    }

    "can convert UUID to hex string" {
        "fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b".toUUID().toHexString() shouldBe "fe8e24a68c954b6d9c192d4d9ca8fa7b"
    }

    "loading dir" {
        val resource = UtilTests::class.java.getResource("org/ktlib")
        println(resource)
    }

    "report and throw error" {
        val exception = shouldThrow<Exception> {
            reportAndThrow {
                throw Exception("hello")
            }
        }

        exception.message shouldBe "hello"
    }

    "report and swallow error" {
        reportAndSwallow {
            throw Exception("still passing test")
        }
    }

    "zip string" {
        "hello".gzip().ungzip() shouldBe "hello"
    }

    "zip bytes" {
        val bytes = "hello".toByteArray()

        bytes.gzip().ungzip() shouldBe bytes
    }
})
