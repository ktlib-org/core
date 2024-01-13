package org.ktlib

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BootstrapTests : StringSpec({
    "boostrap called" {
        BootstrapRunner.init()
        System.getProperty("boostrap-called") shouldBe "true"
    }
})