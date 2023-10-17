package controller

import Command
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
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
import toRed
import zmq.ZmqSocket

private const val SERVER_REPLY_TIMEOUT_MS: Long = 1000

private fun getClientName(): String =
    "SpMs Controller"

internal class SpMsControllerError(message: String): CliktError(message.toRed())

@OptIn(ExperimentalForeignApi::class)
class SpMsController private constructor(): Command(
    name = "ctrl",
    is_default = true
) {
    private val port: Int by option().int().default(DEFAULT_PORT).help("The port use when connecting to the server interface")

    override fun run() {
        val mem_scope = MemScope()
        val socket: ZmqSocket = ZmqSocket(mem_scope, ZMQ_DEALER, is_binder = false)

        val context = SpMpControllerCommandContext(socket, mem_scope)
        currentContext.obj = SpMpControllerCommandContext(socket, mem_scope)

        try {
            println("Connecting to port $port...")
            socket.connect("tcp://localhost:$port")

            println("Sending handshake...")
            socket.sendStringMultipart(listOf(getClientName()))

            val reply: List<String>? = socket.recvStringMultipart(SERVER_REPLY_TIMEOUT_MS)

            if (reply == null) {
                throw SpMsControllerError("Server did not respond within timeout (${SERVER_REPLY_TIMEOUT_MS}ms)")
            }

            println("Got handshake reply from server $reply")
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
        fun get(): SpMsController =
            SpMsController().subcommands(Interactive(), Run(), Poll())
    }
}
