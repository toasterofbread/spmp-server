@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption

val FLAG_LINK_STATIC: String = "linkStatic"
val GENERATED_FILE_PREFIX: String = "// Generated on build in build.gradle.kts\n"

plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://jitpack.io")
}

enum class Arch {
    X86_64, ARM64;

    val libdir_name: String get() = when (this) {
        X86_64 -> "x86_64-linux-gnu"
        ARM64 -> "aarch64-linux-gnu"
    }

    val PKG_CONFIG_PATH: String get() = "/usr/lib/$libdir_name/pkgconfig"

    companion object {
        val target: Arch get() {
            val target_arch: String = System.getenv("SPMS_ARCH") ?: System.getProperty("os.arch")
            return when (target_arch.lowercase()) {
                "x86_64", "amd64" -> X86_64
                "aarch64" -> ARM64
                else -> throw GradleException("Unsupported CPU architecture '$target_arch'")
            }
        }
    }
}

enum class OS {
    LINUX_X86, LINUX_ARM64, WINDOWS, OSX_X86, OSX_ARM;

    val identifier: String
        get() = when (this) {
            LINUX_X86 -> "linux-x86_64"
            LINUX_ARM64 -> "linux-arm64"
            WINDOWS -> "windows-x86_64"
            OSX_X86 -> "osx-x86_64"
            OSX_ARM -> "osx-arm64"
        }

    val gen_file_extension: String
        get() = when (this) {
            LINUX_X86, LINUX_ARM64 -> "linux"
            WINDOWS -> "windows"
            OSX_X86, OSX_ARM -> "osx"
        }

    companion object {
        val target: OS get() {
            val target_os: String = System.getenv("SPMS_OS") ?: System.getProperty("os.name")
            val target_arch: Arch = Arch.target

            val os: String = target_os.lowercase()

            if (os == "linux") {
                return when (target_arch) {
                    Arch.X86_64 -> LINUX_X86
                    Arch.ARM64 -> LINUX_ARM64
                }
            }
            else if (os.startsWith("windows") && target_arch == Arch.X86_64) {
                return WINDOWS
            }

            throw GradleException("Unsupported host OS and architecture '$target_os' ($target_arch)")
        }
    }
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
        compiler_opts = listOf("-DZMQ_BUILD_DRAFT_API=1")
    ).apply {
        when (OS.target) {
            OS.LINUX_X86, OS.LINUX_ARM64 -> static_lib = "libzmq.a"
            OS.WINDOWS -> {
                linker_opts = listOf("-lssp")
                bin_dependencies = listOf("libzmq-mt-4_3_5.dll")
            }
            else -> {}
        }
    },
    CInteropDefinition(
        "libcurl",
        when (OS.target) {
            OS.WINDOWS -> "libcurl.lib"
            else -> "libcurl"
        },
        listOf("curl/curl.h"),
        bin_dependencies = when (OS.target) {
            OS.WINDOWS -> listOf("libcurl.dll")
            else -> emptyList()
        }
    ),
    CInteropDefinition(
        "libappindicator",
        "appindicator3-0.1",
        listOf("libappindicator3-0.1/libappindicator/app-indicator.h"),
        platforms = listOf(OS.LINUX_X86, OS.LINUX_ARM64)
    )
)

kotlin {
    val native_target: KotlinNativeTarget =
        when (OS.target) {
            OS.LINUX_X86 -> linuxX64("native")
            OS.LINUX_ARM64 -> linuxArm64("native")
            OS.WINDOWS -> mingwX64("native")
            OS.OSX_X86 -> macosX64("native")
            OS.OSX_ARM -> macosArm64("native")
        }

    println("Building for target ${OS.target}")

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
                baseName = "spms-${OS.target.identifier}"
                entryPoint = "main"
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            languageSettings.optIn("kotlin.experimental.ExperimentalNativeApi")
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")

            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("com.squareup.okio:okio:3.6.0")

                when (Arch.target) {
                    Arch.X86_64 -> implementation("com.github.ajalt.clikt:clikt:4.2.2")
                    Arch.ARM64 -> implementation("com.github.toasterofbread:clikt:65ebc8a4bb0eeecfcfeeec7cd1d05099a4e33df1")
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

tasks.register("nativeBinariesStatic") {
    val native_binaries = tasks.getByName("nativeBinaries")
    finalizedBy(native_binaries)
    group = native_binaries.group

    doFirst {
        project.ext.set(FLAG_LINK_STATIC, 1)
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

            var platform_file: File = path.getFile('.' + OS.target.gen_file_extension.toLowerCase())
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

data class CInteropDefinition(
    val name: String,
    val lib: String,
    val headers: List<String>,
    var static_lib: String? = null,
    var compiler_opts: List<String> = emptyList(),
    var linker_opts: List<String> = emptyList(),
    var bin_dependencies: List<String> = emptyList(),
    var platforms: List<OS> = OS.values().toList()
) {
    fun writeTo(writer: PrintWriter, static: Boolean) {
        writer.writeList("headers", headers.map { formatPlatformInclude(it) })

        var copts: List<String> = getBaseCompilerOpts() + compiler_opts
        var lopts: List<String> = getBaseLinkerOpts() + linker_opts

        if (static && static_lib != null) {
            writer.writeList("libraryPaths", listOf("src/nativeInterop/lib"))
            writer.writeList("staticLibraries", listOf(static_lib!!))
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
            OS.LINUX_X86, OS.LINUX_ARM64 -> listOf("-I/usr/include", "-I/usr/include/${Arch.target.libdir_name}")
            else -> emptyList()
        } + listOf("-I" + project.file("src/nativeInterop/include").absolutePath.replace('\\', '/'))

    private fun getBaseLinkerOpts(): List<String> =
        when (OS.target) {
            OS.LINUX_X86, OS.LINUX_ARM64 -> listOf("-L/usr/lib", "-L/usr/lib/${Arch.target.libdir_name}")
            else -> emptyList()
        }

    private fun PrintWriter.writeList(property: String, items: List<String>) {
        if (items.isNotEmpty()) {
            print("$property =")
            for (item in items)  {
                print(' ')
                print(item)
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
    if (OS.target != OS.LINUX_X86 && OS.target != OS.LINUX_ARM64) {
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
    process_builder.environment()["PKG_CONFIG_PATH"] = Arch.target.PKG_CONFIG_PATH
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
