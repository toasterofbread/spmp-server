package dev.toastbits.spms

import okio.Path
import okio.FileSystem
import dev.toastbits.spms.PLATFORM
import gen.libmpv.LibMpv

expect val FileSystem.Companion.PLATFORM: FileSystem?

expect fun getHostname(): String
expect fun getOSName(): String

expect fun getTempDir(): Path?

expect fun getCacheDir(): Path?

expect fun createLibMpv(): LibMpv?

fun getMachineId(): String? {
    val filesystem: FileSystem = FileSystem.PLATFORM ?: return null
    val id_path: Path = (getTempDir() ?: return null).resolve("spmp_machine_id.txt")

    if (filesystem.exists(id_path)) {
        return filesystem.read(id_path) {
            readUtf8()
        }
    }

    val parent: Path = id_path.parent!!
    if (!filesystem.exists(parent)) {
        filesystem.createDirectories(parent, true)
    }

    val id_length: Int = 8
    val allowed_chars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    val new_id: String = (1..id_length).map { allowed_chars.random() }.joinToString("")

    filesystem.write(id_path) {
        writeUtf8(new_id)
    }

    return new_id
}

fun getDeviceName(): String =
    "${getHostname()} on ${getOSName()}"
