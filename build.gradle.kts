@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import java.io.PrintWriter
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

val FLAG_LINK_STATIC: String = "linkStatic"
val GENERATED_FILE_PREFIX: String = "// Generated on build in build.gradle.kts\n"

plugins {
    kotlin("multiplatform") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val platform_specific_files: List<String> = listOf(
    "cinterop/indicator/TrayIndicatorImpl.kt",
    "spms/Platform.kt"
)

val cinterop_definitions: List<CInteropDefinition> = listOf(
    CInteropDefinition(
        "libmpv",
        when (OS.target) {
            OS.WINDOWS -> "libmpv.dll.a"
            else -> "mpv"
        },
        listOf("mpv/client.h"),
        bin_dependencies = when (OS.target) {
            OS.WINDOWS -> listOf("libmpv-2.dll")
            else -> emptyList()
        }
    ),
    CInteropDefinition(
        "libzmq",
        when (OS.target) {
            OS.WINDOWS -> "libzmq-mt-4_3_5.lib"
            else -> "libzmq"
        },
        listOf("zmq.h", "zmq_utils.h"),
        static_lib = "libzmq.a",
        compiler_opts = listOf("-DZMQ_BUILD_DRAFT_API=1"),
        linker_opts = when (OS.target) {
            OS.WINDOWS -> listOf("-lssp")
            else -> emptyList()
        },
        bin_dependencies = when (OS.target) {
            OS.WINDOWS -> listOf("libzmq-mt-4_3_5.dll")
            else -> emptyList()
        }
    ),
    CInteropDefinition(
        "libappindicator",
        "appindicator3-0.1",
        listOf("libappindicator3-0.1/libappindicator/app-indicator.h"),
        platforms = listOf(OS.LINUX)
    )
)

kotlin {
    val native_target: KotlinNativeTargetWithHostTests =
        when (OS.target) {
            OS.LINUX -> linuxX64("native")
            OS.WINDOWS -> mingwX64("native")
            OS.OSX_X86 -> macosX64("native")
            OS.OSX_ARM -> macosArm64("native")
        }

    native_target.apply {
        compilations.getByName("main") {
            cinterops {
                for (lib in cinterop_definitions) {
                    if (!lib.platforms.contains(OS.target)) {
                        continue
                    }
                    create(lib.name)
                }
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

                if (OS.target != OS.WINDOWS) {
                    implementation("io.ktor:ktor-server-core:$ktor_version")
                    implementation("io.ktor:ktor-server-cio:$ktor_version")
                }
            }
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.6"
    distributionType = Wrapper.DistributionType.BIN
}

tasks.register("bundleIcon") {
    val in_file: File = project.file("icon.png")
    inputs.file(in_file)

    val out_file: File = project.file("src/nativeMain/kotlin/Icon.gen.kt")
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

tasks.register("bundleGitCommitHash") {
    val out_file: File = project.file("src/nativeMain/kotlin/GitCommitHash.gen.kt")
    outputs.file(out_file)
    outputs.upToDateWhen { false }

    doLast {
        val git_commit_hash: String = getCurrentGitCommitHash()!!
        out_file.writeText("${GENERATED_FILE_PREFIX}val GIT_COMMIT_HASH: String = \"$git_commit_hash\"\n")
    }
}

tasks.register("configurePlatformSpecificFiles") {
    outputs.upToDateWhen { false }

    fun String.getFile(suffix: String? = null): File =
        project.file("src/nativeMain/kotlin/" + if (suffix == null) this.replace(".kt", ".gen.kt") else (this + suffix))

    for (path in platform_specific_files) {
        outputs.file(path.getFile())

        val input_files: List<File> =
            OS.values().map { path.getFile('.' + it.name.toLowerCase()) } + listOf(path.getFile(".other"))
        for (file in input_files) {
            if (file.isFile) {
                inputs.file(file)
            }
        }
    }

    doLast {
        for (path in platform_specific_files) {
            val out_file: File = path.getFile()

            var platform_file: File = path.getFile('.' + OS.target.name.toLowerCase())
            if (!platform_file.isFile) {
                platform_file = path.getFile(".other")
            }

            if (platform_file.isFile) {
                out_file.writer().use { writer ->
                    writer.write(GENERATED_FILE_PREFIX)

                    platform_file.reader().use { reader ->
                        reader.transferTo(writer)
                    }
                }
            }
        }
    }
}

tasks.getByName("compileKotlinNative") {
    dependsOn("bundleIcon")
    dependsOn("bundleGitCommitHash")
    dependsOn("configurePlatformSpecificFiles")
}

tasks.register("generateCInteropDefinitions") {
    outputs.upToDateWhen { false }

    doLast {
        val static: Boolean = project.hasProperty(FLAG_LINK_STATIC)

        val cinterop_directory: File = project.file("src/nativeInterop/cinterop")
        val bin_directory: File = project.file("src/nativeInterop/bin")

        for (lib in cinterop_definitions) {
            val file: File = cinterop_directory.resolve(lib.name + ".def")

            if (!lib.platforms.contains(OS.target)) {
                file.delete()
                continue
            }

            for (bin in lib.bin_dependencies) {
                val bin_file: File = bin_directory.resolve(bin)
                if (!bin_file.isFile) {
                    println("WARNING: File ${bin_file.path} is required by library ${lib.name}, but was not found. Compiled executable may not work correctly.")
                }
            }

            file.ensureParentDirsCreated()
            file.createNewFile()

            file.printWriter().use { writer ->
                writer.print(GENERATED_FILE_PREFIX.replace("//", "#"))
                lib.writeTo(writer, static)
            }
        }
    }
}

for (lib in cinterop_definitions) {
    if (!lib.platforms.contains(OS.target)) {
        continue
    }

    val libname: String = lib.name.first().toUpperCase() + lib.name.drop(1)
    tasks.getByName("cinterop${libname}Native") {
        dependsOn("generateCInteropDefinitions")
    }
}

open class FinaliseBuild: DefaultTask() {
    @get:InputDirectory
    val binary_output_directory: DirectoryProperty = project.objects.directoryProperty()

    @get:Input
    val _cinterop_definitions: ListProperty<CInteropDefinition> = project.objects.listProperty()

    @TaskAction
    fun execute() {
        val bin_directory: File = project.file("src/nativeInterop/bin")
        val target_directory: File = binary_output_directory.get().asFile

        for (lib in _cinterop_definitions.get()) {
            for (bin in lib.bin_dependencies) {
                val file: File = bin_directory.resolve(bin)
                if (!file.isFile)  {
                    continue
                }

                Files.copy(file.toPath(), target_directory.resolve(bin).toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}

tasks.register("finaliseBuildDebug", FinaliseBuild::class.java) {
    val task = tasks.getByName("linkDebugExecutableNative")
    binary_output_directory.set(task.outputs.getFiles().single())

    _cinterop_definitions.set(cinterop_definitions)
}

tasks.register("finaliseBuildRelease", FinaliseBuild::class.java) {
    val task = tasks.getByName("linkReleaseExecutableNative")
    binary_output_directory.set(task.outputs.getFiles().single())

    _cinterop_definitions.set(cinterop_definitions)
}

tasks.getByName("linkDebugExecutableNative") {
    finalizedBy("finaliseBuildDebug")
}

tasks.getByName("linkReleaseExecutableNative") {
    finalizedBy("finaliseBuildRelease")
}

enum class OS {
    LINUX, WINDOWS, OSX_X86, OSX_ARM;

    companion object {
        val target: OS get() {
            val target_os: String = System.getenv("SPMS_OS") ?: System.getProperty("os.name")
            val target_arch: String = System.getenv("SPMS_ARCH") ?: System.getProperty("os.arch")

            val os: String = target_os.toLowerCase()
            val arch: String = target_arch.toLowerCase()

            return when {
                os == "linux" -> LINUX
                os.startsWith("windows") -> WINDOWS
                os == "mac os x" && arch == "x86_64" -> OSX_X86
                os == "mac os x" && arch == "aarch64" -> OSX_ARM
                else -> throw GradleException("Host OS '$target_os' ($target_arch) is not supported by Kotlin/Native")
            }
        }
    }
}

data class CInteropDefinition(
    val name: String,
    val lib: String,
    val headers: List<String>,
    val static_lib: String? = null,
    val compiler_opts: List<String> = emptyList(),
    val linker_opts: List<String> = emptyList(),
    val bin_dependencies: List<String> = emptyList(),
    val platforms: List<OS> = OS.values().toList()
) {
    fun writeTo(writer: PrintWriter, static: Boolean) {
        writer.writeList("headers", headers.map { formatPlatformInclude(it) })

        var copts: List<String> = getBaseCompilerOpts() + compiler_opts
        var lopts: List<String> = getBaseLinkerOpts() + linker_opts
        
        if (static && static_lib != null) {
            writer.writeList("libraryPaths", listOf("src/nativeInterop/lib"))
            writer.writeList("staticLibraries", listOf(static_lib))
        }
        else {
            copts += pkgConfig(formatPlatformLib(lib), cflags = true)
            lopts += pkgConfig(formatPlatformLib(lib), libs = true)
        }

        writer.writeList("compilerOpts", copts)
        writer.writeList("linkerOpts", lopts)
    }

    private fun formatPlatformInclude(path: String): String =
        when (OS.target) {
            else -> path
        }

    private fun formatPlatformLib(path: String): String =
        when (OS.target) {
            OS.WINDOWS -> project.file("src/nativeInterop/lib").absolutePath.replace('\\', '/') + '/' + path
            else -> path
        }

    private fun getBaseCompilerOpts(): List<String> =
        when (OS.target) {
            OS.LINUX -> listOf("-I/usr/include", "-I/usr/include/x86_64-linux-gnu")
            else -> emptyList()
        } + listOf("-I" + project.file("src/nativeInterop/include").absolutePath.replace('\\', '/'))

    private fun getBaseLinkerOpts(): List<String> =
        when (OS.target) {
            OS.LINUX -> listOf("-L/usr/lib", "-L/usr/lib/x86_64-linux-gnu")
            else -> emptyList()
        }

    private fun PrintWriter.writeList(property: String, items: List<String>) {
        if (items.isNotEmpty()) {
            print("$property =")
            for (header in items)  {
                print(' ')
                print(header)
            }
            print('\n')
        }
    }
}

// https://gist.github.com/micolous/c00b14b2dc321fdb0eab8ad796d71b80
fun pkgConfig(
    vararg package_names: String,
    cflags: Boolean = false,
    libs: Boolean = false
): List<String> {
    if (OS.target != OS.LINUX) {
        if (libs) {
            return package_names.map {
                if (it.startsWith("lib")) "-l" + it.drop(3)
                else it
            }
        }
        return emptyList()
    }

    require(cflags || libs)

    val process_builder: ProcessBuilder = ProcessBuilder(
        listOfNotNull(
            "pkg-config",
            if (cflags) "--cflags" else null,
            if (libs) "--libs" else null
        ) + package_names
    )
    process_builder.environment()["PKG_CONFIG_ALLOW_SYSTEM_LIBS"] = "1"

    val process: Process = process_builder.start()
    process.waitFor(10, TimeUnit.SECONDS)

    check(process.exitValue() == 0) {
        process.errorStream
        "pkg-config failed with package_names: ${package_names.toList()}\n" + process.errorStream.bufferedReader().use { it.readText() }
    }

    return process.inputStream.bufferedReader().use { reader ->
        reader.readText().split(" ").mapNotNull { it.trim().takeIf { it.isNotBlank() } }
    }
}

fun cmd(vararg args: String): String {
    val out = ByteArrayOutputStream()
    exec {
        commandLine(args.toList())
        standardOutput = out
    }
    return out.toString().trim()
}

fun getCurrentGitTag(): String? {
    try {
        return cmd("git", "tag", "--points-at", "HEAD").ifBlank { null }
    }
    catch (e: Throwable) {
        RuntimeException("Getting Git tag failed", e).printStackTrace()
        return null
    }
}

fun getCurrentGitCommitHash(): String? {
    try {
        return cmd("git", "rev-parse", "HEAD").ifBlank { null }
    }
    catch (e: Throwable) {
        RuntimeException("Getting Git commit hash failed", e).printStackTrace()
        return null
    }
}
