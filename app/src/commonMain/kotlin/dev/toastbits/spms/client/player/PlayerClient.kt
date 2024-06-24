package dev.toastbits.spms.client.player

import dev.toastbits.spms.mpv.MpvClientImpl
import dev.toastbits.spms.mpv.getCurrentStateJson
import dev.toastbits.spms.zmq.ZmqSocket
import dev.toastbits.spms.zmq.ZmqSocketType
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import dev.toastbits.spms.Command
import dev.toastbits.spms.client.ClientOptions
import dev.toastbits.spms.client.cli.SpMsCommandLineClientError
import dev.toastbits.spms.localisation.loc
import dev.toastbits.spms.server.PlayerOptions
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.server.CLIENT_HEARTBEAT_TARGET_PERIOD
import dev.toastbits.spms.socketapi.parseSocketMessage
import dev.toastbits.spms.socketapi.player.PlayerAction
import dev.toastbits.spms.socketapi.shared.*
import dev.toastbits.spms.getDeviceName
import dev.toastbits.spms.getMachineId
import dev.toastbits.spms.createLibMpv
import kotlin.time.*
import gen.libmpv.LibMpv

private val SERVER_REPLY_TIMEOUT: Duration = with (Duration) { 2.seconds }
private val CLIENT_REPLY_TIMEOUT: Duration = with (Duration) { 1.seconds }

private fun getClientName(): String =
    "SpMs Player Client"

private abstract class PlayerImpl(libmpv: LibMpv, headless: Boolean = true): MpvClientImpl(libmpv, headless = headless, playlist_auto_progress = false) {
    private var applied_server_state: SpMsServerState? = null
    private var server_state_applied_time: TimeMark? = null

    override fun onEvent(event: SpMsPlayerEvent, clientless: Boolean) {
        if (event.type == SpMsPlayerEvent.Type.READY_TO_PLAY) {
            onReadyToPlay()

            applied_server_state?.also { state ->
                var position: Duration = with (Duration) { state.current_position_ms.milliseconds }

                if (state.is_playing) {
                    position += server_state_applied_time!!.elapsedNow()
                }

                seekToTime(position.inWholeMilliseconds)

                applied_server_state = null
                server_state_applied_time = null
            }
        }
    }

    abstract fun onReadyToPlay()

    fun applyServerState(state: SpMsServerState) {
        clearQueue()
        for (item in state.queue) {
            addItem(item, -1)
        }

        seekToItem(state.current_item_index)

        if (state.is_playing) {
            play()
        }
        else {
            pause()
        }

        applied_server_state = state
        server_state_applied_time = TimeSource.Monotonic.markNow()
    }

    fun processServerEvent(event: SpMsPlayerEvent) {
        try {
            when (event.type) {
                SpMsPlayerEvent.Type.ITEM_TRANSITION -> {
                    val index: Int = event.properties["index"]!!.int
                    if (index >= 0) {
                        seekToItem(event.properties["index"]!!.int)
                    }
                }
                SpMsPlayerEvent.Type.PROPERTY_CHANGED -> {
                    val value: JsonPrimitive? = event.properties["value"]?.jsonPrimitive
                    val key: String = event.properties["key"]!!.content
                    when (key) {
                        "state" -> {
                        }
                        "is_playing" -> {
                            if (value!!.boolean) {
                                play()
                            }
                            else {
                                pause()
                            }
                        }
                        "repeat_mode" -> {
                            setRepeatMode(SpMsPlayerRepeatMode.entries[value!!.int])
                        }
                        "duration_ms" -> {
                        }
                        else -> throw NotImplementedError("${event.properties} ($key)")
                    }
                }
                SpMsPlayerEvent.Type.SEEKED -> {
                    seekToTime(event.properties["position_ms"]!!.long)
                }
                SpMsPlayerEvent.Type.ITEM_ADDED -> {
                    val item_id: String = event.properties["item_id"]!!.content
                    val index: Int = event.properties["index"]!!.int
                    addItem(item_id, index)
                }
                SpMsPlayerEvent.Type.ITEM_REMOVED -> {
                    removeItem(event.properties["index"]!!.int)
                }
                SpMsPlayerEvent.Type.ITEM_MOVED -> {
                    val to: Int = event.properties["to"]!!.int
                    val from: Int = event.properties["from"]!!.int
                    moveItem(from, to)
                }
                SpMsPlayerEvent.Type.CLEARED -> {
                    clearQueue()
                }
                SpMsPlayerEvent.Type.CANCEL_RADIO -> {}
                SpMsPlayerEvent.Type.READY_TO_PLAY -> {}
            }
        }
        catch (e: Throwable) {
            throw RuntimeException("Processing event $event failed", e)
        }
    }
}

