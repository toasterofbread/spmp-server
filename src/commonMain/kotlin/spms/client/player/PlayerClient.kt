package spms.client.player

import cinterop.mpv.MpvClientImpl
import cinterop.mpv.getCurrentStateJson
import cinterop.zmq.ZmqSocket
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import libzmq.ZMQ_DEALER
import libzmq.ZMQ_NOBLOCK
import libzmq.ZMQ_REP
import spms.Command
import spms.client.ClientOptions
import spms.client.cli.SpMsCommandLineClientError
import spms.localisation.loc
import spms.server.PlayerOptions
import spms.server.SpMs
import spms.server.getDeviceName
import spms.socketapi.parseSocketMessage
import spms.socketapi.player.PlayerAction
import spms.socketapi.shared.*
import kotlin.system.getTimeMillis
import kotlin.time.*

private val SERVER_REPLY_TIMEOUT: Duration = with (Duration) { 2.seconds }
private val SERVER_EVENT_TIMEOUT: Duration = with (Duration) { 11.seconds }
private val POLL_INTERVAL: Duration = with (Duration) { 100.milliseconds }
private val CLIENT_REPLY_TIMEOUT: Duration = with (Duration) { 1.seconds }

private fun getClientName(): String =
    "SpMs Player Client"

private abstract class PlayerImpl(headless: Boolean = true): MpvClientImpl(headless = headless, playlist_auto_progress = false) {
    override fun onEvent(event: SpMsPlayerEvent, clientless: Boolean) {
        if (event.type == SpMsPlayerEvent.Type.READY_TO_PLAY) {
            onReadyToPlay()
        }
    }

    abstract fun onReadyToPlay()

    fun applyServerState(state: SpMsServerState) {
        clearQueue()
        for (item in state.queue) {
            addItem(item, -1)
        }

        seekToItem(state.current_item_index)
        seekToTime(state.current_position_ms.toLong())

        if (state.is_playing) {
            play()
        }
        else {
            pause()
        }
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

    companion object {
        fun isAvailable(): Boolean = MpvClientImpl.isAvailable()
    }
}

@OptIn(ExperimentalForeignApi::class)
class PlayerClient private constructor(): Command(
    name = "player",
    help = { "TODO" },
    is_default = true
) {
    companion object {
        fun get(): PlayerClient? = if (PlayerImpl.isAvailable()) PlayerClient() else null
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

        player = object : PlayerImpl(headless = !player_options.enable_gui) {
            override fun onShutdown() {
                shutdown = true
            }

            override fun canPlay() = true

            override fun onReadyToPlay() {
                queued_messages.add("readyToPlay" to listOf(JsonPrimitive(current_item_index), JsonPrimitive(getItem()), JsonPrimitive(duration_ms)))
            }
        }

        runBlocking {
            memScoped {
                coroutineScope {
                    launch(Dispatchers.Default) {
                        runPlayer(this@memScoped, player_port)
                    }
                    launch(Dispatchers.Default) {
                        runServer(this@memScoped, player_port)
                    }
                }
            }
        }
    }

    private suspend fun runServer(mem_scope: MemScope, player_port: Int) {
        val address: String = "tcp://127.0.0.1:$player_port"
        log("Starting server socket on $address")

        val socket: ZmqSocket = ZmqSocket(mem_scope, ZMQ_REP, true)
        socket.connect(address)
        delay(1000)

        while (!shutdown) {
            try {
                delay(POLL_INTERVAL)

                // We don't actually care about the client handshake, it's just for consistency with the main server api
//                val handshake_message: List<String> =
                socket.recvStringMultipart(CLIENT_REPLY_TIMEOUT) ?: continue

                val handshake_reply: SpMsServerHandshake =
                    SpMsServerHandshake(
                        name = getClientName(),
                        device_name = getDeviceName(),
                        spms_api_version = SPMS_API_VERSION,
                        server_state = player.getCurrentStateJson(),
                        machine_id = SpMs.getMachineId()
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

    private suspend fun runPlayer(mem_scope: MemScope, player_port: Int) {
        val socket: ZmqSocket = ZmqSocket(mem_scope, ZMQ_DEALER, is_binder = false)

        val socket_address: String = client_options.getAddress("tcp")
        log(currentContext.loc.cli.connectingToSocket(socket_address))
        socket.connect(socket_address)

        log(currentContext.loc.cli.sending_handshake)

        val handshake: SpMsClientHandshake = SpMsClientHandshake(
            name = getClientName(),
            type = SpMsClientType.PLAYER,
            machine_id = SpMs.getMachineId(),
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

        while (!shutdown) {
//            delay(POLL_INTERVAL)

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

                log("Sending messages: $message")
            }
            else {
                message.add(" ")
            }

            socket.sendStringMultipart(message)
            message.clear()
        }
    }

    private fun ZmqSocket.pollEvents(): List<SpMsPlayerEvent> {
        val wait_start: TimeMark = TimeSource.Monotonic.markNow()
        var events: List<SpMsPlayerEvent>? = null

        while (events == null && wait_start.elapsedNow() < SERVER_EVENT_TIMEOUT) {
            val message: List<String>? = with (Duration) {
                recvStringMultipart(
                    (SERVER_EVENT_TIMEOUT - wait_start.elapsedNow()).inWholeMilliseconds.coerceAtLeast(ZMQ_NOBLOCK.toLong()).milliseconds
                )
            }

            events = message?.mapNotNull {
                try {
                    Json.decodeFromString<SpMsPlayerEvent?>(it)
                }
                catch (e: Throwable) {
                    RuntimeException("Parsing SpMsPlayerEvent failed $it", e).printStackTrace()
                    return emptyList()
                }
            }
        }

        if (events == null) {
            throw SpMsCommandLineClientError(currentContext.loc.cli.errServerDidNotSendEvents(SERVER_EVENT_TIMEOUT))
        }

        return events.orEmpty()
    }
}
