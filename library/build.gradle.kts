@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.gradle.internal.os.OperatingSystem
import dev.toastbits.kjna.c.CType

val GENERATED_FILE_PREFIX: String = "// Generated on build in build.gradle.kts\n"

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("plugin.publishing")
    id("dev.toastbits.kjna")
}

kotlin {
    jvmToolchain(22)

    val native_targets: MutableList<KotlinNativeTarget> = mutableListOf()

    for (platform in Platform.supported) {
        when (platform) {
            Platform.JVM -> {
                jvm {
                    withJava()
                }
                continue
            }
            Platform.LINUX_X86 -> native_targets.add(linuxX64().apply { configureNativeTarget(platform) })
            Platform.LINUX_ARM64 -> native_targets.add(linuxArm64().apply { configureNativeTarget(platform) })
            Platform.WINDOWS -> native_targets.add(mingwX64().apply { configureNativeTarget(platform) })
            Platform.OSX_X86 -> native_targets.add(macosX64().apply { configureNativeTarget(platform) })
            Platform.OSX_ARM -> native_targets.add(macosArm64().apply { configureNativeTarget(platform) })
        }
    }

    kjna {
        generate {
            override_jextract_loader = true

            packages(native_targets) {
                add("gen.libmpv") {
                    enabled = !BuildFlag.DISABLE_MPV.isSet(project)

                    addHeader("mpv/client.h", "LibMpv")
                    libraries = listOf("mpv")

                    if (OperatingSystem.current().isWindows()) {
                        overrides.overrideTypedefType("size_t", CType.Primitive.LONG)
                    }
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.experimental.ExperimentalNativeApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")

                enableLanguageFeature("ExpectActualClasses")
            }
        }

        val ytm_version: String = extra["ytm.version"] as String

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

                val okio_version: String = extra["okio.version"] as String
                implementation("com.squareup.okio:okio:$okio_version")

                implementation("dev.toastbits.ytmkt:ytmkt:$ytm_version")

                val kjna_version: String = rootProject.extra["kjna.version"] as String
                implementation("dev.toastbits.kjna:runtime:$kjna_version")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("net.java.dev.jna:jna:5.14.0")
                implementation("org.zeromq:jeromq:0.6.0")

                val ktor_version: String = extra["ktor.version"] as String
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-cio:$ktor_version")
            }
        }
    }
}

enum class BuildFlag {
    DISABLE_MPV, DISABLE_APPINDICATOR;

