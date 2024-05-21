package dev.toastbits.spms

actual fun canOpenProcess(): Boolean = false
actual fun openProcess(command: String, modes: String) { TODO() }

actual fun canEndProcess(): Boolean = true
actual fun endProcess() { TODO() }
