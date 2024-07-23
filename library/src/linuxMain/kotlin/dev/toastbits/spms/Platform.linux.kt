package dev.toastbits.spms

import okio.Path
import okio.Path.Companion.toPath
import kotlinx.cinterop.*
import platform.posix.gethostname

actual fun getTempDir(): Path? = "/tmp/".toPath()

@OptIn(ExperimentalForeignApi::class)
actual fun getHostname(): String =
    memScoped {
        val str: CPointer<ByteVarOf<Byte>> = allocArray(1024)
        gethostname(str, 1023UL)
        return@memScoped str.toKString()
    }
