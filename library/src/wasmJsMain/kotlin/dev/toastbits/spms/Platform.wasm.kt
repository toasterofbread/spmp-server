package dev.toastbits.spms

import okio.FileSystem
import okio.Path
import gen.libmpv.LibMpv

actual val FileSystem.Companion.PLATFORM: FileSystem? get() = null

actual fun getHostname(): String = "TODO"
actual fun getOSName(): String = "TODO"

actual fun getTempDir(): Path? = null

actual fun getCacheDir(): Path? = null

actual fun createLibMpv(): LibMpv? = null
