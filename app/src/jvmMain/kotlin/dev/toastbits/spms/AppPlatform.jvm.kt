package dev.toastbits.spms

actual fun canOpenProcess(): Boolean = true
actual fun openProcess(command: String, modes: String) { Runtime.getRuntime().exec(command) }

actual fun canEndProcess(): Boolean = false
actual fun endProcess() { TODO() }
