package org.ktlib

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * This is the entry point for an application.
 *
 * Usually it's called from the main function like this:
 * ```
 * fun main() = Application {
 *   // Additional app initialization done here
 * }
 * ```
 *
 * These configuration properties can be set:
 * - applicationName the name of the application
 */
object Application {
    private val initStart = System.currentTimeMillis()
    val name: String

    init {
        Environment.init()
        BootstrapRunner.init()

        name = config("applicationName")
    }

    operator fun invoke(init: () -> Any? = {}) {
        val logger = KotlinLogging.logger {}

        init()
        logger.info { "Application initialized in ${nowMillis() - initStart}ms" }
    }
}
