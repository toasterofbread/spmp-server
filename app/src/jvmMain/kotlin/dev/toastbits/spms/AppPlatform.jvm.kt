package dev.toastbits.spms

import dev.toastbits.spms.indicator.TrayIndicator
import dev.toastbits.spms.indicator.AppIndicatorImpl

actual fun canOpenProcess(): Boolean = true
actual fun openProcess(command: String, modes: String) { Runtime.getRuntime().exec(command) }

actual fun canEndProcess(): Boolean = false
actual fun endProcess() { TODO() }

actual fun createTrayIndicator(name: String, icon_path: List<String>): TrayIndicator? =
    when (OS.current) {
        OS.LINUX -> AppIndicatorImpl(name, icon_path)
        OS.WINDOWS -> null
    }
