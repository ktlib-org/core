buildscript {
    val kotlinVersion: String by project

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
    }
}

group = "org.ktlib"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.0.20"
}

dependencies {
    val kotlinVersion: String by project
    val jacksonVersion: String by project
    val kotestVersion: String by project

    compileOnly(gradleApi())

    implementation("ch.qos.logback:logback-classic:1.5.7")
    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.yaml:snakeyaml:2.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.springframework.security:spring-security-crypto:6.3.3")
    implementation("com.github.f4b6a3:uuid-creator:6.0.0")
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.42.1"))
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.27.0-alpha")
    implementation("io.opentelemetry:opentelemetry-exporter-logging")

    compileOnly("io.kotest:kotest-runner-junit5:$kotestVersion")
    compileOnly("io.mockk:mockk:1.13.12")
    compileOnly("io.sentry:sentry-servlet:7.14.0")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.mockk:mockk:1.13.12")
}

kotlin {
    jvmToolchain(21)
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.ktlib"
            artifactId = "core"
            version = "0.1.1"

            from(components["java"])

            pom {
                name.set("core")
                description.set("A library making some things easier in Kotlin")
                url.set("http://ktlib.org")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("aaronfreeman")
                        name.set("Aaron Freeman")
                        email.set("aaron@freeman.zone")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:ktlib-org/ktlib.git")
                    url.set("https://github.com/ktlib-org/ktlib")
                }
            }
        }
    }
}
