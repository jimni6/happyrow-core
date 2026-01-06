version = System.getProperty("app.version")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    id("com.diffplug.spotless") version "6.25.0"
    application
    `maven-publish`
    `java-test-fixtures`
}
repositories {
    mavenCentral()
    gradlePluginPortal()
}
kotlin {
    jvmToolchain(21)
}
allprojects {
    val libs = rootProject.libs
    apply(plugin = "kotlin")
    apply(plugin = "java-test-fixtures")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    repositories {
        mavenCentral()
//        maven { url = uri("https://repository.betclic.net/artifactory/maven/") }
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(libs.kotlin.reflect)
        implementation(libs.logback.classic)
        implementation(libs.arrow.core)
        implementation(libs.arrow.fx.coroutines)

        testImplementation(libs.bundles.kotest.assertions)
        testImplementation(libs.kotest.runner.junit5)
        testImplementation(libs.mockk)
        testImplementation(libs.awaitility.kotlin)
        testImplementation(libs.junit.jupiter.params)

        testFixturesImplementation(libs.bundles.kotest.assertions)
        testFixturesImplementation(libs.arrow.core)
        testFixturesImplementation(libs.awaitility.kotlin)

        // detektPlugins(libs.detekt.formatting) // Disabled to avoid conflicts with Spotless formatting
    }

    tasks.withType<Test> {
        environment("DB_CHANGELOG_FILEPATH", File("${project.rootDir}/deploy/db").absolutePath)
        useJUnitPlatform()
    }
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    baseline = file("$projectDir/detekt-baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "21"
}

// Spotless configuration for auto-formatting
spotless {
    kotlin {
        ktlint().editorConfigOverride(mapOf(
            "ktlint_standard_property-naming" to "disabled",
            "ktlint_standard_value-argument-comment" to "disabled",
            "ktlint_standard_value-parameter-comment" to "disabled",
            "ktlint_standard_comment-spacing" to "disabled",
            "ktlint_standard_discouraged-comment-location" to "disabled"
        ))
        target("src/**/*.kt", "domain/src/**/*.kt", "infrastructure/src/**/*.kt")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure"))

    implementation(libs.config4k)
    implementation(libs.java.dogstatsd.client)
    implementation(libs.auth0.jwt)
    implementation(libs.auth0.jwks)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.koin.ktor)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    testImplementation(libs.bundles.ktor.test)
    testImplementation(libs.kotest.assertions.json)
    testFixturesApi(libs.koin.test.junit5)

    testImplementation(testFixtures(project(":infrastructure")))
    testImplementation(testFixtures(project(":domain")))
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.happyrow.core.ApplicationKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Prevent running integration tests by using: -PWithoutIntegrationTests
    if (project.hasProperty("WithoutIntegrationTests")) {
        exclude("**/*Integ*")
    }
    // empty aws creds to be able to launch standalone integration tests using localstack
    environment("AWS_ACCESS_KEY_ID", "A")
    environment("AWS_SECRET_ACCESS_KEY", "B")
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

publishing {
    val registryUrl = System.getProperty("registry.url")
    val registryUser = System.getProperty("registry.user")
    val registryPasswd = System.getProperty("registry.password")
    val registryRepo = System.getProperty("registry.repository")
    val artifactVersion = version.toString()

    repositories {
        maven {
            url = uri(registryUrl + registryRepo)

            credentials {
                username = registryUser
                password = registryPasswd
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = "com.betclic"
            artifactId = "audience-config-service"
            version = artifactVersion

            from(components["java"])
        }
    }
}
