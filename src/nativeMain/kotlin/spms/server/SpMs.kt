package spms.server

import GIT_COMMIT_HASH
import cinterop.mpv.MpvClientImpl
import cinterop.mpv.getCurrentStateJson
import cinterop.zmq.ZmqRouter
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv
import spms.getHostname
import spms.localisation.Language
import spms.player.HeadlessPlayer
import spms.player.Player
import spms.player.PlayerEvent
import spms.serveraction.ServerAction
import kotlin.experimental.ExperimentalNativeApi
import kotlin.system.getTimeMillis

const val SERVER_EXPECT_REPLY_CHAR: Char = '!'
const val SEND_EVENTS_TO_INSTIGATING_CLIENT: Boolean = true

@OptIn(ExperimentalForeignApi::class)
class SpMs(mem_scope: MemScope, val secondary_port: Int, val headless: Boolean = false, enable_gui: Boolean = false): ZmqRouter(mem_scope) {
    @Serializable
    data class ActionReply(val success: Boolean, val error: String? = null, val error_cause: String? = null, val result: JsonElement? = null)

    private var item_durations: MutableMap<String, Long> = mutableMapOf()
    private val item_durations_channel: Channel<Unit> = Channel()

    val player: Player =
        if (headless)
            object : HeadlessPlayer() {
                override fun getCachedItemDuration(item_id: String): Long? = item_durations[item_id]

                override suspend fun loadItemDuration(item_id: String): Long {
                    var cached: Long? = item_durations[item_id]
                    while (cached == null) {
                        item_durations_channel.receive()
                        cached = item_durations[item_id]
                    }
                    return cached
                }

                override fun onEvent(event: PlayerEvent, clientless: Boolean) = onPlayerEvent(event, clientless)
                override fun onShutdown() = onPlayerShutdown()
            }
        else
            object : MpvClientImpl(secondary_port, headless = !enable_gui) {
                override fun onEvent(event: PlayerEvent, clientless: Boolean) = onPlayerEvent(event, clientless)
            }

    private var executing_client_id: Int? = null
    private var player_event_inc: Int = 0
    private var player_shut_down: Boolean = false
    private val player_events: MutableList<PlayerEvent> = mutableListOf()
    private val clients: MutableList<SpMsClient> = mutableListOf()
    private var playback_waiting_for_clients: Boolean = false

    fun getClients(caller: SpMsClientID? = null): List<SpMsClientInfo> =
        listOf(
            SpMsClientInfo(
                application_name,
                SpMsClientType.SERVER,
                Language.EN,
                getMachineId(),
                player_port = if (headless) null else secondary_port
            )
        ) + clients.map { it.info.copy(is_caller = it.id == caller) }

    fun poll(client_reply_timeout_ms: Long): Boolean {

        // Process stray messages (hopefully client handshakes)
        while (true) {
            val message: Message = recvMultipart(null) ?: break
            println("Got stray message before polling")
            onClientMessage(message)
        }

        // Send relevant events to each client
        var client_i: Int = clients.size - 1
        while (client_i >= 0) {
            val client: SpMsClient = clients[client_i--]

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
                        player_events.remove(event)
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
                val remaining = wait_end - getTimeMillis()
                if (remaining <= 0) {
                    break
                }

                val message: Message = recvMultipart(remaining) ?: continue

                if (message.client_id.contentHashCode() == client.id) {
                    client_reply = message
                    break
                }
                else {
                    // Handle connections from other clients
                    println("Got stray message during polling")
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

            val reply: MutableList<ActionReply> = mutableListOf()

            var i: Int = 0
            while (i < client_reply.parts.size) {
                val first: String = client_reply.parts[i++]
                val expects_reply: Boolean = first.first() == SERVER_EXPECT_REPLY_CHAR

                val action_name: String = if (expects_reply) first.substring(1) else first
                val action_params: List<JsonPrimitive> = Json.decodeFromString(client_reply.parts[i++])

                val result: JsonElement?
                try {
                    result = ServerAction.executeByName(this@SpMs, client.id, action_name, action_params)
                }
                catch (e: Throwable) {
                    val message: String = "Action $action_name(${action_params.map { it.contentOrNull }}) from $client failed"

                    if (expects_reply) {
                        reply.add(
                            ActionReply(
                                success = false,
                                error = message,
                                error_cause = e.message
                            )
                        )
                    }

                    RuntimeException(message, e).printStackTrace()

                    continue
                }

                if (expects_reply) {
                    reply.add(
                        ActionReply(
                            success = true,
                            result = result
                        )
                    )
                }
            }

            if (reply.isNotEmpty()) {
                sendMultipart(client.createMessage(listOf(Json.encodeToString(reply))))
                println("Sent reply to $client: $reply")
            }

            executing_client_id = null
        }

        return !player_shut_down
    }

    fun onClientReadyToPlay(client_id: SpMsClientID, item_index: Int, item_id: String, item_duration_ms: Long) {
        if (!playback_waiting_for_clients) {
            return
        }

        val ready_client: SpMsClient = clients.first { it.id == client_id }

        if (item_index != player.current_item_index || item_id != player.getItem()) {
            println("Got readyToPlay from $ready_client with mismatched index and/or item ID ($item_index, $item_id instead of ${player.current_item_index}, ${player.getItem()})")
            return
        }

        item_durations[item_id] = item_duration_ms
        if (player is HeadlessPlayer) {
            player.onDurationLoaded(item_id, item_duration_ms)
        }

        item_durations_channel.trySend(Unit)

        if (ready_client.ready_to_play) {
            return
        }

        ready_client.ready_to_play = true

        if (clients.all { it.type != SpMsClientType.PLAYER || it.ready_to_play }) {
            player.play()
            playback_waiting_for_clients = false
        }
    }

    private fun onPlayerEvent(event: PlayerEvent, clientless: Boolean) {
        if (event.type == PlayerEvent.Type.READY_TO_PLAY) {
            if (!playback_waiting_for_clients) {
                player.play()
            }
            return
        }

        println("Event ($clientless): $event")

        if (clients.isEmpty()) {
            return
        }

        if (event.type == PlayerEvent.Type.ITEM_TRANSITION) {
            for (client in clients) {
                client.ready_to_play = false

                if (client.type == SpMsClientType.PLAYER) {
                    playback_waiting_for_clients = true
                }
            }
        }

        event.init(
            event_id = player_event_inc++,
            client_id = if (clientless) null else executing_client_id,
            client_amount = clients.size
        )
        player_events.add(event)
    }

    private fun onPlayerShutdown() {
        player_shut_down = true
    }

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

        val client_handshake: SpMsClientHandshake
        try {
            client_handshake = handshake_message.parts.firstOrNull()?.let { Json.decodeFromString(it) } ?: return
        }
        catch (e: SerializationException) {
            println("Ignoring SerializationException in onClientMessage")
            return
        }

        val client: SpMsClient = SpMsClient(
            handshake_message.client_id,
            SpMsClientInfo(
                getNewClientName(client_handshake.name),
                client_handshake.type,
                client_handshake.getLanguage(),
                client_handshake.machine_id,
                player_port = client_handshake.player_port
            ),
            player_event_inc
        )

        clients.add(client)

        val server_handshake: SpMsServerHandshake =
            SpMsServerHandshake(
                SpMs.application_name,
                getDeviceName(),
                GIT_COMMIT_HASH,
                player.getCurrentStateJson()
            )

        sendMultipart(
            Message(
                client.id_bytes,
                listOf(Json.encodeToString(server_handshake))
            )
        )

        onClientConnected(client)
    }

