package dev.toastbits.spms

import okio.Path
import okio.Path.Companion.toPath
import okio.FileSystem
import java.net.InetAddress
import java.lang.System.getenv
import dev.toastbits.spms.server.SpMs
import gen.libmpv.LibMpv
import java.io.File

actual val FileSystem.Companion.PLATFORM: FileSystem get() = FileSystem.SYSTEM

actual fun getHostname(): String = InetAddress.getLocalHost().hostName
actual fun getOSName(): String = System.getProperty("os.name")

actual fun getTempDir(): Path =
    when (OS.current) {
        OS.LINUX -> "/tmp/".toPath()
        OS.WINDOWS -> "${getenv("USERPROFILE")!!}/AppData/Local/Temp/".toPath()
    }

actual fun getCacheDir(): Path =
    when (OS.current) {
        OS.LINUX -> "/home/${getenv("USER")!!}/.cache/".toPath().resolve(SpMs.application_name)
        OS.WINDOWS -> "${getenv("USERPROFILE")!!}/AppData/Local/${SpMs.application_name}/cache".toPath()
    }

actual fun createLibMpv(): LibMpv {
    val working_dir: String = System.getProperty("user.dir")
    val lib_dirs: MutableList<String> = (listOf(working_dir) + System.getProperty("java.library.path").split(";")).toMutableList()
    
    val os_name: String = System.getProperty("os.name")
    val lib_name: String =
        when {
            os_name == "Linux" -> "libmpv.so"
            os_name.startsWith("Win") -> {
                lib_dirs.add("C:\\mingw64\\bin")
                "libmpv-2.dll"
            }
            os_name == "Mac OS X" -> TODO()
            else -> throw NotImplementedError(os_name)
        }
    
    var lib_found: Boolean = false
    
    for (dir in lib_dirs) {
        val file: File = File(dir).resolve(lib_name)
        if (file.isFile) {
            gen.libmpv.jextract.LibMpv.setLibraryByPath(file.toPath())
            lib_found = true
            break
        }
    }

    check(lib_found) { "mpv library file '$lib_name' not found in any of the following locations: $lib_dirs" }

    val lib: LibMpv = LibMpv()
    try {
        setlocale(
            1, // LC_NUMERIC
            "C"
        )
    }
    catch (_: Throwable) {
        println("WARNING: Unable to set LC_NUMERIC locale, mpv may not work")
    }
    return lib
}

private enum class OS {
    LINUX, WINDOWS;

    companion object {
        val current: OS get() {
            val os: String = System.getProperty("os.name").lowercase()
            if (os.contains("windows")) {
                return WINDOWS
            }
            if (os.contains("nix") || os.contains("linux")) {
                return LINUX
            }

            throw NotImplementedError(os)
        }
    }
}
