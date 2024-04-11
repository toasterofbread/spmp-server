@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption

val FLAG_LINK_STATIC: String = "linkStatic"
val GENERATED_FILE_PREFIX: String = "// Generated on build in build.gradle.kts\n"

plugins {
    kotlin("multiplatform") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://jitpack.io")
}

fun String.capitalised() = this.first().uppercase() + this.drop(1)

enum class Arch {
    X86_64, ARM64;

    val libdir_name: String get() = when (this) {
        X86_64 -> "x86_64-linux-gnu"
        ARM64 -> "aarch64-linux-gnu"
    }

    val PKG_CONFIG_PATH: String get() = "/usr/lib/$libdir_name/pkgconfig"

    companion object {
        fun getTarget(project: Project): Arch {
            val target_arch: String =
                project.findProperty("SPMS_ARCH")?.toString()
                ?: System.getenv("SPMS_ARCH")
                ?: System.getProperty("os.arch")

            return when (target_arch.lowercase()) {
                "x86_64", "amd64" -> X86_64
                "aarch64", "arm64" -> ARM64
                else -> throw GradleException("Unsupported CPU architecture '$target_arch'")
            }
        }
    }
}

enum class Platform {
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
        get() = identifier.replace("-", ".")

    val alt_gen_file_extension: String
        get() = when (this) {
            LINUX_X86, LINUX_ARM64 -> "linux"
            WINDOWS -> "windows"
            OSX_X86, OSX_ARM -> "osx"
        }

    val arch: Arch
        get() = when (this) {
            LINUX_X86, WINDOWS, OSX_X86 -> Arch.X86_64
            LINUX_ARM64, OSX_ARM -> Arch.ARM64
        }

    fun getNativeDependenciesDir(project: Project) =
        project.file("src/nativeInterop/$identifier")

