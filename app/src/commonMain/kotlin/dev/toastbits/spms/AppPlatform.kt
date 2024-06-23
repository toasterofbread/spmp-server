package dev.toastbits.spms

import dev.toastbits.spms.indicator.TrayIndicator

expect fun canOpenProcess(): Boolean
expect fun openProcess(command: String, modes: String)

expect fun canEndProcess(): Boolean
expect fun endProcess()

expect fun createTrayIndicator(name: String, icon_path: List<String>): TrayIndicator?
