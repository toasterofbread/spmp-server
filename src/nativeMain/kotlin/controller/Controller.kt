package controller

import Command
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import controller.modes.Interactive
import controller.modes.Poll
import controller.modes.Run
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import libzmq.ZMQ_DEALER
import spms.DEFAULT_PORT
import spms.SpMpServer
import toRed
import zmq.ZmqSocket

const val SERVER_REPLY_TIMEOUT_MS: Long = 10000

private fun getClientName(): String =
    "SpMs Controller"

internal class SpMsControllerError(message: String): CliktError(message.toRed())

@OptIn(ExperimentalForeignApi::class)
class Controller private constructor(): Command(
    name = "ctrl",
    is_default = true
) {
    private val port: Int by option("-p", "--port").int().default(DEFAULT_PORT).help("The port use when connecting to the server interface")
    private val verbose: Boolean by option("-v", "--verbose").flag()

    override fun run() {
        val mem_scope = MemScope()
        val socket: ZmqSocket = ZmqSocket(mem_scope, ZMQ_DEALER, is_binder = false)

        val context = ControllerModeContext(socket, mem_scope, verbose)
        currentContext.obj = context

        try {
            context.logVerbose("Connecting to port $port...")
            socket.connect("tcp://localhost:$port")

            context.logVerbose("Sending handshake...")

            socket.sendStringMultipart(listOf(getClientName()))

            val reply: List<String>? = socket.recvStringMultipart(SERVER_REPLY_TIMEOUT_MS)

            if (reply == null) {
                throw SpMsControllerError("Server did not respond within timeout (${SERVER_REPLY_TIMEOUT_MS}ms)")
            }

            context.logVerbose("Got handshake reply from server $reply")
        }
        catch (e: Throwable) {
            context.release()
            throw e
        }

        if (currentContext.invokedSubcommand == null) {
            Interactive().parse(emptyList(), currentContext)
        }
    }

    companion object {
        fun get(): Controller =
            Controller().subcommands(Interactive(), Run.get(), Poll())
    }
}
