plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot
    implementation(libs.spring.boot.starter.web)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Resilience4j
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)

    // Test dependencies
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(project(":tc-common"))
    testImplementation(libs.testcontainers.toxiproxy)
    testImplementation(libs.wiremock)
}
