package dev.toastbits.spms

import kotlinx.cinterop.*
import dev.toastbits.spms.native.safeToKString
import dev.toastbits.spms.server.SpMs
import platform.posix.getenv
import okio.Path
import okio.Path.Companion.toPath
import okio.FileSystem
import gen.libmpv.LibMpv

actual val FileSystem.Companion.PLATFORM: FileSystem get() = FileSystem.SYSTEM

actual fun getOSName(): String {
    val os: String =
        when (Platform.osFamily) {
            OsFamily.MACOSX -> "OSX"
            OsFamily.IOS -> "iOS"
            OsFamily.LINUX -> "Linux"
            OsFamily.WINDOWS -> "Windows"
            OsFamily.ANDROID -> "Android"
            OsFamily.WASM -> "Wasm"
            OsFamily.TVOS -> "TV"
            OsFamily.WATCHOS -> "WatchOS"
            OsFamily.UNKNOWN -> "Unknown"
        }

    val architecture: String =
        when (Platform.cpuArchitecture) {
            CpuArchitecture.X86 -> "x86"
            CpuArchitecture.X64 -> "x86-64"
            CpuArchitecture.UNKNOWN -> "Unknown arch"
            else -> Platform.cpuArchitecture.name
        }

    return "$os ($architecture)"
}

actual fun getCacheDir(): Path =
    when (Platform.osFamily) {
        OsFamily.LINUX -> "/home/${getenv("USER")!!.toKString()}/.cache/".toPath().resolve(SpMs.application_name)
        OsFamily.WINDOWS -> "${getenv("USERPROFILE")!!.toKString()}/AppData/Local/${SpMs.application_name}/cache".toPath()
        else -> throw NotImplementedError(Platform.osFamily.name)
    }

actual fun createLibMpv(): LibMpv = LibMpv()
