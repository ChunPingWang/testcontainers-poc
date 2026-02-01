plugins {
    `java-library`
}

dependencies {
    // Testcontainers BOM
    api(platform(libs.testcontainers.bom))
    api(libs.testcontainers.core)
    api(libs.testcontainers.junit)
    api(libs.testcontainers.postgresql)
    api(libs.testcontainers.rabbitmq)
    api(libs.testcontainers.kafka)
    api(libs.testcontainers.elasticsearch)
    api(libs.testcontainers.vault)
    api(libs.testcontainers.localstack)
    api(libs.testcontainers.toxiproxy)

    // Testing utilities
    api(libs.awaitility)
    api(libs.rest.assured)

    // JUnit
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)

    // Keycloak Admin Client (for token generation)
    implementation(libs.keycloak.admin.client)

    // WireMock
    api(libs.wiremock)

    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
}
