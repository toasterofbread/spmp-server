package dev.toastbits.spms

import com.sun.jna.Library
import com.sun.jna.Native

private interface PosixInterface: Library {
    fun setlocale(category: Int, locale: String): String
    fun pthread_mutexattr_setrobust()
}

private val posix: PosixInterface by lazy { Native.load("c", PosixInterface::class.java) }

fun setlocale(category: Int, locale: String) {
    posix.setlocale(category, locale)
}
