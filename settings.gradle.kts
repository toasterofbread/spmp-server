rootProject.name = "spms"

include(":library")
includeBuild("library/build-logic")
include(":app")

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            val kjna_version: String = extra["kjna.version"] as String
            if (requested.id.toString() == "dev.toastbits.kjna") {
                useModule("dev.toastbits.kjna:plugin:$kjna_version")
            }
        }
    }

    repositories {
        mavenLocal()
        maven("https://maven.toastbits.dev/")
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }

    plugins {
        val kotlin_version: String = extra["kotlin.version"] as String
        kotlin("multiplatform").version(kotlin_version)
        kotlin("plugin.serialization").version(kotlin_version)

        val kjna_version: String = extra["kjna.version"] as String
        id("dev.toastbits.kjna").version(kjna_version)
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://maven.toastbits.dev/")
        maven("https://jitpack.io")
        mavenCentral()
    }
}
