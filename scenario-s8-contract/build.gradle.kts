plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot
    implementation(libs.spring.boot.starter.web)

    // Pact
    testImplementation(libs.pact.consumer.junit5)
    testImplementation(libs.pact.provider.junit5)

    // Test dependencies
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(project(":tc-common"))
}

// Configure Pact to output pact files to build/pacts directory
tasks.withType<Test> {
    systemProperty("pact.rootDir", layout.buildDirectory.dir("pacts").get().asFile.absolutePath)
}
