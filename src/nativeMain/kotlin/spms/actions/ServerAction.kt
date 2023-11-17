package spms.actions

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import libzmq.ZMQ_NOBLOCK
import spms.SERVER_EXPECT_REPLY_CHAR
import spms.SpMpServer
import zmq.ZmqSocket
import kotlin.system.getTimeMillis

sealed class ServerAction(
    val identifier: String,
    val help: String,
    val parameters: List<Parameter>
) {
    data class Parameter(val type: Type, val required: Boolean, val identifier: String, val help: String) {
        enum class Type {
            String, Int, Float
        }
    }
    protected inner class Context(private val parameter_values: List<JsonPrimitive>) {
        fun getParameterValue(identifier: String): JsonPrimitive? {
            val index: Int = parameters.indexOfFirst { it.identifier == identifier }
            val parameter: Parameter = parameters[index]

            val value: JsonPrimitive? = parameter_values.getOrNull(index)
            if (value == null && parameter.required) {
                throw InvalidParameterException(parameter, value)
            }

            return value
        }
    }
    class InvalidParameterException(val parameter: Parameter, val value: JsonPrimitive?): RuntimeException()

    open fun formatResult(result: JsonElement) = result.toString()
    protected abstract fun execute(server: SpMpServer, context: Context): JsonElement?

    fun execute(server: SpMpServer, parameter_values: List<JsonPrimitive>): JsonElement? =
        execute(server, Context(parameter_values))

    fun executeOnRemoteServer(
        socket: ZmqSocket,
        parameter_values: List<JsonPrimitive>,
        reply_timeout_ms: Long?,
        verbose: Boolean = false,
    ): SpMpServer.ActionReply? {
        socket.recvMultipart(reply_timeout_ms) ?: return null

        if (verbose) {
            println("Sending action '$identifier' to server...")
        }

        socket.sendStringMultipart(
            listOf(SERVER_EXPECT_REPLY_CHAR + identifier, Json.encodeToString(parameter_values))
        )

        if (verbose) {
            println("Action '$identifier' sent to server. Waiting for reply...")
        }

        val timeout_end: Long? = reply_timeout_ms?.let { getTimeMillis() + it }
        do {
            val reply: List<String>? = socket.recvStringMultipart(timeout_end?.let { (it - getTimeMillis()).coerceAtLeast(ZMQ_NOBLOCK.toLong()) })
            if (!reply.isNullOrEmpty()) {
                if (verbose) {
                    println("Received reply from server for action '$identifier'")
                }

                // Hacky workaround, but if it works who cares?
                val joined: String = reply.joinToString().replace("\u0000, ", "")
                return Json.decodeFromString(joined)
            }
            else if (verbose) {
                println("Received empty reply from server for action '$identifier', continuing to wait...")
            }
        } while (timeout_end == null || getTimeMillis() < timeout_end)

        return null
    }

    companion object {
        private val ALL: List<ServerAction> = listOf(
            ServerActionStatus(),
            ServerActionGetProperty(),

            ServerActionPause(),
            ServerActionPlay(),
            ServerActionPlayPause(),

            ServerActionSeekToItem(),
            ServerActionSeekToNext(),
            ServerActionSeekToPrevious(),
            ServerActionSeekToTime(),
            ServerActionSetVolume(),

            ServerActionAddItem(),
            ServerActionMoveItem(),
            ServerActionRemoveItem(),
            ServerActionClearQueue()
        )

        fun getAll(): List<ServerAction> = ALL

        fun executeByName(server: SpMpServer, action_name: String, parameter_values: List<JsonPrimitive>): JsonElement? {
            val action: ServerAction? = ALL.firstOrNull { it.identifier == action_name }
            if (action == null) {
                throw NotImplementedError("Unknown action '$action_name'")
            }

            return action.execute(server, parameter_values)
        }
    }
}