class PlayerClient private constructor(val libmpv: LibMpv): Command(
    name = "player",
    help = { "TODO" },
    is_default = true
) {
    companion object {
        fun get(): PlayerClient? {
            if (!LibMpv.isAvailable()) {
                return null
            }

            return PlayerClient(createLibMpv())
        }
    }

    private val client_options by ClientOptions()
    private val player_options by PlayerOptions()

    private val json: Json = Json { ignoreUnknownKeys = true }
    private var shutdown: Boolean = false
    private val queued_messages: MutableList<Pair<String, List<JsonPrimitive>>> = mutableListOf()

    private lateinit var player: PlayerImpl

    override fun run() {
        super.run()

        val player_port: Int = client_options.port + 1

        player = object : PlayerImpl(libmpv, headless = !player_options.enable_gui) {
            override fun onShutdown() {
                shutdown = true
            }

            override fun canPlay() = true

            override fun onReadyToPlay() {
                queued_messages.add("readyToPlay" to listOf(JsonPrimitive(current_item_index), JsonPrimitive(getItem()), JsonPrimitive(duration_ms)))
            }
        }

        runBlocking {
            coroutineScope {
                launch(Dispatchers.Default) {
                    runPlayer(player_port)
                }
                launch(Dispatchers.Default) {
                    runServer(player_port)
                }
            }
        }
    }

    private suspend fun runServer(player_port: Int) {
        val address: String = "tcp://127.0.0.1:$player_port"
        log("Starting server socket on $address")

        val socket: ZmqSocket = ZmqSocket(ZmqSocketType.REP, true)
        socket.connect(address)
        delay(1000)

        while (!shutdown) {
            try {
                // We don't actually care about the client handshake, it's just for consistency with the main server api
//                val handshake_message: List<String> =
                socket.recvStringMultipart(CLIENT_REPLY_TIMEOUT) ?: continue

                val handshake_reply: SpMsServerHandshake =
                    SpMsServerHandshake(
                        name = getClientName(),
                        device_name = getDeviceName(),
                        spms_api_version = SPMS_API_VERSION,
                        server_state = player.getCurrentStateJson(),
                        machine_id = getMachineId()
                    )

                socket.sendStringMultipart(
                    listOf(Json.encodeToString(handshake_reply))
                )

                val message: List<String> =
                    socket.recvStringMultipart(CLIENT_REPLY_TIMEOUT) ?: continue

                val reply: List<SpMsActionReply> =
                    parseSocketMessage(message) { action_name, action_params ->
                        PlayerAction.executeByName(player, action_name, action_params)
                    }

                socket.sendStringMultipart(listOf(Json.encodeToString(reply)))
            }
            catch (e: Throwable) {
                if (e is CancellationException) {
                    break
                }
                e.printStackTrace()
            }
        }

        socket.release()
    }

    private suspend fun runPlayer(player_port: Int) {
        val socket: ZmqSocket = ZmqSocket(ZmqSocketType.DEALER, is_binder = false)

        val socket_address: String = client_options.getAddress("tcp")
        log(currentContext.loc.cli.connectingToSocket(socket_address))
        socket.connect(socket_address)

        log(currentContext.loc.cli.sending_handshake)

        val handshake: SpMsClientHandshake = SpMsClientHandshake(
            name = getClientName(),
            type = SpMsClientType.PLAYER,
            machine_id = getMachineId(),
            language = currentContext.loc.language.name,
            player_port = player_port
        )
        socket.sendStringMultipart(listOf(json.encodeToString(handshake)))

        val reply: List<String>? = socket.recvStringMultipart(SERVER_REPLY_TIMEOUT)
        if (reply == null) {
            throw SpMsCommandLineClientError(currentContext.loc.cli.errServerDidNotRespond(SERVER_REPLY_TIMEOUT))
        }

        val server_handshake: SpMsServerHandshake = Json.decodeFromString(reply.first())
        player.applyServerState(server_handshake.server_state)

        if (player_options.mute_on_start) {
            player.setVolume(0.0)
        }

        log("Initial state: ${server_handshake.server_state}")

        val message: MutableList<String> = mutableListOf()
        var last_heartbeat: TimeMark = TimeSource.Monotonic.markNow()

        while (!shutdown) {
            val events: List<SpMsPlayerEvent> = socket.pollEvents()
            for (event in events) {
                println("Processing event $event")
                player.processServerEvent(event)
            }

            if (queued_messages.isNotEmpty()) {
                for (queued in queued_messages) {
                    message.add(queued.first)
                    message.add(Json.encodeToString(queued.second))
                }
                queued_messages.clear()
            }
            else if (last_heartbeat.elapsedNow() >= CLIENT_HEARTBEAT_TARGET_PERIOD) {
                message.add(" ")
            }
            else {
                continue
            }

            log("Sending messages: $message")
            socket.sendStringMultipart(message)
            message.clear()

            last_heartbeat = TimeSource.Monotonic.markNow()
        }
    }

    private fun ZmqSocket.pollEvents(): List<SpMsPlayerEvent> {
        val events: List<SpMsPlayerEvent>? = recvStringMultipart(null)?.mapNotNull {
            try {
                Json.decodeFromString<SpMsPlayerEvent?>(it)
            }
            catch (e: Throwable) {
                RuntimeException("Parsing SpMsPlayerEvent failed $it", e).printStackTrace()
                return emptyList()
            }
        }

        return events.orEmpty()
    }
}
