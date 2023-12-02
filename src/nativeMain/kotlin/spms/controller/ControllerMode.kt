package spms.controller

import spms.Command
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import spms.DEFAULT_ADDRESS
import spms.DEFAULT_PORT
import cinterop.zmq.ZmqSocket
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import spms.LocalisedMessageProvider
import spms.controller.modes.Interactive
import spms.localisation.loc

abstract class ControllerMode(name: String, help: LocalisedMessageProvider): Command(name, help = help) {
    private val controller_options by ControllerOptions()

    protected val context: ControllerModeContext by requireObject()
    private var socket_connected: Boolean = false
    protected val socket: ZmqSocket get() {
        check(socket_connected)
        return context.socket
    }

    fun connectSocket() {
        check(!socket_connected)

        try {
            val socket_address: String = "tcp://${controller_options.address}:${controller_options.port}"
            log(currentContext.loc.cli.connectingToSocket(socket_address))
            context.socket.connect("tcp://${controller_options.address}:${controller_options.port}")

            log(currentContext.loc.cli.sending_handshake)

            context.socket.sendStringMultipart(listOf(context.client_name))

            val reply: List<String>? = context.socket.recvStringMultipart(SERVER_REPLY_TIMEOUT_MS)

            if (reply == null) {
                throw SpMsControllerError(currentContext.loc.cli.errServerDidNotRespond(SERVER_REPLY_TIMEOUT_MS))
            }

            log(currentContext.loc.cli.handshake_reply_received)
        }
        catch (e: Throwable) {
            log(currentContext.loc.cli.releasing_socket)
            context.release()
            throw e
        }

        socket_connected = true
    }

    fun releaseSocket() {
        log(currentContext.loc.cli.releasing_socket)
        context.release()
        socket_connected = false
    }

    companion object {
        fun getDefault(): ControllerMode = Interactive()
    }
}

class ControllerOptions: OptionGroup() {
    val address: String by option("-a", "--address").default(DEFAULT_ADDRESS).help { context.loc.cli.option_help_server_address }
    val port: Int by option("-p", "--port").int().default(DEFAULT_PORT).help { context.loc.cli.option_help_server_port }
}
