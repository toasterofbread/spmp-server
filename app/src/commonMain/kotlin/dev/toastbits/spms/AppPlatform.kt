package dev.toastbits.spms

expect fun canOpenProcess(): Boolean
expect fun openProcess(command: String, modes: String)

expect fun canEndProcess(): Boolean
expect fun endProcess()
