package org.ktlib

import io.kotest.core.spec.style.StringSpec

class ApplicationTests : StringSpec({
    "initializes app" {
        Application {
            println("Done")
        }
    }
})