    private fun onClientConnected(client: SpMsClient) {
        println("Client connected: $client")

        if (client.type == SpMsClientType.PLAYER) {
            if (!playback_waiting_for_clients && player.state == Player.State.BUFFERING) {
                playback_waiting_for_clients = true
            }
        }
    }

    private fun onClientTimedOut(client: SpMsClient, failed_timeout_ms: Long) {
        clients.remove(client)
        println("$client failed to respond within timeout (${failed_timeout_ms}ms)")

        if (
            client.type == SpMsClientType.PLAYER
            && !client.ready_to_play
            && clients.all { it.type != SpMsClientType.PLAYER || it.ready_to_play }
        ) {
            player.play()
            playback_waiting_for_clients = false
        }
    }

    private fun getEventsForClient(client: SpMsClient): List<PlayerEvent> =
        player_events.filter { event ->
            event.event_id >= client.event_head && (SEND_EVENTS_TO_INSTIGATING_CLIENT || event.client_id != client.id)
        }

    private fun SpMsClient.createMessage(parts: List<String>): ZmqRouter.Message =
        ZmqRouter.Message(id_bytes, parts)

    override fun toString(): String =
        "SpMs(player=$player)"

    companion object {
        const val application_name: String = "spmp-server"

        fun getMachineId(): String {
            val id_path: Path =
                when (Platform.osFamily) {
                    OsFamily.LINUX -> "/tmp/".toPath()
                    OsFamily.WINDOWS -> "${getenv("USERPROFILE")!!.toKString()}/AppData/Local/Temp/".toPath()
                    else -> throw NotImplementedError(Platform.osFamily.name)
                }.resolve("spmp_machine_id.txt")

            if (FileSystem.SYSTEM.exists(id_path)) {
                return FileSystem.SYSTEM.read(id_path) {
                    readUtf8()
                }
            }

            val parent: Path = id_path.parent!!
            if (!FileSystem.SYSTEM.exists(parent)) {
                FileSystem.SYSTEM.createDirectories(parent, true)
            }

            val id_length: Int = 8
            val allowed_chars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')

            val new_id: String = (1..id_length).map { allowed_chars.random() }.joinToString("")

            FileSystem.SYSTEM.write(id_path) {
                writeUtf8(new_id)
            }

            return new_id
        }
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
fun getDeviceName(): String {
    val hostname: String = memScoped {
        val str: CPointer<ByteVarOf<Byte>> = allocArray(1024)
        getHostname(str, 1023)
        return@memScoped str.toKString()
    }

    val os: String =
        when (Platform.osFamily) {
            OsFamily.MACOSX -> "OSX"
            OsFamily.IOS -> "iOS"
            OsFamily.LINUX -> "Linux"
            OsFamily.WINDOWS -> "Windows"
            OsFamily.ANDROID -> "Android"
            OsFamily.WASM -> "Wasm"
            OsFamily.TVOS -> "TV"
            OsFamily.WATCHOS -> "WatchOS"
            OsFamily.UNKNOWN -> "Unknown"
        }

    val architecture: String =
        when (Platform.cpuArchitecture) {
            CpuArchitecture.X86 -> "x86"
            CpuArchitecture.X64 -> "x86-64"
            CpuArchitecture.UNKNOWN -> "Unknown"
            else -> Platform.cpuArchitecture.name
        }

    return "$hostname ($os $architecture)"
}
