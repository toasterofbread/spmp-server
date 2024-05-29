rootProject.name = "spms"

include(":library")
includeBuild("library/build-logic")
include(":app")

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://jitpack.io")
        mavenCentral()
        gradlePluginPortal()
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
        mavenCentral()
    }
}
