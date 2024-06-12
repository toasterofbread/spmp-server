package dev.toastbits.spms

import com.github.ajalt.clikt.core.subcommands
import dev.toastbits.spms.Command
import dev.toastbits.spms.client.cli.CommandLineClient
import dev.toastbits.spms.client.player.PlayerClient
import dev.toastbits.spms.server.SpMsCommand
import dev.toastbits.kjna.runtime.KJnaTypedPointer
import dev.toastbits.kjna.runtime.KJnaUtils
import kotlin.system.exitProcess
import kotlinx.coroutines.*
import gen.libmpv.LibMpv
import kjna.struct.mpv_event

fun String.toRed() =
    "\u001b[31m$this\u001b[0m"

fun main(args: Array<String>) {
    try {
        KJnaUtils.setLocale(
            1, // LC_NUMERIC
            "C"
        )
    }
    catch (e: Throwable) {
        RuntimeException("WARNING: Unable to set LC_NUMERIC locale, some interops may not work", e).printStackTrace()
    }

    try {
        val command: Command = SpMsCommand().subcommands(listOfNotNull(CommandLineClient.get(), PlayerClient.get()))
        command.main(args)
    }
    catch (e: Throwable) {
        RuntimeException("Exception in main method, exiting", e).printStackTrace()
        exitProcess(1)
    }
}
