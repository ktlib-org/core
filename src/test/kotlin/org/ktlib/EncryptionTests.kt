package org.ktlib

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EncryptionTests : StringSpec({
    "hash string" {
        "some string".sha512() shouldBe "FJJeAaegzwgBqpX+UtVCtXivWK55l62mbbOm6uaKMp1QYApbe0Quq/Tqd+qO9f5ArPKrMdRzEbKiMsT2QAmqwQ=="
        "some string".sha256() shouldBe "YdA0RzEC19rDBZAncEcf1Q9MWyb2gxpW3ZC1GEs8MPw="
        "some string".sha1() shouldBe "i0XkvRxqy4i+v2QH0WIF9WfmKj4="
    }

    "base encoding" {
        "another string".toByteArray().base64Encode() shouldBe "YW5vdGhlciBzdHJpbmc="
    }

    "base decode" {
        String("something".toByteArray().base64Encode().base64Decode()) shouldBe "something"
    }

    "hash password" {
        val password = "myPassword"

        password.matchesHashedPassword(password.hashPassword()) shouldBe true
    }

    "generate key" {
        generateKey(10).length shouldBe 10
        generateKey(70).length shouldBe 70
        generateKey(5248).length shouldBe 5248
    }
})