package spms.server

import cinterop.mpv.MpvClientImpl
import cinterop.mpv.getCurrentStateJson
import cinterop.zmq.ZmqRouter
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv
import spms.getHostname
import spms.player.HeadlessPlayer
import spms.player.Player
import spms.socketapi.parseSocketMessage
import spms.socketapi.player.PlayerAction
import spms.socketapi.server.ServerAction
import spms.socketapi.shared.*
import spms.localisation.SpMsLocalisation
import kotlin.experimental.ExperimentalNativeApi
import kotlin.system.exitProcess
import kotlin.system.getTimeMillis

private const val CLIENT_REPLY_TIMEOUT_MS: Long = 100

@OptIn(ExperimentalForeignApi::class)
class SpMs(
    mem_scope: MemScope,
    val headless: Boolean = false,
    enable_gui: Boolean = false,
    enable_media_session: Boolean = false
): ZmqRouter(mem_scope) {
    private var item_durations: MutableMap<String, Long> = mutableMapOf()
    private val item_durations_channel: Channel<Unit> = Channel()

    private var executing_client_id: Int? = null
    private var player_event_inc: Int = 0
    private val player_events: MutableList<SpMsPlayerEvent> = mutableListOf()
    private val clients: MutableList<SpMsClient> = mutableListOf()
    private var playback_waiting_for_clients: Boolean = false

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

                override fun canPlay(): Boolean = !playback_waiting_for_clients
                override fun onEvent(event: SpMsPlayerEvent, clientless: Boolean) = onPlayerEvent(event, clientless)
                override fun onShutdown() = onPlayerShutdown()
            }
        else
            object : MpvClientImpl(headless = !enable_gui) {
                override fun canPlay(): Boolean {
                    if (playback_waiting_for_clients) {
                        val waiting_for_clients: List<SpMsClient> = clients.filter { it.type.playsAudio() && !it.ready_to_play }
                        println("Call to canPlay returning false, waiting for the following clients: $waiting_for_clients")
                        return false
                    }
                    return true
                }
                override fun onEvent(event: SpMsPlayerEvent, clientless: Boolean) = onPlayerEvent(event, clientless)
                override fun onShutdown() = onPlayerShutdown()
            }

    private val media_session: SpMsMediaSession? =
        try {
            if (enable_media_session) SpMsMediaSession.create(player)
            else null
        }
        catch (e: Throwable) {
            RuntimeException("Ignoring exception that occurred when creating media session", e).printStackTrace()
            null
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
            val message: Message = recvMultipart(null) ?: break
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
            val wait_end: Long = getTimeMillis() + CLIENT_REPLY_TIMEOUT_MS
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

    private fun processClientMessage(message: Message, client: SpMsClient) {
        val reply: List<SpMsActionReply> = processClientActions(message.parts, client)

        if (reply.isNotEmpty()) {
            sendMultipart(client.createMessage(listOf(Json.encodeToString(reply))))
        }
    }

    fun onClientReadyToPlay(client_id: SpMsClientID, item_index: Int, item_id: String, item_duration_ms: Long) {
        if (!playback_waiting_for_clients) {
            println("Got readyToPlay from a client, but playback_waiting_for_clients is false, ignoring")
            return
        }

        val ready_client: SpMsClient? = clients.firstOrNull { it.id == client_id }
        if (ready_client == null) {
            println("Got readyToPlay from an unknown client, ignoring")
            return
        }

        if (item_index != player.current_item_index || item_id != player.getItem()) {
            println("Got readyToPlay from $ready_client with mismatched index and/or item ID, ignoring ($item_index, $item_id instead of ${player.current_item_index}, ${player.getItem()})")
            return
        }

        item_durations[item_id] = item_duration_ms
        if (player is HeadlessPlayer) {
            player.onDurationLoaded(item_id, item_duration_ms)
        }

        item_durations_channel.trySend(Unit)

        if (ready_client.ready_to_play) {
            println("Got readyToPlay from $ready_client, but it is already marked as ready, ignoring")
            return
        }

        ready_client.ready_to_play = true

        val waiting_for: Int = clients.count { it.type.playsAudio() && !it.ready_to_play }

        if (waiting_for == 0) {
            println("Got readyToPlay from $ready_client, beginning playback")

            playback_waiting_for_clients = false
            player.play()
        }
        else {
            println("Got readyToPlay from $ready_client, but still waiting for $waiting_for other clients to be ready")
        }
    }

    private fun onPlayerEvent(event: SpMsPlayerEvent, clientless: Boolean) {
        if (event.type == SpMsPlayerEvent.Type.READY_TO_PLAY) {
            if (!playback_waiting_for_clients) {
                player.play()
            }
            return
        }

        val event_client: SpMsClient? = clients.firstOrNull { it.id == event.client_id }
        println("Event ($clientless, $event_client): $event")

        media_session?.onPlayerEvent(event)

        if (clients.isEmpty()) {
            return
        }

        if (event.type == SpMsPlayerEvent.Type.ITEM_TRANSITION || event.type == SpMsPlayerEvent.Type.SEEKED) {
            var paused: Boolean = false
            for (client in clients) {
                client.ready_to_play = false

                if (client.type.playsAudio()) {
                    if (!paused) {
                        player.pause()
                        paused = true
                    }
                    playback_waiting_for_clients = true
                }
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

    private fun onPlayerShutdown() {
        exitProcess(0)
    }

    private fun getNewClientName(requested_name: String): String {
        var num: Int = 1
        var numbered_name: String = requested_name.trim()

        while (clients.any { it.name == numbered_name }) {
            numbered_name = "$requested_name #${++num}"
        }

        return numbered_name
    }

    private fun onClientMessage(message: Message) {
        val id: Int = message.client_id.contentHashCode()

        if (clients.any { it.id == id }) {
            return
        }

        val client_handshake: SpMsClientHandshake
        try {
            client_handshake = message.parts.firstOrNull()?.let { Json.decodeFromString(it) } ?: return
        }
        catch (e: SerializationException) {
            RuntimeException("Exception while parsing the following handshake message: ${message.parts}", e).printStackTrace()
            return
        }

        val client: SpMsClient = SpMsClient(
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
                machine_id = SpMs.getMachineId(),
                action_replies = action_replies
            )

        sendMultipart(
            Message(
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

    private fun SpMsClient.createMessage(parts: List<String>): ZmqRouter.Message =
        ZmqRouter.Message(id_bytes, parts)

    override fun toString(): String =
        "SpMs(player=$player)"

    companion object {
        const val application_name: String = "spmp-server"

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
