package spms.client.cli

import cinterop.zmq.ZmqSocket
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import libzmq.ZMQ_DEALER
import spms.Command
import spms.client.ClientOptions
import spms.client.cli.modes.Interactive
import spms.client.cli.modes.Poll
import spms.client.cli.modes.Run
import toRed

const val SERVER_REPLY_TIMEOUT_MS: Long = 2000

private fun getClientName(): String =
    "SpMs CLI"

internal class SpMsCommandLineClientError(message: String): CliktError(message.toRed())

@OptIn(ExperimentalForeignApi::class)
class CommandLineClient private constructor(): Command(
    name = "ctrl",
    help = { cli.command_help_ctrl },
    is_default = true
) {
    private val client_options by ClientOptions()

    override fun run() {
        super.run()

        val mem_scope: MemScope = MemScope()
        val socket: ZmqSocket = ZmqSocket(mem_scope, ZMQ_DEALER, is_binder = false)

        val context: CommandLineModeContext = CommandLineModeContext(socket, mem_scope, getClientName())
        currentContext.obj = context

        if (currentContext.invokedSubcommand == null) {
            CommandLineClientMode.getDefault().parse(emptyList(), currentContext)
        }
    }

    companion object {
        fun get(): CommandLineClient =
            CommandLineClient().subcommands(Interactive(), Run.get(), Poll())
    }
}
