package dev.toastbits.spms.server

import dev.toastbits.spms.mpv.MpvClientImpl
import dev.toastbits.spms.mpv.getCurrentStateJson
import dev.toastbits.spms.zmq.ZmqRouter
import dev.toastbits.spms.zmq.ZmqMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import dev.toastbits.spms.getHostname
import dev.toastbits.spms.player.Player
import dev.toastbits.spms.player.headless.HeadlessPlayer
import dev.toastbits.spms.socketapi.parseSocketMessage
import dev.toastbits.spms.socketapi.player.PlayerAction
import dev.toastbits.spms.socketapi.server.ServerAction
import dev.toastbits.spms.socketapi.shared.*
import dev.toastbits.spms.localisation.SpMsLocalisation
import dev.toastbits.spms.getMachineId
import dev.toastbits.spms.getDeviceName
import dev.toastbits.spms.createLibMpv
import kotlin.experimental.ExperimentalNativeApi
import kotlin.time.*
import gen.libmpv.LibMpv

private val CLIENT_REPLY_TIMEOUT: Duration = with (Duration) { 100.milliseconds }

open class SpMs(
    val headless: Boolean = !LibMpv.isAvailable(),
    enable_gui: Boolean = false
): ZmqRouter() {
    private var item_durations: MutableMap<String, Duration> = mutableMapOf()
    private val item_durations_channel: Channel<Unit> = Channel()

    private var executing_client_id: Int? = null
    private var player_event_inc: Int = 0
    private val player_events: MutableList<SpMsPlayerEvent> = mutableListOf()
    private val clients: MutableList<SpMsClient> = mutableListOf()
    private var playback_waiting_for_clients: Boolean = false

    val player: Player =
        if (headless)
            object : HeadlessPlayer() {
                override fun getCachedItemDuration(item_id: String): Duration? = item_durations[item_id]

                override suspend fun loadItemDuration(item_id: String): Duration {
                    var cached: Duration? = item_durations[item_id]
                    while (cached == null) {
                        item_durations_channel.receive()
                        cached = item_durations[item_id]
                    }
                    return cached
                }

                override fun canPlay(): Boolean = this@SpMs.canPlay()
                override fun onEvent(event: SpMsPlayerEvent, clientless: Boolean) = onPlayerEvent(event, clientless)
                override fun onShutdown() = onPlayerShutdown()
            }
        else
            object : MpvClientImpl(createLibMpv(), headless = !enable_gui) {
                override fun canPlay(): Boolean = this@SpMs.canPlay()
                override fun onEvent(event: SpMsPlayerEvent, clientless: Boolean) = onPlayerEvent(event, clientless)
                override fun onShutdown() = onPlayerShutdown()
            }

    private fun canPlay(): Boolean {
        if (playback_waiting_for_clients) {
            val waiting_for_clients: List<SpMsClient> = clients.filter { it.type.playsAudio() && !it.ready_to_play }
            println("Call to canPlay returning false, waiting for the following clients: $waiting_for_clients")
            return false
        }
        return true
    }

    fun getClients(caller: SpMsClientID? = null): List<SpMsClientInfo> =
        listOf(
            SpMsClientInfo(
                application_name,
                SpMsClientType.SERVER,
                SpMsLanguage.EN,
                getMachineId(),
                player_port = if (headless) null else bound_port
            )
        ) + clients.map { it.info.copy(is_caller = it.id == caller) }

    fun poll(client_reply_attempts: Int) {

        // Process stray messages (hopefully client handshakes)
        while (true) {
            val message: ZmqMessage = recvMultipart(null) ?: break
            println("Got stray message before polling")
            onClientMessage(message)
        }

        // Send relevant events to each client
        var client_i: Int = clients.size - 1
        while (client_i >= 0) {
            val client: SpMsClient = clients[client_i--]

            val message_parts: MutableList<String> = mutableListOf()

            val events: List<SpMsPlayerEvent> = getEventsForClient(client)
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
            val wait_start: TimeMark = TimeSource.Monotonic.markNow()
            var client_reply: ZmqMessage? = null

            while (true) {
                val remaining: Duration = CLIENT_REPLY_TIMEOUT - wait_start.elapsedNow()
                if (remaining <= Duration.ZERO) {
                    break
                }

                val message: ZmqMessage = recvMultipart(remaining) ?: continue

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
                if (++client.failed_connection_attempts >= client_reply_attempts) {
                    onClientTimedOut(client, client_reply_attempts)
                }
                continue
            }

            client.failed_connection_attempts = 0

            check(executing_client_id == null)
            executing_client_id = client.id

            try {
                processClientMessage(client_reply, client)
            }
            catch (e: Throwable) {
                RuntimeException("Exception while processing reply from $client", e).printStackTrace()
            }

            executing_client_id = null
        }
    }

    private fun processClientActions(actions: List<String>, client: SpMsClient): List<SpMsActionReply> {
        return parseSocketMessage(
            actions,
            {
                RuntimeException("Parse exception while processing message from $client", it).printStackTrace()
            }
        ) { action_name, action_params ->
            val server_action: ServerAction? = ServerAction.getByName(action_name)
            if (server_action != null) {
                println("Performing server action $action_name with $action_params from $client")
                return@parseSocketMessage server_action.execute(this, client.id, action_params)
            }

            if (!headless && player is MpvClientImpl) {
                val player_action: PlayerAction? = PlayerAction.getByName(action_name)
                if (player_action != null) {
                    println("Performing player action $action_name with $action_params from $client")
                    return@parseSocketMessage player_action.execute(player, action_params)
                }
            }

            throw NotImplementedError("Unknown action '$action_name' from $client")
        }
    }

    private fun processClientMessage(message: ZmqMessage, client: SpMsClient) {
        val reply: List<SpMsActionReply> = processClientActions(message.parts, client)

        if (reply.isNotEmpty()) {
            sendMultipart(client.createMessage(listOf(Json.encodeToString(reply))))
        }
    }

    fun onClientReadyToPlay(client_id: SpMsClientID, item_index: Int, item_id: String, item_duration: Duration) {
        if (!playback_waiting_for_clients) {
            println("Got readyToPlay from a client, but playback_waiting_for_clients is false, ignoring")
            return
        }

        val ready_client: SpMsClient? = clients.firstOrNull { it.id == client_id }
        if (ready_client == null) {
            println("Got readyToPlay from an unknown client, ignoring")
            return
        }

        if (item_duration <= Duration.ZERO) {
            println("Got readyToPlay from $ready_client with invalid duration ($item_duration), ignoring")
            return
        }

        if (item_index != player.current_item_index || item_id != player.getItem()) {
            println("Got readyToPlay from $ready_client with mismatched index and/or item ID, ignoring ($item_index, $item_id instead of ${player.current_item_index}, ${player.getItem()})")
            return
        }

        item_durations[item_id] = item_duration
        if (player is HeadlessPlayer) {
            player.onDurationLoaded(item_id, item_duration)
        }

        item_durations_channel.trySend(Unit)

        if (ready_client.ready_to_play) {
            println("Got readyToPlay from $ready_client, but it is already marked as ready, ignoring (duration=$item_duration)")
            return
        }

        ready_client.ready_to_play = true

        val waiting_for: Int = clients.count { it.type.playsAudio() && !it.ready_to_play }

        if (waiting_for == 0) {
            println("Got readyToPlay from $ready_client, beginning playback (duration=$item_duration)")

            playback_waiting_for_clients = false
            player.play()
        }
        else {
            println("Got readyToPlay from $ready_client, but still waiting for $waiting_for other clients to be ready (duration=$item_duration)")
        }
    }

    protected open fun onPlayerEvent(event: SpMsPlayerEvent, clientless: Boolean) {
        if (event.type == SpMsPlayerEvent.Type.READY_TO_PLAY) {
            if (!playback_waiting_for_clients) {
                player.play()
            }
            return
        }

        val event_client: SpMsClient? = clients.firstOrNull { it.id == event.client_id }
        println("Event ($clientless, $event_client): $event")

        if (clients.isEmpty()) {
            return
        }

        if (event.type == SpMsPlayerEvent.Type.ITEM_TRANSITION || event.type == SpMsPlayerEvent.Type.SEEKED) {
            var audio_client_present: Boolean = false

            for (client in clients) {
                client.ready_to_play = false
                if (client.type.playsAudio()) {
                    audio_client_present = true
                }
            }

            if (player is HeadlessPlayer || audio_client_present) {
                player.pause()
                playback_waiting_for_clients = true
            }
        }

        event.init(
            event_id = player_event_inc++,
            client_id = if (clientless) null else executing_client_id,
            client_amount = clients.count { it.type.receivesEvents() }
        )

        val i: MutableIterator<SpMsPlayerEvent> = player_events.iterator()
        while (i.hasNext()) {
            val other: SpMsPlayerEvent = i.next()
            if (event.overrides(other)) {
                i.remove()
            }
        }

        player_events.add(event)
    }

    protected open fun onPlayerShutdown() {}

    private fun getNewClientName(requested_name: String): String {
        var num: Int = 1
        var numbered_name: String = requested_name.trim()

        while (clients.any { it.name == numbered_name }) {
            numbered_name = "$requested_name #${++num}"
        }

        return numbered_name
    }

    private fun onClientMessage(message: ZmqMessage) {
        val id: Int = message.client_id.contentHashCode()
        val content: String? = message.parts.firstOrNull()

        var client: SpMsClient? = clients.firstOrNull { it.id == id }
        if (client != null) {
            println("Got stray message from connected client $client, ignoring: ${message.parts.toList()}")
            return
        }
        else if (content == null) {
            println("Got empty stray message from unknown client, ignoring")
            return
        }

        val client_handshake: SpMsClientHandshake
        try {
            client_handshake = Json.decodeFromString(content)
        }
        catch (e: SerializationException) {
            RuntimeException("Exception while parsing the following handshake message: ${message.parts}", e).printStackTrace()
            return
        }

        client = SpMsClient(
            message.client_id,
            SpMsClientInfo(
                name = getNewClientName(client_handshake.name),
                type = client_handshake.type,
                language = client_handshake.getLanguage(),
                machine_id = client_handshake.machine_id,
                player_port = client_handshake.player_port
            ),
            player_event_inc
        )

        val action_replies: List<SpMsActionReply>?
        if (client_handshake.actions != null) {
            action_replies = processClientActions(client_handshake.actions, client)
        }
        else {
            action_replies = null
        }

        val server_handshake: SpMsServerHandshake =
            SpMsServerHandshake(
                name = SpMs.application_name,
                device_name = getDeviceName(),
                spms_api_version = SPMS_API_VERSION,
                server_state = player.getCurrentStateJson(),
                machine_id = getMachineId(),
                action_replies = action_replies
            )

        sendMultipart(
            ZmqMessage(
                client.id_bytes,
                listOf(Json.encodeToString(server_handshake))
            )
        )
        println("Sent connection reply to $client: $server_handshake")

        if (client.type.receivesEvents()) {
            clients.add(client)
            onClientConnected(client)
        }
    }

    private fun onClientConnected(client: SpMsClient) {
        println("Client connected: $client")

        if (client.type.playsAudio() && player.state == SpMsPlayerState.BUFFERING) {
            playback_waiting_for_clients = true
        }
    }

    private fun onClientTimedOut(client: SpMsClient, attempts: Int) {
        clients.remove(client)
        println("$client failed to respond after $attempts attempts")

        if (
            client.type.playsAudio()
            && !client.ready_to_play
            && clients.all { !it.type.playsAudio() || it.ready_to_play }
        ) {
            player.play()
            playback_waiting_for_clients = false
        }
    }

    private fun getEventsForClient(client: SpMsClient): List<SpMsPlayerEvent> =
        player_events.filter { event ->
            event.event_id >= client.event_head && (event.shouldSendToInstigatingClient() || event.client_id != client.id)
        }

    private fun SpMsClient.createMessage(parts: List<String>): ZmqMessage =
        ZmqMessage(id_bytes, parts)

    override fun toString(): String =
        "SpMs(player=$player)"

    companion object {
        const val application_name: String = "spmp-server"

        fun isAvailable(headless: Boolean): Boolean {
            if (headless) {
                return true
            }

            return LibMpv.isAvailable()
        }

        var logging_enabled: Boolean = true
        fun log(msg: Any?) {
            if (logging_enabled) {
                println(msg)
            }
        }

        private var version_printed: Boolean = false
        fun printVersionInfo(localisation: SpMsLocalisation) {
            if (version_printed) {
                return
            }
            println(localisation.versionInfoText(SPMS_API_VERSION))
            version_printed = true
        }
    }
}
