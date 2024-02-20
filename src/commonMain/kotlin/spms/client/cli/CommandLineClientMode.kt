package spms.client.cli

import cinterop.zmq.ZmqSocket
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import spms.Command
import spms.LocalisedMessageProvider
import spms.client.ClientOptions
import spms.client.cli.modes.Interactive
import spms.localisation.loc
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientHandshake
import spms.socketapi.shared.SpMsClientType

abstract class CommandLineClientMode(
    name: String,
    help: LocalisedMessageProvider?,
    hidden: Boolean = false,
    help_tags: Map<String, String> = emptyMap()
): Command(name, help = help, hidden = hidden, help_tags = help_tags) {
    private val client_options by ClientOptions()

    protected val context: CommandLineModeContext by requireObject()
    private var socket_connected: Boolean = false
    protected val socket: ZmqSocket get() {
        check(socket_connected)
        return context.socket
    }

    fun connectSocket(type: SpMsClientType = SpMsClientType.COMMAND_LINE_ACTION, handshake_actions: List<String>? = null): List<String> {
        check(!socket_connected)

        try {
            val socket_address: String = client_options.getAddress("tcp")
            log(currentContext.loc.cli.connectingToSocket(socket_address))
            context.socket.connect(socket_address)

            log(currentContext.loc.cli.sending_handshake)

            val handshake: SpMsClientHandshake = SpMsClientHandshake(
                name = context.client_name,
                type = type,
                machine_id = SpMs.getMachineId(),
                language = currentContext.loc.language.name,
                actions = handshake_actions
            )
            context.socket.sendStringMultipart(listOf(Json.encodeToString(handshake)))

            val reply: List<String>? = context.socket.recvStringMultipart(SERVER_REPLY_TIMEOUT_MS)

            if (reply == null) {
                throw SpMsCommandLineClientError(currentContext.loc.cli.errServerDidNotRespond(SERVER_REPLY_TIMEOUT_MS))
            }

            log(currentContext.loc.cli.handshake_reply_received + " " + reply.toString())
            socket_connected = true

            return reply
        }
        catch (e: Throwable) {
            log(currentContext.loc.cli.releasing_socket)
            context.release()
            throw e
        }
    }

    fun releaseSocket() {
        log(currentContext.loc.cli.releasing_socket)
        context.release()
        socket_connected = false
    }

    companion object {
        fun getDefault(): CommandLineClientMode = Interactive()
    }
}

