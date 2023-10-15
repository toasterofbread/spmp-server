plugins {
    kotlin("multiplatform") version "1.9.10"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    val host_os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    val nativeTarget = when {
        host_os == "Linux" -> linuxX64("native")
        host_os.startsWith("Windows") -> mingwX64("native")
        host_os == "Mac OS X" && arch == "x86_64" -> macosX64("native")
        host_os == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
        else -> throw GradleException("Host OS '$host_os' is not supported Kotlin/Native")
    }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val libmpv by creating
                val libzmq by creating
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
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

tasks.withType<Wrapper> {
    gradleVersion = "7.6"
    distributionType = Wrapper.DistributionType.BIN
}
