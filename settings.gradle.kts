pluginManagement {
    plugins {
        val kotlin_version = extra["kotlin.version"] as String
        kotlin("multiplatform").version(kotlin_version)
        kotlin("plugin.serialization").version(kotlin_version)
    }
}
