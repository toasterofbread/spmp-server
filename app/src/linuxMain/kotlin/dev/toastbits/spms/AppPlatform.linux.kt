package dev.toastbits.spms

import platform.posix.*
import kotlinx.cinterop.ExperimentalForeignApi
import dev.toastbits.spms.indicator.AppIndicatorImpl
import dev.toastbits.spms.indicator.TrayIndicator

actual fun canOpenProcess(): Boolean = true
@OptIn(ExperimentalForeignApi::class)
actual fun openProcess(command: String, modes: String) { popen(command, modes) }

actual fun canEndProcess(): Boolean = true
actual fun endProcess() { kill(0, SIGTERM) }

actual fun createTrayIndicator(name: String, icon_path: List<String>): TrayIndicator? =
    AppIndicatorImpl(name, icon_path)
