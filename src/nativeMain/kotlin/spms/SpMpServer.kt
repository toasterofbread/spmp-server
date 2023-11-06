package spms

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import libzmq.ZMQ_NOBLOCK
import mpv.EventEmitterMpvClient
import mpv.PlayerEvent
import mpv.getCurrentStatusJson
import spms.actions.ServerAction
import zmq.ZmqRouter
import kotlin.system.getTimeMillis

const val SERVER_EXPECT_REPLY_CHAR: Char = '!'
const val SEND_EVENTS_TO_INSTIGATING_CLIENT: Boolean = true

@OptIn(ExperimentalForeignApi::class)
class SpMpServer(mem_scope: MemScope, val headless: Boolean = true): ZmqRouter(mem_scope) {
    private var executing_client_id: Int? = null
    private var player_event_incr: Int = 0
    private var player_shut_down: Boolean = false

    @Serializable
    data class ActionReply(val success: Boolean, val error: String? = null, val error_cause: String? = null, val result: JsonElement? = null)

    private class Client(val id_bytes: ByteArray, val name: String, var event_head: Int) {
        val id: Int = id_bytes.contentHashCode()

        fun createMessage(parts: List<String>): Message =
            Message(id_bytes, parts)

        override fun toString(): String =
            "Client(id=$id, name=$name, event_head=$event_head)"
    }

    inner class SpMpMpvClient(): EventEmitterMpvClient(headless) {
        val events: MutableList<PlayerEvent> = mutableListOf()

        override fun onEvent(event: PlayerEvent, clientless: Boolean) {
            println("Event ($clientless): $event")

            if (clients.isEmpty()) {
                return
            }

            event.init(
                event_id = player_event_incr++,
                client_id = if (clientless) null else executing_client_id,
                client_amount = clients.size
            )
            events.add(event)
        }

        override fun onShutdown() {
            player_shut_down = true
        }
    }

    val mpv = SpMpMpvClient()
    private val clients: MutableList<Client> = mutableListOf()

    private fun getNewClientName(requested_name: String): String {
        var num: Int = 1
        var numbered_name: String = requested_name.trim()

        while (clients.any { it.name == numbered_name }) {
            numbered_name = "$requested_name #${++num}"
        }

        return numbered_name
    }

    private fun onClientMessage(handshake_message: Message) {
        val id: Int = handshake_message.client_id.contentHashCode()

        // Return if client is already added
        if (clients.any { it.id == id }) {
            return
        }

        val requested_name: String = handshake_message.parts.firstOrNull() ?: return
        val client = Client(handshake_message.client_id, getNewClientName(requested_name), player_event_incr)

        clients.add(client)
        println("$client connected")

        sendMultipart(
            Message(
                client.id_bytes,
                listOf(Json.encodeToString(mpv.getCurrentStatusJson()))
            )
        )
    }

    private fun onClientTimedOut(client: Client, failed_timeout_ms: Long) {
        clients.remove(client)
        println("$client failed to respond within timeout (${failed_timeout_ms}ms)")
    }

    private fun getEventsForClient(client: Client): List<PlayerEvent> =
        mpv.events.filter { event ->
            event.event_id >= client.event_head && (SEND_EVENTS_TO_INSTIGATING_CLIENT || event.client_id != client.id)
        }

    fun poll(client_reply_timeout_ms: Long): Boolean {

        // Process stray messages (hopefully client handshakes)
        while (true) {
            val message: Message = recvMultipart(null) ?: break
            onClientMessage(message)
        }

        // Send relevant events to each client
        var client_i: Int = clients.size - 1
        while (client_i >= 0) {
            val client: Client = clients[client_i--]
            val message_parts: MutableList<String> = mutableListOf()

            val events: List<PlayerEvent> = getEventsForClient(client)
            if (events.isEmpty()) {
                message_parts.add("null")
            }
            else {
                // Add events to message, then consume
                for (event in events) {
                    message_parts.add(Json.encodeToString(event))
                    client.event_head = maxOf(event.event_id, client.event_head)

                    event.onConsumedByClient()
                    if (event.pending_client_amount <= 0) {
                        mpv.events.remove(event)
                    }
                }
            }

            sendMultipart(client.createMessage(message_parts))

            if (events.isNotEmpty()) {
                println("Sent events $events to client $client")
            }

            // Wait for client to reply
            val wait_end: Long = getTimeMillis() + client_reply_timeout_ms
            var client_reply: Message? = null

            while (true) {
                val message: Message = recvMultipart(
                    (wait_end - getTimeMillis()).coerceAtLeast(ZMQ_NOBLOCK.toLong())
                ) ?: break

                if (message.client_id.contentHashCode() == client.id) {
                    client_reply = message
                    break
                }
                else {
                    // Handle connections from other clients
                    onClientMessage(message)
                }
            }

            // Client did not reply to message within timeout
            if (client_reply == null) {
                onClientTimedOut(client, client_reply_timeout_ms)
                continue
            }

            // Empty response
            if (client_reply.parts.size < 2) {
                continue
            }

            if (client_reply.parts.size % 2 != 0) {
                println("$client reply size (${client_reply.parts.size}) is invalid")
                continue
            }

            println("Got reply from $client: $client_reply")

            check(executing_client_id == null)
            executing_client_id = client.id

            var i: Int = 0
            while (i < client_reply.parts.size) {
                val first: String = client_reply.parts[i++]
                val expects_reply: Boolean = first.first() == SERVER_EXPECT_REPLY_CHAR

                val action_name: String = if (expects_reply) first.substring(1) else first
                val action_params: List<JsonPrimitive> = Json.decodeFromString(client_reply.parts[i++])

                try {
                    val result: JsonElement? = ServerAction.executeByName(this@SpMpServer, action_name, action_params)
                    if (expects_reply) {
                        val reply = ActionReply(
                            success = true,
                            result = result
                        )
                        sendMultipart(client.createMessage(listOf(Json.encodeToString(reply))))

                        println("Sent reply to $client for action '$action_name': $reply")
                    }
                }
                catch (e: Throwable) {
                    val message: String = "Action $action_name(${action_params.map { it.contentOrNull }}) from $client failed"

                    if (expects_reply) {
                        val reply = ActionReply(
                            success = false,
                            error = message,
                            error_cause = e.message
                        )
                        sendMultipart(client.createMessage(listOf(Json.encodeToString(reply))))
                    }

                    RuntimeException(message, e).printStackTrace()
                }
            }

            executing_client_id = null
        }

        return !player_shut_down
    }
}
