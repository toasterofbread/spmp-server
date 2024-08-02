package dev.toastbits.spms

import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv
import platform.posix.gethostname
import kotlinx.cinterop.toKString
import kotlinx.cinterop.*

actual fun getTempDir(): Path = "${getenv("USERPROFILE")!!.toKString()}/AppData/Local/Temp/".toPath()

@OptIn(ExperimentalForeignApi::class)
actual fun getHostname(): String? =
    memScoped {
        val str: CPointer<ByteVarOf<Byte>> = allocArray(1024)
        gethostname(str, 1023)
        return@memScoped str.toKString()
    }
