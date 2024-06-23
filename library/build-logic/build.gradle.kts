import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://jitpack.io")
    gradlePluginPortal()
}

dependencies {
    implementation("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:0.28.0")
}

kotlin {
    jvmToolchain(21)
}

// java {
//     toolchain {
//         languageVersion.set(JavaLanguageVersion.of(22))
//     }
// }

// kotlin {
//     compilerOptions {
//         jvmTarget.set("21")

//     }
// }

// val javaVersion = 22
// java {
//     toolchain {
//         languageVersion.set(JavaLanguageVersion.of(javaVersion))
//     }
// }
// tasks.withType(JavaCompile::class) {
//     options.release.set(javaVersion)
// }
