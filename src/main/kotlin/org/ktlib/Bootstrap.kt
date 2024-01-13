package org.ktlib

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * The `Bootstrap` interface represents the contract for initializing a system or application. If you
 * create an implementation of this interface, then set the configuration property named `bootstrap`
 * to the class name of your implementation, then the `init` function on that class will be called
 * when the environment is initialized.
 */
interface Bootstrap {
    fun init()
}

object BootstrapRunner {
    private val logger by lazy { KotlinLogging.logger {} }

    init {
        configOrNull<Bootstrap>("bootstrap")?.apply {
            logger.info { "Running bootstrap: ${this::class.qualifiedName}" }
            init()
        }
    }

    fun init() = Unit
}
