plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.data.elasticsearch)

    // Jackson for JSON serialization (including Instant support)
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Database
    implementation(libs.bundles.database)
    runtimeOnly(libs.postgresql.driver)

    // Test dependencies
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(project(":tc-common"))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.elasticsearch)
}
