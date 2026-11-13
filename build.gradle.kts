import io.qameta.allure.gradle.report.tasks.AllureReport
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

// Agente de AspectJ: teje los aspectos de Allure para que @Step y @Attachment generen pasos.
val aspectjAgent: Configuration by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.allure.bom))

    testImplementation(libs.selenium.java)
    testImplementation(libs.junit.jupiter)
    // API del launcher: AllureRunListener escribe entorno, ejecutor y categorías.
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    testImplementation(libs.allure.jupiter)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.datafaker)
    testImplementation(libs.dotenv)
    testImplementation(libs.slf4j.api)
    testImplementation(libs.jspecify)

    testRuntimeOnly(libs.logback.classic)

    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)

    aspectjAgent(libs.aspectjweaver)
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
val allureReportDir = layout.buildDirectory.dir("reports/allure-report/allureReport")
val allureHistoryDir = allureReportDir.map { it.dir("history") }

/** Pasa el agente de AspectJ a la JVM de las pruebas sin romper la caché de configuración. */
class AspectjAgent(@get:Classpath val agent: FileCollection) : CommandLineArgumentProvider {
    override fun asArguments() = listOf("-javaagent:${agent.singleFile.absolutePath}")
}

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

    jvmArgumentProviders.add(AspectjAgent(aspectjAgent))
    // La conversión de capturas a JPEG usa java.awt: sin pantalla y sin ícono en el Dock de macOS.
    systemProperty("java.awt.headless", "true")

    systemProperty("allure.results.directory", allureResultsDir.get().asFile.absolutePath)
    // Resultados limpios en cada ejecución, conservando el historial del reporte anterior para
    // que Allure dibuje la tendencia entre ejecuciones.
    val resultsDir = allureResultsDir.get().asFile
    val previousHistory = allureHistoryDir.get().asFile
    doFirst {
        resultsDir.deleteRecursively()
        if (previousHistory.isDirectory) {
            previousHistory.copyRecursively(resultsDir.resolve("history"), overwrite = true)
        }
    }

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

tasks.named<AllureReport>("allureReport") {
    // El plugin allure-report por sí solo no conoce los resultados (en el plugin completo los
    // aporta el adaptador): se conecta el directorio que escribe allure-jupiter.
    resultsDirs.from(allureResultsDir)
    // Reporte limpio en cada generación, sin restos de ejecuciones anteriores.
    clean = true
    // El reporte se genera siempre, también cuando hay pruebas fallidas.
    mustRunAfter(tasks.test)
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}
