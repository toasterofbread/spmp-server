package dev.toastbits.spms

import platform.posix.*
import kotlinx.cinterop.ExperimentalForeignApi

actual fun canOpenProcess(): Boolean = true
@OptIn(ExperimentalForeignApi::class)
actual fun openProcess(command: String, modes: String) { popen(command, modes) }

actual fun canEndProcess(): Boolean = true
actual fun endProcess() { kill(0, SIGTERM) }
