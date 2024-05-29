package dev.toastbits.spms.client.cli

import dev.toastbits.spms.zmq.ZmqSocket
import dev.toastbits.spms.zmq.ZmqSocketType
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.parse
import dev.toastbits.spms.Command
import dev.toastbits.spms.client.ClientOptions
import dev.toastbits.spms.client.cli.modes.Interactive
import dev.toastbits.spms.client.cli.modes.Poll
import dev.toastbits.spms.client.cli.modes.Run
import dev.toastbits.spms.toRed
import kotlin.time.Duration

val SERVER_REPLY_TIMEOUT: Duration = with (Duration) { 2.seconds }

private fun getClientName(): String =
    "SpMs CLI"

internal class SpMsCommandLineClientError(message: String): CliktError(message.toRed())

class CommandLineClient private constructor(): Command(
    name = "ctrl",
    help = { cli.command_help_ctrl },
    is_default = true
) {
    private val client_options by ClientOptions()

    override fun run() {
        super.run()

        val socket: ZmqSocket = ZmqSocket(ZmqSocketType.DEALER, is_binder = false)

        val context: CommandLineModeContext = CommandLineModeContext(socket, getClientName())
        currentContext.obj = context

        if (currentContext.invokedSubcommand == null) {
            CommandLineClientMode.getDefault().parse(emptyList())
        }
    }

    companion object {
        fun get(): CommandLineClient =
            CommandLineClient().subcommands(Interactive(), Run.get(), Poll())
    }
}
