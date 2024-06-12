package dev.toastbits.spms

import dev.toastbits.spms.indicator.TrayIndicator

actual fun canOpenProcess(): Boolean = false
actual fun openProcess(command: String, modes: String) { TODO() }

actual fun canEndProcess(): Boolean = true
actual fun endProcess() { TODO() }

actual fun createTrayIndicator(name: String, icon_path: List<String>): TrayIndicator? = null
