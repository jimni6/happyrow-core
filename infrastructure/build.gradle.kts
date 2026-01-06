plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.swagger)
}
dependencies {
    implementation(project(":domain"))

    implementation(libs.java.dogstatsd.client)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.resources)

    implementation(libs.bundles.ktor.client)

    implementation(libs.hikariCP)
    implementation(libs.postgresql)

    implementation(platform(libs.exposed.bom))
    implementation(libs.bundles.exposed)

    implementation(libs.bundles.jackson)
    
    implementation(libs.auth0.jwt)
    implementation(libs.auth0.jwks)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.localstack)
    testImplementation(libs.testcontainers)
    testImplementation(libs.ktor.client.mock)

    testFixturesImplementation(testFixtures(project(":domain")))
    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.localstack)
    testFixturesImplementation(libs.testcontainers)
    testFixturesImplementation(libs.koin.ktor)
    testFixturesImplementation(libs.liquibase.core)
    testFixturesImplementation(libs.exposed.core)
    testFixturesImplementation(libs.jackson.module.kotlin)
    testFixturesImplementation(libs.ktor.client.mock)

}

