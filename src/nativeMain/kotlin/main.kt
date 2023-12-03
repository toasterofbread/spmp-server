import com.github.ajalt.clikt.core.subcommands
import kotlinx.cinterop.ExperimentalForeignApi
import spms.Command
import spms.client.cli.CommandLineClient
import spms.client.player.PlayerClient
import spms.server.SpMsCommand
import kotlin.system.exitProcess

fun String.toRed() =
    "\u001b[31m$this\u001b[0m"

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    try {
        val command: Command = SpMsCommand().subcommands(CommandLineClient.get(), PlayerClient.get())
        command.main(args)
    }
    catch (e: Throwable) {
        e.printStackTrace()
        exitProcess(1)
    }
}
