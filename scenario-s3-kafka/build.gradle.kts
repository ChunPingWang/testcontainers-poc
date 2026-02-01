plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot & Kafka
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.kafka)

    // Avro & Schema Registry
    implementation(libs.avro)
    implementation(libs.kafka.avro.serializer)
    implementation(libs.kafka.schema.registry.client)

    // Test dependencies
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(libs.spring.kafka.test)
    testImplementation(project(":tc-common"))
    testImplementation(libs.testcontainers.kafka)
}
