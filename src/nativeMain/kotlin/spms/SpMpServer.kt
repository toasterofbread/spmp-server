package spms

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import libzmq.ZMQ_NOBLOCK
import mpv.EventEmitterMpvClient
import mpv.PlayerEvent
import mpv.executeActionByName
import mpv.getSerialisedProperties
import zmq.ZmqRouter
import kotlin.system.getTimeMillis

@OptIn(ExperimentalForeignApi::class)
class SpMpServer(mem_scope: MemScope): ZmqRouter(mem_scope) {
    private var executing_client_id: Int? = null
    private var player_event_incr: Int = 0

    private val mpv = object : EventEmitterMpvClient() {
        val events: MutableList<PlayerEvent> = mutableListOf()

        override fun onEvent(event: PlayerEvent) {
            event.init(player_event_incr++, executing_client_id, clients.size)
            events.add(event)
        }
    }

    private class Client(val id_bytes: ByteArray, val name: String, val event_head: Int) {
        val id: Int = id_bytes.contentHashCode()

        fun createMessage(parts: List<String>): Message =
            Message(id_bytes, parts)

        override fun toString(): String =
            "Client(id=$id, name=$name, event_head=$event_head)"
    }
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

        sendMultipart(Message(client.id_bytes, listOf(mpv.getSerialisedProperties())))
    }

    private fun onClientTimedOut(client: Client, failed_timeout_ms: Long) {
        clients.remove(client)
        println("$client failed to respond within timeout (${failed_timeout_ms}ms)")
    }

    private fun getEventsForClient(client: Client): List<PlayerEvent> =
        mpv.events.filter { event ->
            event.event_id >= client.event_head && event.client_id != client.id
        }

    fun poll(client_reply_timeout_ms: Long) {

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
                message_parts.add("")
            }
            else {
                // Add events to message, then consume
                for (event in events) {
                    message_parts.add(Json.encodeToString(event))

                    event.onConsumedByClient()
                    if (event.pending_client_amount == 0) {
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
            if (client_reply.parts.isEmpty()) {
                continue
            }

            if (client_reply.parts.size % 2 != 0) {
                println("$client reply size (${client_reply.parts.size}) is invalid")
                continue
            }

            check(executing_client_id == null)
            executing_client_id = client.id

            var i: Int = 0
            while (i < client_reply.parts.size) {
                val action_name: String = client_reply.parts[i++]
                val action_params: List<JsonPrimitive> = Json.decodeFromString(client_reply.parts[i++])

                try {
                    mpv.executeActionByName(action_name, action_params)
                }
                catch (e: Throwable) {
                    RuntimeException(
                        "Action $action_name(${action_params.map { it.contentOrNull }}) from $client failed:",
                        e
                    ).printStackTrace()
                }
            }

            executing_client_id = null
        }
    }
}