    companion object {
        val supported: List<Platform> = listOf(
            LINUX_X86, LINUX_ARM64, WINDOWS
        )

        fun getTarget(project: Project): Platform {
            val target_os: String =
                project.findProperty("SPMS_OS")?.toString()
                ?: System.getenv("SPMS_OS")
                ?: System.getProperty("os.name")
            val target_arch: Arch = Arch.getTarget(project)

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
    "spms/Platform.kt",
    "spms/server/SpMsMediaSession.kt"
)

enum class CinteropLibraries {
    LIBMPV, LIBZMQ, LIBCURL, LIBAPPINDICATOR;

    val identifier: String get() = name.lowercase()

    private fun Platform.createDefinition(lib: String, headers: List<String>, action: CInteropDefinition.() -> Unit = {}): CInteropDefinition =
        CInteropDefinition(this, this@CinteropLibraries.identifier, lib, headers).apply(action)

    private fun String.capitalised() = this.first().uppercase() + this.drop(1)

    fun getDefinition(platform: Platform): CInteropDefinition = when (this) {
        LIBMPV ->
            platform.createDefinition(
                when (platform) {
                    Platform.WINDOWS -> "libmpv.dll.a"
                    else -> "mpv"
                },
                listOf("mpv/client.h")
            ) {
                if (platform == Platform.WINDOWS) {
                    bin_dependencies = listOf("libmpv-2.dll")
                }
            }
        LIBZMQ ->
            platform.createDefinition(
                when (platform) {
                    Platform.WINDOWS -> "libzmq-mt-4_3_5.lib"
                    else -> "libzmq"
                },
                listOf("zmq.h", "zmq_utils.h")
            ) {
                compiler_opts = listOf("-DZMQ_BUILD_DRAFT_API=1")
                when (platform) {
                    Platform.LINUX_X86, Platform.LINUX_ARM64 -> {
                        static_lib = "libzmq.a"
                    }
                    Platform.WINDOWS -> {
                        linker_opts = listOf("-lssp", "-static", "-static-libgcc", "-static-libstdc++", "-lgcc", "-lstdc++")
                        bin_dependencies = listOf("libzmq-mt-4_3_5.dll")
                    }
                    else -> {}
                }
            }
        LIBCURL ->
            platform.createDefinition(
                when (platform) {
                    Platform.WINDOWS -> "libcurl.lib"
                    else -> "libcurl"
                },
                listOf("curl/curl.h")
            ) {
                if (platform == Platform.WINDOWS) {
                    bin_dependencies = listOf("libcurl.dll", "zlib1.dll")
                }
            }
        LIBAPPINDICATOR ->
            platform.createDefinition(
                "appindicator3-0.1",
                listOf("libappindicator3-0.1/libappindicator/app-indicator.h")
            ) {
                platforms = listOf(Platform.LINUX_X86, Platform.LINUX_ARM64)
            }
    }
}

kotlin {
    for (os in Platform.supported) {
        configureKotlinTarget(os)
    }
}

fun KotlinMultiplatformExtension.configureKotlinTarget(platform: Platform) {
    val native_target: KotlinNativeTarget = when (platform) {
        Platform.LINUX_X86 -> linuxX64(platform.identifier)
        Platform.LINUX_ARM64 -> linuxArm64(platform.identifier)
        Platform.WINDOWS -> mingwX64(platform.identifier)
        Platform.OSX_X86 -> macosX64(platform.identifier)
        Platform.OSX_ARM -> macosArm64(platform.identifier)
    }

    native_target.apply {
        compilations.getByName("main") {
            cinterops {
                for (library in CinteropLibraries.values()) {
                    val definition: CInteropDefinition = library.getDefinition(platform)
                    if (!definition.platforms.contains(platform)) {
                        continue
                    }
                    create(definition.name)
                }
            }
        }

        binaries {
            executable {
                baseName = "spms-${platform.identifier}"
                entryPoint = "main"
            }
        }
    }

    sourceSets.getByName(platform.identifier + "Main") {
        languageSettings.optIn("kotlin.experimental.ExperimentalNativeApi")
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")

        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
            implementation("com.squareup.okio:okio:3.6.0")

            if (platform == Platform.LINUX_X86) {
                implementation("dev.toastbits.mediasession:library-linuxx64:0.0.1")
            }

            when (platform.arch) {
                Arch.X86_64 -> {
                    implementation("com.github.ajalt.clikt:clikt:4.2.2")
                }
                Arch.ARM64 -> {
                    implementation("com.github.toasterofbread:clikt:65ebc8a4bb0eeecfcfeeec7cd1d05099a4e33df1")
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

tasks.register("configurePlatformSpecificFiles") {
    outputs.upToDateWhen { false }

    fun String.getFile(suffix: String? = null): File =
        project.file("src/commonMain/kotlin/" + if (suffix == null) this.replace(".kt", ".gen.kt") else (this + suffix))

    for (path in platform_specific_files) {
        outputs.file(path.getFile())

        val input_files: List<File> =
            Platform.supported.map { path.getFile('.' + it.name.toLowerCase()) } + listOf(path.getFile(".other"))
        for (file in input_files) {
            if (file.isFile) {
                inputs.file(file)
            }
        }
    }

    doLast {
        for (path in platform_specific_files) {
            val out_file: File = path.getFile()

            var platform_file: File = path.getFile('.' + Platform.getTarget(project).gen_file_extension)

            if (!platform_file.isFile) {
                platform_file = path.getFile('.' + Platform.getTarget(project).alt_gen_file_extension)

                if (!platform_file.isFile) {
                    platform_file = path.getFile(".other")
                }
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

tasks.register("printBuildTarget") {
    doLast {
        println("Building spmp-server for target ${Platform.getTarget(project)}")
    }
}

tasks.register("generateCInteropDefinitions") {
    outputs.upToDateWhen { false }

    doLast {
        println("Generating CInterop definitions for platform ${Platform.getTarget(project)}")

        val platform: Platform = Platform.getTarget(project)
        val static: Boolean = project.hasProperty(FLAG_LINK_STATIC)

        val cinterop_directory: File = project.file("src/nativeInterop/cinterop")
        val bin_directory: File = platform.getNativeDependenciesDir(project).resolve("bin")

        for (library in CinteropLibraries.values()) {
            val definition: CInteropDefinition = library.getDefinition(platform)

            val file: File = cinterop_directory.resolve(definition.name + ".def")

            if (!definition.platforms.contains(platform) && !gradle.taskGraph.hasTask(":prepareKotlinBuildScriptModel")) {
                file.delete()
                continue
            }

            for (bin in definition.bin_dependencies) {
                val bin_file: File = bin_directory.resolve(bin)
                if (!bin_file.isFile) {
                    println("WARNING: File ${bin_file.path} is required by library ${definition.name}, but was not found. Compiled executable may not work correctly.")
                }
            }

            file.ensureParentDirsCreated()
            file.createNewFile()

            file.printWriter().use { writer ->
                writer.print(GENERATED_FILE_PREFIX.replace("//", "#"))
                definition.writeTo(writer, static, project)
            }
        }
    }
}

for (platform in Platform.supported) {
    tasks.getByName("compileKotlin" + platform.identifier.capitalised()) {
        dependsOn("printBuildTarget")
        dependsOn("bundleIcon")
        dependsOn("configurePlatformSpecificFiles")
    }

    val register_task = tasks.register("setEnv${platform.identifier.capitalised()}") {
        doFirst {
            project.ext.set("SPMS_OS", when (platform) {
                Platform.LINUX_X86, Platform.LINUX_ARM64 -> "linux"
                Platform.WINDOWS -> "windows"
                else -> throw NotImplementedError(platform.name)
            })
            project.ext.set("SPMS_ARCH", platform.arch.name)
        }
    }

    for (library in CinteropLibraries.values()) {
        val definition: CInteropDefinition = library.getDefinition(platform)
        if (!definition.platforms.contains(platform)) {
            continue
        }

        tasks.getByName("cinterop${definition.name.capitalised()}${platform.identifier.capitalised()}") {
            dependsOn(register_task)

            val generate_task = tasks.getByName("generateCInteropDefinitions")
            dependsOn(generate_task)
            generate_task.mustRunAfter(register_task)
        }
    }

    val debug_link_task: Task = tasks.getByName("linkDebugExecutable" + platform.identifier.capitalised())
    val debug_finalise_task = tasks.register("finaliseBuildDebug" + platform.identifier.capitalised(), FinaliseBuild::class.java) {
        binary_output_directory.set(debug_link_task.outputs.files.single())
    }
    debug_link_task.finalizedBy(debug_finalise_task)

    val release_link_task: Task = tasks.getByName("linkReleaseExecutable" + platform.identifier.capitalised())
    val release_finalise_task = tasks.register("finaliseBuildRelease" + platform.identifier.capitalised(), FinaliseBuild::class.java) {
        binary_output_directory.set(release_link_task.outputs.files.single())
    }
    release_link_task.finalizedBy(release_finalise_task)

    tasks.register(platform.identifier + "BinariesStatic") {
        val native_binaries = tasks.getByName(platform.identifier + "Binaries")
        finalizedBy(native_binaries)
        group = native_binaries.group

        doFirst {
            project.ext.set(FLAG_LINK_STATIC, 1)
        }
    }
}

open class FinaliseBuild: DefaultTask() {
    @get:InputDirectory
    val binary_output_directory: DirectoryProperty = project.objects.directoryProperty()

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun execute() {
        val platform: Platform = Platform.getTarget(project)

        val bin_directory: File = platform.getNativeDependenciesDir(project).resolve("bin")
        val target_directory: File = binary_output_directory.get().asFile

        for (library in CinteropLibraries.values()) {
            val definition: CInteropDefinition = library.getDefinition(platform)
            for (bin in definition.bin_dependencies) {
                val file: File = bin_directory.resolve(bin)
                if (!file.isFile)  {
                    continue
                }

                file.copyTo(target_directory.resolve(bin), overwrite = true)
            }
        }
    }
}

data class CInteropDefinition(
    val platform: Platform,
    val name: String,
    val lib: String,
    val headers: List<String>,
    var static_lib: String? = null,
    var compiler_opts: List<String> = emptyList(),
    var linker_opts: List<String> = emptyList(),
    var bin_dependencies: List<String> = emptyList(),
    var platforms: List<Platform> = Platform.supported.toList()
) {
    fun writeTo(writer: PrintWriter, static: Boolean, project: Project) {
        writer.writeList("headers", headers.map { formatPlatformInclude(it) })

        var copts: List<String> = getBaseCompilerOpts(project) + compiler_opts
        var lopts: List<String> = getBaseLinkerOpts(project) + linker_opts

        if (static && static_lib != null) {
            writer.writeList(
                "libraryPaths",
                listOfNotNull(
                    platform.getNativeDependenciesDir(project).resolve("lib").absolutePath,
                    System.getenv("SPMS_LIB")
                )
            )
            writer.writeList("staticLibraries", listOf(static_lib!!))
        }
        else {
            copts += pkgConfig(formatPlatformLib(project, lib), cflags = true)
            lopts += pkgConfig(formatPlatformLib(project, lib), libs = true)
        }

        writer.writeList("compilerOpts", copts)
        writer.writeList("linkerOpts", lopts)
    }

    private fun formatPlatformInclude(path: String): String =
        when (platform) {
            else -> path
        }

    private fun formatPlatformLib(project: Project, path: String): String =
        when (platform) {
            Platform.WINDOWS -> platform.getNativeDependenciesDir(project).resolve("lib").absolutePath.replace('\\', '/') + '/' + path
            else -> path
        }

    private fun getBaseCompilerOpts(project: Project): List<String> =
        when (platform) {
            Platform.LINUX_X86, Platform.LINUX_ARM64 -> listOf("-I/usr/include", "-I/usr/include/${platform.arch.libdir_name}")
            else -> emptyList()
        } + listOfNotNull(platform.getNativeDependenciesDir(project).resolve("include"), System.getenv("SPMS_INCLUDE")).map { path ->
            "-I" + project.file(path).absolutePath.replace('\\', '/')
        }

    private fun getBaseLinkerOpts(project: Project): List<String> =
        when (platform) {
            Platform.LINUX_X86, Platform.LINUX_ARM64 -> listOf("-L/usr/lib", "-L/usr/lib/${platform.arch.libdir_name}")
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

    // https://gist.github.com/micolous/c00b14b2dc321fdb0eab8ad796d71b80
    private fun pkgConfig(
        vararg package_names: String,
        cflags: Boolean = false,
        libs: Boolean = false
    ): List<String> {
        if (platform != Platform.LINUX_X86 && platform != Platform.LINUX_ARM64) {
            if (libs) {
                // return package_names.map { "-l$it" }
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
        process_builder.environment()["PKG_CONFIG_PATH"] = listOfNotNull(platform.arch.PKG_CONFIG_PATH, System.getenv("SPMS_LIB")?.plus("/pkgconfig")).joinToString(":")
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
}
