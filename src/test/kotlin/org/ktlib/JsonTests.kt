package org.ktlib

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class JsonTests : StringSpec({
    "serialize" {
        val result = Json.serialize(TestData("1", "2"))

        result shouldBe "{\"oneValue\":\"1\",\"twoValue\":\"2\"}"
    }

    "deserialize to map" {
        val data = "{\"one\":\"1\"}".fromJson<Map<String, String>>()

        data["one"] shouldBe "1"
    }

    "deserialize list" {
        val json = Json.serialize(listOf(TestData("1", "2")))
        val result = json.fromJson<List<TestData>>()

        result.size shouldBe 1
    }

    "deserialize list by type" {
        val json = Json.serialize(listOf(TestData("1", "2")))
        val result = json.fromJson<List<TestData>>()

        result.size shouldBe 1
        result[0].oneValue shouldBe "1"
    }

    "deserialize string" {
        val result = "\"hello\"".fromJson<String>()

        result shouldBe "hello"
    }

    "deserialize string list" {
        val result = "[\"hello\",\"there\"]".fromJson<List<String>>()

        result shouldBe listOf("hello", "there")
    }

    "deserialize snake case" {
        val result = """{"one_value":"one","two_value":"two"}""".fromJson<TestDataSnake>()

        result.oneValue shouldBe "one"
        result.twoValue shouldBe "two"
    }

    "deserialize snake case list" {
        val result = """[{"one_value":"one","two_value":"two"}]""".fromJson<List<TestDataSnake>>()

        result[0].oneValue shouldBe "one"
        result[0].twoValue shouldBe "two"
    }

    "serialize snake case" {
        val result = Json.serialize(TestDataSnake(oneValue = "one", twoValue = "two"))

        result shouldBe """{"one_value":"one","two_value":"two"}"""
    }

    "serialize date" {
        val date = LocalDateTime.now()
        val result = Json.serialize(TestDataWithDate(date))

        result.contains("T") shouldBe true
    }
})

data class TestData(val oneValue: String, val twoValue: String)
data class TestDataWithDate(val date: LocalDateTime)

@SnakeCaseJson
data class TestDataSnake(val oneValue: String, val twoValue: String)