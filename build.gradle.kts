@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

plugins {
    kotlin("multiplatform") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    val host_os: String = System.getProperty("os.name")
    val arch: String = System.getProperty("os.arch")
    val nativeTarget: KotlinNativeTargetWithHostTests = when {
        host_os == "Linux" -> linuxX64("native")
        host_os.startsWith("Windows") -> mingwX64("native")
        host_os == "Mac OS X" && arch == "x86_64" -> macosX64("native")
        host_os == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
        else -> throw GradleException("Host OS '$host_os' ($arch) is not supported by Kotlin/Native")
    }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val libmpv by creating
                val libzmq by creating
                val libappindicator by creating
            }
        }

        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("com.github.ajalt.clikt:clikt:4.2.1")
                implementation("com.squareup.okio:okio:3.6.0")

                val ktor_version: String = "2.3.6"
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-curl:$ktor_version")
            }
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.6"
    distributionType = Wrapper.DistributionType.BIN
}

tasks.register("bundleIcon") {
    val in_file = project.file("icon.png")
    inputs.file(in_file)

    val out_file = project.file("src/nativeMain/kotlin/icon.gen.kt")
    outputs.file(out_file)

    doLast {
        check(in_file.isFile)

        out_file.writer().use { writer ->
            writer.write("// https://youtrack.jetbrains.com/issue/KT-39194\n")
            writer.write("val ICON_BYTES: ByteArray = byteArrayOf(")

            val bytes: ByteArray = in_file.readBytes()
            for ((i, byte) in bytes.withIndex()) {
                if (byte >= 0) {
                    writer.write("0x${byte.toString(16)}")
                }
                else {
                    writer.write("-0x${byte.toString(16).substring(1)}")
                }

                if (i + 1 != bytes.size) {
                    writer.write(",")
                }
            }

            writer.write(")\n")
        }
    }
}

tasks.getByName("compileKotlinNative") {
    dependsOn("bundleIcon")
}
