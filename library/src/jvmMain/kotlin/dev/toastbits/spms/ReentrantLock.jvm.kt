@file:OptIn(io.ktor.utils.io.InternalAPI::class)
package dev.toastbits.spms

import io.ktor.utils.io.locks.withLock

actual class ReentrantLock {
    private val lock = io.ktor.utils.io.locks.ReentrantLock()
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    actual inline fun <T> withLock(action: () -> T): T = lock.withLock(action)
}