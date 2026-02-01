plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        // Confluent repository for Kafka/Avro
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
        // Testcontainers configuration
        environment("TESTCONTAINERS_REUSE_ENABLE", "true")
        // Use docker.raw.sock for direct Docker VM access on Docker Desktop
        val dockerRawSocket = "unix://${System.getProperty("user.home")}/Library/Containers/com.docker.docker/Data/docker.raw.sock"
        environment("DOCKER_HOST", dockerRawSocket)
        environment("DOCKER_API_VERSION", "1.44")
        systemProperty("docker.host", dockerRawSocket)
        systemProperty("docker.api.version", "1.44")
        environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required = true
            html.required = true
        }
    }
}

// Aggregated JaCoCo report
tasks.register<JacocoReport>("jacocoAggregatedReport") {
    dependsOn(subprojects.map { it.tasks.named("test") })

    additionalSourceDirs.setFrom(subprojects.flatMap { it.sourceSets["main"].allSource.srcDirs })
    sourceDirectories.setFrom(subprojects.flatMap { it.sourceSets["main"].allSource.srcDirs })
    classDirectories.setFrom(subprojects.flatMap { it.sourceSets["main"].output })
    executionData.setFrom(subprojects.mapNotNull {
        it.tasks.findByName("test")?.let { task ->
            (task as Test).extensions.getByType(JacocoTaskExtension::class.java).destinationFile
        }
    })

    reports {
        xml.required = true
        html.required = true
        html.outputLocation = layout.buildDirectory.dir("reports/jacoco/aggregated/html")
        xml.outputLocation = layout.buildDirectory.file("reports/jacoco/aggregated/jacocoAggregatedReport.xml")
    }
}
