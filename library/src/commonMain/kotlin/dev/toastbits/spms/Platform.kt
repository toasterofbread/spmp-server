package dev.toastbits.spms

import okio.Path
import okio.FileSystem
import dev.toastbits.spms.PLATFORM
import gen.libmpv.LibMpv

expect val FileSystem.Companion.PLATFORM: FileSystem

expect fun getHostname(): String
expect fun getOSName(): String

expect fun getTempDir(): Path

expect fun getCacheDir(): Path

expect fun createLibMpv(): LibMpv

fun getMachineId(): String {
    val id_path: Path = getTempDir().resolve("spmp_machine_id.txt")

    if (FileSystem.PLATFORM.exists(id_path)) {
        return FileSystem.PLATFORM.read(id_path) {
            readUtf8()
        }
    }

    val parent: Path = id_path.parent!!
    if (!FileSystem.PLATFORM.exists(parent)) {
        FileSystem.PLATFORM.createDirectories(parent, true)
    }

    val id_length: Int = 8
    val allowed_chars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    val new_id: String = (1..id_length).map { allowed_chars.random() }.joinToString("")

    FileSystem.PLATFORM.write(id_path) {
        writeUtf8(new_id)
    }

    return new_id
}

fun getDeviceName(): String =
    "${getHostname()} on ${getOSName()}"
