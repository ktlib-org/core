package org.ktlib

class BootstrapImpl : Bootstrap {
    override fun init() {
        println("Env: ${Environment.name}")
        System.setProperty("boostrap-called", "true")
    }
}