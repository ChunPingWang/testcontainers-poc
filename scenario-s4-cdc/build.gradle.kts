plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.kafka)

    // Database
    implementation(libs.bundles.database)
    runtimeOnly(libs.postgresql.driver)

    // Debezium
    implementation(libs.debezium.api)

    // Test dependencies
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(project(":tc-common"))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
}
