package spms.action.server

import cinterop.zmq.ZmqSocket
import com.github.ajalt.clikt.core.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import libzmq.ZMQ_NOBLOCK
import spms.LocalisedMessageProvider
import spms.localisation.loc
import spms.server.SERVER_EXPECT_REPLY_CHAR
import spms.server.SpMs
import spms.server.SpMsClientID
import kotlin.system.getTimeMillis

sealed class ServerAction(
    override val identifier: String,
    override val name: LocalisedMessageProvider,
    override val help: LocalisedMessageProvider,
    override val parameters: List<Parameter>,
    override val hidden: Boolean = false
): Action<SpMs> {
    override val type: Type = type.SERVER

    fun executeOnRemoteServer(
        socket: ZmqSocket,
        parameter_values: List<JsonPrimitive>,
        reply_timeout_ms: Long?,
        context: Context,
        silent: Boolean = false
    ): SpMs.ActionReply? {
        socket.recvMultipart(reply_timeout_ms) ?: return null

        if (!silent) {
            println(context.loc.server_actions.sendingActionToServer(identifier))
        }

        socket.sendStringMultipart(
            listOf(SERVER_EXPECT_REPLY_CHAR + identifier, Json.encodeToString(parameter_values))
        )

        if (!silent) {
            println(context.loc.server_actions.actionSentAndWaitingForReply(identifier))
        }

        val timeout_end: Long? = reply_timeout_ms?.let { getTimeMillis() + it }
        do {
            val reply: List<String>? = socket.recvStringMultipart(timeout_end?.let { (it - getTimeMillis()).coerceAtLeast(ZMQ_NOBLOCK.toLong()) })
            if (!reply.isNullOrEmpty()) {
                if (!silent) {
                    println(context.loc.server_actions.receivedReplyFromServer(identifier))
                }

                // Hacky workaround, but if it works who cares?
                val joined: String = reply.joinToString().replace("\u0000, ", "")
                return Json.decodeFromString<List<SpMs.ActionReply>>(joined).first()
            }
            else if (!silent) {
                println(context.loc.server_actions.receivedEmptyReplyFromServer(identifier))
            }
        } while (timeout_end == null || getTimeMillis() < timeout_end)

        return null
    }

    companion object {
        private val ALL: List<ServerAction> = listOf(
            ServerActionGetStatus(),
            ServerActionGetClients(),
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
            ServerActionClearQueue(),

            ServerActionReadyToPlay()
        )

        fun getAll(): List<ServerAction> = ALL
        fun getByName(action_name: String): ServerAction? = ALL.firstOrNull { it.identifier == action_name }

        fun executeByName(server: SpMs, client: SpMsClientID, action_name: String, parameter_values: List<JsonPrimitive>): JsonElement? {
            val action: ServerAction? = getByName(action_name)
            if (action == null) {
                throw NotImplementedError("Unknown action '$action_name'")
            }

            return action.execute(server, client, parameter_values)
        }
    }
}
