package dev.toastbits.spms

import okio.Path
import okio.Path.Companion.toPath
import okio.FileSystem
import java.net.InetAddress
import java.lang.System.getenv
import dev.toastbits.spms.server.SpMs
import gen.libmpv.LibMpv

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
    val lib: LibMpv = LibMpv()
    setlocale(
        1, // LC_NUMERIC
        "C"
    )
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
