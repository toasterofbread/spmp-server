package dev.toastbits.spms

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.main
import dev.toastbits.spms.Command
import dev.toastbits.spms.client.cli.CommandLineClient
import dev.toastbits.spms.client.player.PlayerClient
import dev.toastbits.spms.server.SpMsCommand
import dev.toastbits.kjna.runtime.KJnaTypedPointer
import kotlin.system.exitProcess
import kotlinx.coroutines.*
import gen.libmpv.LibMpv
import kjna.struct.mpv_event

fun String.toRed() =
    "\u001b[31m$this\u001b[0m"

fun main(args: Array<String>) {
    try {
        val command: Command = SpMsCommand().subcommands(listOfNotNull(CommandLineClient.get(), PlayerClient.get()))
        command.main(args)
    }
    catch (e: Throwable) {
        RuntimeException("Exception in main method, exiting", e).printStackTrace()
        exitProcess(1)
    }
}
