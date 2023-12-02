package spms.controller

import spms.Command
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import spms.controller.modes.Interactive
import spms.controller.modes.Poll
import spms.controller.modes.Run
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import libzmq.ZMQ_DEALER
import toRed
import cinterop.zmq.ZmqSocket
import spms.localisation.loc

const val SERVER_REPLY_TIMEOUT_MS: Long = 10000

private fun getClientName(): String =
    "SpMs CLI"

internal class SpMsControllerError(message: String): CliktError(message.toRed())

@OptIn(ExperimentalForeignApi::class)
class Controller private constructor(): Command(
    name = "ctrl",
    help = { cli.command_help_ctrl },
    is_default = true
) {
    private val controller_options by ControllerOptions()

    override fun run() {
        super.run()

        val mem_scope: MemScope = MemScope()
        val socket: ZmqSocket = ZmqSocket(mem_scope, ZMQ_DEALER, is_binder = false)

        val context: ControllerModeContext = ControllerModeContext(socket, mem_scope, getClientName())
        currentContext.obj = context

        if (currentContext.invokedSubcommand == null) {
            ControllerMode.getDefault().parse(emptyList(), currentContext)
        }
    }

    companion object {
        fun get(): Controller =
            Controller().subcommands(Interactive(), Run.get(), Poll())
    }
}
