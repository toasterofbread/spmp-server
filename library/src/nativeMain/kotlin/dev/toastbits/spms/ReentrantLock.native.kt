package dev.toastbits.spms

import kotlinx.atomicfu.locks.withLock

actual class ReentrantLock {
    private val lock = kotlinx.atomicfu.locks.ReentrantLock()
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    actual inline fun <T> withLock(action: () -> T): T = lock.withLock(action)
}