    fun isSet(project: Project): Boolean {
        return project.hasProperty(name)
    }
}

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
    JVM, LINUX_X86, LINUX_ARM64, WINDOWS, OSX_X86, OSX_ARM;

    val identifier: String get() =
        when (this) {
            JVM -> "jvm"
            LINUX_X86 -> "linuxX64"
            LINUX_ARM64 -> "linuxArm64"
            WINDOWS -> "mingwX64"
            OSX_X86 -> "macosX64"
            OSX_ARM -> "macosArm64"
        }

    val gen_file_extension: String
        get() = when (this) {
            JVM -> "jvm"
            LINUX_X86, LINUX_ARM64 -> "linux"
            WINDOWS -> "windows"
            OSX_X86, OSX_ARM -> "osx"
        }

    val arch: Arch
        get() = when (this) {
            JVM -> Arch.X86_64
            LINUX_X86, WINDOWS, OSX_X86 -> Arch.X86_64
            LINUX_ARM64, OSX_ARM -> Arch.ARM64
        }

    val is_native: Boolean
        get() = this != JVM

    val is_linux: Boolean
        get() = this == LINUX_X86 || this == LINUX_ARM64

    fun getNativeDependenciesDir(project: Project) =
        project.file("src/nativeInterop/$identifier")

    companion object {
        val supported: List<Platform> = listOf(
            JVM, LINUX_X86, LINUX_ARM64, WINDOWS
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
    LIBZMQ, LIBAPPINDICATOR;

    val identifier: String get() = name.lowercase()

    fun shouldInclude(project: Project, platform: Platform): Boolean =
        when (this) {
            LIBAPPINDICATOR -> platform.is_linux && !BuildFlag.DISABLE_APPINDICATOR.isSet(project)
            else -> true
        }

    fun getDependentFiles(): List<String> =
        when (this) {
            else -> emptyList()
        }

    fun getBinaryDependencies(platform: Platform): List<String> =
        when (this) {
            LIBZMQ ->
                if (platform == Platform.WINDOWS) listOf("libzmq-mt-4_3_5.dll")
                else emptyList()
            else -> emptyList()
        }

    private fun getPackageName(): String =
        when (this) {
            LIBZMQ -> "libzmq"
            LIBAPPINDICATOR -> "appindicator3-0.1"
        }

    fun configureCinterop(
        project: Project,
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

                if (file == null) {
                    println("WARNING: Could not find header file '$path' for platform $platform in $cflags or ${deps_directory.resolve("include")} or $default_include_dirs")
                    return
                }
            }

            settings.header(file)
        }

        when (this) {
            LIBZMQ -> {
                addHeaderFile("zmq.h")
                addHeaderFile("zmq_utils.h")
                settings.compilerOpts("-DZMQ_BUILD_DRAFT_API=1")
            }
            LIBAPPINDICATOR -> {
                addHeaderFile("libappindicator3-0.1/libappindicator/app-indicator.h")
            }
        }

        val libs_dir: File = deps_directory.resolve("lib")

        val lib_filenames: List<String> =
            when (this) {
                LIBZMQ -> listOf("libzmq.a")
                else -> emptyList()
            }

        for (filename in lib_filenames) {
            val lib_file: File = libs_dir.resolve(filename)
            if (!lib_file.isFile) {
                println("WARNING: Could not find library file '$lib_file' in $libs_dir")
            }
        }

        val def_file: File = project.file("build/def/${name}.${platform.identifier}.def")
        if (!def_file.exists()) {
            def_file.ensureParentDirsCreated()
            def_file.createNewFile()
        }

        val linker_opts: MutableList<String> = pkgConfig(platform, deps_directory, getPackageName(), libs = true).toMutableList()

        when (this) {
            LIBZMQ -> {
                if (!platform.is_linux) {
                    linker_opts.addAll(listOf("-lssp", "-static", "-static-libgcc", "-static-libstdc++", "-lgcc", "-lstdc++"))
                }
            }
            else -> {}
        }

        def_file.writeText("""
            staticLibraries = ${lib_filenames.joinToString(" ")}
            libraryPaths = ${libs_dir.absolutePath}
            linkerOpts = ${linker_opts.joinToString(" ")}
        """.trimIndent())

        settings.defFile(def_file)
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

fun KotlinNativeTarget.configureNativeTarget(platform: Platform) {
    val deps_directory: File = platform.getNativeDependenciesDir(project)

    compilations.getByName("main") {
        cinterops {
            for (library in CinteropLibraries.values()) {
                if (!library.shouldInclude(project, platform)) {
                    continue
                }

                val library_name: String = library.name.lowercase()

                create(library_name) {
                    packageName(library_name)
                    library.configureCinterop(project, platform, this, deps_directory)
                }
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

for (platform in Platform.supported) {
    fun String.getFile(suffix: String? = null): File =
        project.file("src/" + if (suffix == null) this.replace(".kt", ".gen.kt") else (this + suffix))

    val configure_library_dependent_files_task by tasks.register("configureLibraryDependentFiles${platform.identifier}") {
        outputs.upToDateWhen { false }

        for (library in CinteropLibraries.values()) {
            for (path in library.getDependentFiles()) {
                // outputs.file(path.getFile())
                inputs.file(path.getFile(".enabled"))
                inputs.file(path.getFile(".disabled"))
            }
        }

        doLast {
            for (library in CinteropLibraries.values()) {
                val enabled: Boolean = library.shouldInclude(project, platform)

                for (path in library.getDependentFiles()) {
                    if (path.getFile("").isFile) {
                        continue
                    }

                    val out_file: File = path.getFile()
                    val in_file: File = path.getFile(if (enabled) ".enabled" else ".disabled")

                    out_file.writer().use { writer ->
                        writer.write(GENERATED_FILE_PREFIX)

                        in_file.reader().use { reader ->
                            reader.transferTo(writer)
                        }
                    }
                }
            }
        }
    }

    val compile_task: Task =
        tasks.getByName("compileKotlin" + platform.identifier.capitalised()) {
            dependsOn(configure_library_dependent_files_task)
        }

    if (compile_task !is KotlinNativeCompile) {
        continue
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

fun String.capitalised(lowercase_other_chars: Boolean = false) =
    if (lowercase_other_chars) this.first().uppercase() + this.drop(1).lowercase()
    else this.first().uppercase() + this.drop(1)
