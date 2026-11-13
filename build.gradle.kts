import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    alias(libs.plugins.spotless)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.allure.report)
    alias(libs.plugins.test.retry)
}

group = "com.toolshop"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.allure.bom))

    testImplementation(libs.selenium.java)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.allure.jupiter)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.datafaker)
    testImplementation(libs.dotenv)
    testImplementation(libs.slf4j.api)
    testImplementation(libs.jspecify)

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.logback.classic)

    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

// ---------------------------------------------------------------------------
// Calidad: tipado estricto (lint + Error Prone + NullAway) y formateo.
// ---------------------------------------------------------------------------
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.get().toInt()
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Werror", "-parameters"))
    options.errorprone {
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "com.toolshop")
        option("NullAway:JSpecifyMode", "true")
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat(libs.versions.google.java.format.get())
        removeUnusedImports()
        formatAnnotations()
    }
    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ---------------------------------------------------------------------------
// Ejecución de pruebas.
// ---------------------------------------------------------------------------
val isCi = providers.environmentVariable("CI").map { it.isNotBlank() && it != "false" }.getOrElse(false)
val allureResultsDir = layout.buildDirectory.dir("allure-results")

tasks.test {
    useJUnitPlatform {
        providers.gradleProperty("includeTags").orNull?.let { includeTags(*it.split(",").toTypedArray()) }
        providers.gradleProperty("excludeTags").orNull?.let { excludeTags(*it.split(",").toTypedArray()) }
    }

    // Las pruebas E2E dependen de un sistema externo: nunca se toman de la caché.
    outputs.upToDateWhen { false }

    // Paralelismo dentro de la JVM: un WebDriver por prueba (ThreadLocal).
    val parallelism = providers.environmentVariable("PARALLELISM")
        .orElse(providers.gradleProperty("parallelism"))
        .getOrElse("4")
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", parallelism)
    systemProperty("junit.jupiter.execution.parallel.config.fixed.max-pool-size", parallelism)

    systemProperty("allure.results.directory", allureResultsDir.get().asFile.absolutePath)
    doFirst { allureResultsDir.get().asFile.deleteRecursively() }

    // Reintentos solo en CI.
    retry {
        maxRetries = if (isCi) 2 else 0
        maxFailures = 10
        failOnPassedAfterRetry = false
    }

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
    }

    finalizedBy(tasks.named("allureReport"))
}

allure {
    version = libs.versions.allure.report.get()
    report {
        singleFile = true
    }
}

tasks.named("allureReport") {
    // El reporte se genera siempre, también cuando hay pruebas fallidas.
    mustRunAfter(tasks.test)
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}
