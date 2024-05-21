package dev.toastbits.spms.mpv

import com.sun.jna.Library
import com.sun.jna.Native
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

interface PosixInterface: Library {
    fun setlocale(category: Int, locale: String): String
    fun pthread_mutexattr_setrobust()
}

// val posix: PosixInterface = Native.load("libmpv.so", PosixInterface::class.java)
val posix: PosixInterface = Native.load("c", PosixInterface::class.java)

fun setlocale(category: Int, locale: String) {
    posix.setlocale(category, locale)

    // runBlocking {
    //     delay(2000)
    // }
}


