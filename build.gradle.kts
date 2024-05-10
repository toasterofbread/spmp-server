@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootPlugin.Companion.kotlinBinaryenExtension
import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

val FLAG_LINK_STATIC: String = "linkStatic"
val GENERATED_FILE_PREFIX: String = "// Generated on build in build.gradle.kts\n"

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    maven("https://jitpack.io")
}

val platform_specific_files: List<String> = listOf(
    "cinterop/indicator/TrayIndicatorImpl.kt",
    "spms/Platform.kt",
    "spms/server/SpMsMediaSession.kt"
)

enum class Arch {
    X86_64, ARM64;

    val identifier: String get() =
        when (this) {
            X86_64 -> "x86_64"
            ARM64 -> "arm64"
        }

    val libdir_name: String get() =
        when (this) {
            X86_64 -> "x86_64-linux-gnu"
            ARM64 -> "aarch64-linux-gnu"
        }

    val PKG_CONFIG_PATH: String get() = "/usr/lib/$libdir_name/pkgconfig"

    companion object {
        fun byName(name: String): Arch =
            when (name.lowercase()) {
                "x86_64", "amd64" -> X86_64
                "aarch64", "arm64" -> ARM64
                else -> throw GradleException("Unsupported CPU architecture '$name'")
            }

        fun getCurrent(): Arch =
            byName(System.getProperty("os.arch"))

        fun getTarget(project: Project): Arch {
            val target_override: String? =
                project.findProperty("SPMS_ARCH")?.toString()
                ?: System.getenv("SPMS_ARCH")

            if (target_override == null) {
                return getCurrent()
            }

            return byName(target_override)
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

    val is_linux: Boolean
        get() = this == LINUX_X86 || this == LINUX_ARM64

    fun getNativeDependenciesDir(project: Project) =
        project.file("src/nativeInterop/$identifier")

    companion object {
        val supported: List<Platform> = listOf(
            LINUX_X86, LINUX_ARM64, WINDOWS
        )

        fun byName(name: String, arch: Arch): Platform =
            if (name.lowercase() == "linux")
                when (arch) {
                    Arch.X86_64 -> LINUX_X86
                    Arch.ARM64 -> LINUX_ARM64
                }
            else if (name.lowercase().startsWith("windows") && arch == Arch.X86_64) WINDOWS
            else throw GradleException("Unsupported host OS and architecture '$name' ($arch)")

        fun getCurrent(arch: Arch = Arch.getCurrent()): Platform =
            byName(System.getProperty("os.name"), arch)

        fun getTarget(project: Project): Platform {
            val arch: Arch = Arch.getTarget(project)
            val target_override: String? =
                project.findProperty("SPMS_OS")?.toString()
                ?: System.getenv("SPMS_OS")

            if (target_override == null) {
                return getCurrent(arch)
            }

            return byName(target_override, arch)
        }
    }
}

enum class CinteropLibraries {
    LIBMPV, LIBZMQ, LIBAPPINDICATOR;

    val identifier: String get() = name.lowercase()

    fun includeOnPlatform(platform: Platform): Boolean =
        when (this) {
            LIBAPPINDICATOR -> platform.is_linux
            else -> true
        }

    fun getBinaryDependencies(platform: Platform): List<String> =
        when (this) {
            LIBMPV ->
                if (platform == Platform.WINDOWS) listOf("libmpv-2.dll")
                else emptyList()
            else -> emptyList()
        }

    private fun getPackageName(): String =
        when (this) {
            LIBMPV -> "mpv"
            LIBZMQ -> "libzmq"
            LIBAPPINDICATOR -> "appindicator3-0.1"
        }

    fun configure(
        platform: Platform,
        settings: DefaultCInteropSettings,
        deps_directory: File
    ) {
        val cflags: List<String> = pkgConfig(platform, deps_directory, getPackageName(), cflags = true)
        settings.compilerOpts(cflags)

        val default_include_dirs: List<File> =
            if (platform.is_linux) listOf("/usr/include", "/usr/include/${platform.arch.libdir_name}").map { File(it) }
            else emptyList()

        fun addHeaderFile(path: String) {
            var file: File? = null

            for (dir in listOf(deps_directory.resolve("include")) + default_include_dirs) {
                if (dir.resolve(path).exists()) {
                    file = dir.resolve(path)
                    break
                }
            }

            if (file == null) {
                val first_part: String = path.split("/", limit = 2).first()

                for (flag in cflags) {
                    if (!flag.startsWith("-I/") || !flag.endsWith("/" + first_part)) {
                        continue
                    }

                    file = File(flag.drop(2).dropLast(first_part.length)).resolve(path).takeIf { it.exists() }
                    if (file != null) {
                        break
                    }
                }
            }

            if (file == null) {
                // println("Could not find header file '$path' for platform $platform in $cflags")
                return
            }

            settings.header(file)
        }

        when (this) {
            LIBMPV -> {
                addHeaderFile("mpv/client.h")
            }
            LIBZMQ -> {
                addHeaderFile("zmq.h")
                addHeaderFile("zmq_utils.h")
                settings.compilerOpts("-DZMQ_BUILD_DRAFT_API=1")
            }
            LIBAPPINDICATOR -> {
                addHeaderFile("libappindicator3-0.1/libappindicator/app-indicator.h")
            }
        }
    }

    fun configureExecutable(
        platform: Platform,
        static: Boolean,
        settings: Executable,
        deps_directory: File
    ) {
        if (platform.is_linux) {
            settings.linkerOpts(pkgConfig(platform, deps_directory, getPackageName(), libs = true))

            if (static) {
                when (this) {
                    LIBZMQ -> {
                        val zmq: File = deps_directory.resolve("lib/libzmq.a")
                        settings.linkerOpts("-l:${zmq.absolutePath}")
                        settings.linkerOpts.remove("-lzmq")
                    }
                    else -> {}
                }
            }

            return
        }

        fun addLib(filename: String) {
            settings.linkerOpts(deps_directory.resolve("lib").resolve(filename).absolutePath.replace("\\", "/"))
        }

        when (this) {
            LIBMPV -> {
                addLib("libmpv.dll.a")
            }
            LIBZMQ -> {
                addLib("libzmq-mt-4_3_5.lib")
                settings.linkerOpts("-lssp", "-static", "-static-libgcc", "-static-libstdc++", "-lgcc", "-lstdc++")
            }
            LIBAPPINDICATOR -> throw IllegalAccessException()
        }
    }

    // https://gist.github.com/micolous/c00b14b2dc321fdb0eab8ad796d71b80
    private fun pkgConfig(
        platform: Platform,
        deps_directory: File,
        vararg package_names: String,
        cflags: Boolean = false,
        libs: Boolean = false
    ): List<String> {
        if (Platform.getCurrent() == Platform.WINDOWS) {
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
        process_builder.environment()["PKG_CONFIG_PATH"] =
            listOfNotNull(
                deps_directory.resolve("pkgconfig").takeIf { it.isDirectory }?.absolutePath,
                platform.arch.PKG_CONFIG_PATH,
                System.getenv("SPMS_LIB")?.plus("/pkgconfig")
            ).joinToString(":")
        process_builder.environment()["PKG_CONFIG_ALLOW_SYSTEM_LIBS"] = "1"

        val process: Process = process_builder.start()
        process.waitFor(10, TimeUnit.SECONDS)

        if (process.exitValue() != 0) {
            // println("pkg-config failed for platform $platform with package_names: ${package_names.toList()}\n" + process.errorStream.bufferedReader().use { it.readText() })
            return emptyList()
        }

        return process.inputStream.bufferedReader().use { reader ->
            reader.readText().split(" ").mapNotNull { it.trim().takeIf { line ->
                line.isNotBlank() && line != "-I/usr/include/x86_64-linux-gnu" && line != "-I/usr/include/aarch64-linux-gnu"
            } }
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

    val static: Boolean = project.hasProperty(FLAG_LINK_STATIC)
    val deps_directory: File = platform.getNativeDependenciesDir(project)

    native_target.apply {
        compilations.getByName("main") {
            cinterops {
                for (library in CinteropLibraries.values()) {
                    if (!library.includeOnPlatform(platform)) {
                        continue
                    }

                    val library_name: String = library.name.lowercase()

                    create(library_name) {
                        packageName(library_name)
                        library.configure(platform, this, deps_directory)
                    }
                }
            }
        }

        binaries {
            executable {
                baseName = "spms-${platform.identifier}"
                entryPoint = "main"

                if (deps_directory.resolve("lib").isDirectory) {
                    linkerOpts("-L" + deps_directory.resolve("lib").absolutePath)
                }

                for (library in CinteropLibraries.values()) {
                    if (!library.includeOnPlatform(platform)) {
                        continue
                    }
                    library.configureExecutable(platform, static, this, deps_directory)
                }
            }
        }
    }

    sourceSets.getByName(platform.identifier + "Main") {
        languageSettings.optIn("kotlin.experimental.ExperimentalNativeApi")
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")

        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

            val okio_version: String = extra["okio.version"] as String
            implementation("com.squareup.okio:okio:$okio_version")

            val clikt_version: String = extra["clikt.version"] as String
            implementation("com.github.ajalt.clikt:clikt:$clikt_version")

            val mediasession_version: String = extra["mediasession.version"] as String
            val ytm_version: String = extra["ytm.version"] as String

            when (platform) {
                Platform.LINUX_X86 -> {
                    implementation("dev.toastbits.mediasession:library-linuxx64:$mediasession_version")
                    implementation("dev.toastbits.ytmkt:ytmkt-linuxx64:$ytm_version")
                }
                Platform.LINUX_ARM64 -> {
                    implementation("dev.toastbits.ytmkt:ytmkt-linuxarm64:$ytm_version")
                }
                Platform.WINDOWS -> {
                    implementation("dev.toastbits.mediasession:library-mingwx64:$mediasession_version")
                    implementation("dev.toastbits.ytmkt:ytmkt-mingwx64:$ytm_version")
                }
                else -> {}
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

for (platform in Platform.supported) {
    val print_target_task by tasks.register("printTarget${platform.identifier}") {
        doLast {
            println("Building spmp-server for target $platform")
        }
    }

    val configure_platform_files_task by tasks.register("configurePlatformFiles${platform.identifier}") {
        outputs.upToDateWhen { false }

        fun String.getFile(suffix: String? = null): File =
            project.file("src/commonMain/kotlin/" + if (suffix == null) this.replace(".kt", ".gen.kt") else (this + suffix))

        for (path in platform_specific_files) {
            outputs.file(path.getFile())

            val input_files: List<File> =
                Platform.supported.map { path.getFile('.' + it.name.lowercase()) } + listOf(path.getFile(".other"))
            for (file in input_files) {
                if (file.isFile) {
                    inputs.file(file)
                }
            }
        }

        doLast {
            for (path in platform_specific_files) {
                val out_file: File = path.getFile()

                for (platform_file in listOf(
                    // OS and arch
                    path.getFile('.' + platform.gen_file_extension + '.' + platform.arch.identifier),
                    // OS only
                    path.getFile('.' + platform.gen_file_extension),
                    // Arch only
                    path.getFile('.' + platform.arch.identifier),
                    // Other
                    path.getFile(".other")
                )) {
                    if (platform_file.isFile) {
                        out_file.writer().use { writer ->
                            writer.write(GENERATED_FILE_PREFIX)

                            platform_file.reader().use { reader ->
                                reader.transferTo(writer)
                            }
                        }
                        break
                    }
                }
            }
        }
    }

    val compile_task = tasks.getByName<KotlinNativeCompile>("compileKotlin" + platform.identifier.capitalised()) {
        dependsOn(print_target_task)
        dependsOn("bundleIcon")
        dependsOn(configure_platform_files_task)
    }

    val check_dependencies_task by tasks.register("checkDependencies${platform.identifier}") {
        doFirst {
            @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
            val compilation = (compile_task.compilation as org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo.TCS).compilation as KotlinNativeCompilation

            for (executable in compilation.target.binaries) {
                for (opt in executable.linkerOpts) {
                    if (!opt.startsWith("-l:")) {
                        continue
                    }

                    val file: File = File(opt.drop(3))
                    check(file.isFile) { "Static library doesn't exist '${file.absolutePath}'" }
                }
            }
        }
    }

    compile_task.dependsOn(check_dependencies_task)

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
            for (filename in library.getBinaryDependencies(platform)) {
                val file: File = bin_directory.resolve(filename)
                if (!file.isFile)  {
                    continue
                }

                file.copyTo(target_directory.resolve(filename), overwrite = true)
            }
        }
    }
}

fun String.capitalised() = this.first().uppercase() + this.drop(1).lowercase()
