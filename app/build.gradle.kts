import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmRun

val GENERATED_FILE_PREFIX: String = "// Generated on build in build.gradle.kts\n"

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(22)

    jvm {
        withJava()

        mainRun {
            mainClass.set("dev.toastbits.spms.MainKt")
        }
    }

    val native_targets = listOf(
        linuxX64(),
        linuxArm64(),
        mingwX64()
    )

    applyDefaultHierarchyTemplate()

    for (target in native_targets) {
        target.binaries {
            executable {
                entryPoint = "dev.toastbits.spms.main"
            }
        }
    }

    sourceSets {
        all {
            languageSettings.enableLanguageFeature("ExpectActualClasses")
        }

        val commonMain by getting {
            dependencies {
                implementation(project(":library"))

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

                val clikt_version: String = extra["clikt.version"] as String
                implementation("com.github.ajalt.clikt:clikt:$clikt_version")

                val okio_version: String = extra["okio.version"] as String
                implementation("com.squareup.okio:okio:$okio_version")

                val mediasession_version: String = extra["mediasession.version"] as String
                implementation("dev.toastbits:mediasession:$mediasession_version")

                val kjna_version: String = rootProject.extra["kjna.version"] as String
                implementation("dev.toastbits.kjna:runtime:$kjna_version")
            }
        }
    }
}

tasks.register("bundleIcon") {
    val in_file: File = rootProject.file("icon.png")
    inputs.file(in_file)

    val out_file: File = project.file("src/commonMain/kotlin/Icon.gen.kt")
    outputs.file(out_file)

    doLast {
        check(in_file.isFile)

        out_file.writer().use { writer ->
            writer.write("${GENERATED_FILE_PREFIX}\n// https://youtrack.jetbrains.com/issue/KT-39194\n")
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

tasks.matching { it.name.startsWith("compileKotlin") }.all {
    dependsOn("bundleIcon")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
