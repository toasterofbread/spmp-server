package dev.toastbits.spms

expect class ReentrantLock() {
    inline fun <T> withLock(action: () -> T): T
}
