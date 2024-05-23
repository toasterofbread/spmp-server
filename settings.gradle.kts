rootProject.name = "spms"

include(":library")
includeBuild("library/build-logic")
include(":app")

pluginManagement {
    repositories {
        maven("https://jitpack.io")
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        val kotlin_version: String = extra["kotlin.version"] as String
        kotlin("multiplatform").version(kotlin_version)
        kotlin("plugin.serialization").version(kotlin_version)
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
