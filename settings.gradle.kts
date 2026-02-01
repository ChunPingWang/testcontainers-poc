rootProject.name = "testcontainers-poc"

// Enable typesafe project accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include all modules
include("tc-common")
include("scenario-s1-core")
include("scenario-s2-multistore")
include("scenario-s3-kafka")
include("scenario-s4-cdc")
include("scenario-s5-resilience")
include("scenario-s6-security")
include("scenario-s7-cloud")
include("scenario-s8-contract")

// Plugin management for version catalog
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Dependency resolution management
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_PROJECT
    repositories {
        mavenCentral()
        // Confluent repository for Kafka/Avro
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
    }
}
