package org.ktlib

import ch.qos.logback.classic.ClassicConstants
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Initializes the environment.
 *
 * - The Environment name is set with a SystemProperty named 'environment' or an Environment Variable named
 * "ENVIRONMENT". If an environment isn't set, it will default to 'local'.
 *
 * - If a logback.xml file or logback-{environment}.xml file exists on the root of the classpath, then Logback
 * will be initialized using that file.
 *
 * - If an 'app-banner.txt' file exists on the root of the classpath it's contents will be printed out first to the
 * logger.
 *
 * - If an environment variable named APP_VERSION or 'version' file exists on the root of the classpath it's contents
 * will be set as the version on the environment.
 */
object Environment : Init() {
    val name: String = System.getProperty("environment") ?: System.getenv("ENVIRONMENT") ?: "local"
    val version: String?
    val isLocal: Boolean
    val isProd: Boolean
    val isTest: Boolean
    val isNotLocal: Boolean
    val isNotProd: Boolean
    val isNotTest: Boolean

    init {
        System.setProperty("environment", name)

        val envLogback = "logback-$name.xml"
        val logbackConfig = if (envLogback.resourceExists()) envLogback else "logback.xml"
        if (logbackConfig.resourceExists()) {
            System.setProperty(ClassicConstants.CONFIG_FILE_PROPERTY, logbackConfig)
            System.setProperty("logging.config", "classpath:$logbackConfig")
        }

        val logger = KotlinLogging.logger {}

        val banner = "app-banner.txt".resourceAsString()
        if (banner != null) logger.info { "\n$banner\n" }

        logger.info { "Environment: $name" }

        version = System.getenv("APP_VERSION") ?: "version".resourceAsString()
        if (version != null) logger.info { "Version: $version" }

        isLocal = name == "local"
        isProd = name == "prod"
        isTest = name == "test"

        isNotProd = !isProd
        isNotLocal = !isLocal
        isNotTest = !isTest
    }
}
