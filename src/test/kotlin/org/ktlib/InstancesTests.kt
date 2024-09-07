package org.ktlib

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InstancesTests : StringSpec() {
    interface SomeInterface
    object OneImplementation : SomeInterface
    object TwoImplementation : SomeInterface

    interface AnotherInterface
    object AnotherImplementation : AnotherInterface

    init {
        "can give default for instance" {
            registerInstanceFactory<SomeInterface> { OneImplementation }

            val result = lookupInstance<SomeInterface>(TwoImplementation)

            result shouldBe OneImplementation
        }

        "returns default if nothing found" {
            val result = lookupInstance<AnotherInterface>(AnotherImplementation)

            result shouldBe AnotherImplementation
        }
    }
}