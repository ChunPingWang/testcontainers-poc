plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Vault (optional for dynamic credentials)
    implementation(libs.spring.cloud.vault)

    // Keycloak Admin Client
    implementation(libs.keycloak.admin.client)

    // Test dependencies
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(project(":tc-common"))
    testImplementation(libs.testcontainers.vault)
    testImplementation(libs.rest.assured)
}
