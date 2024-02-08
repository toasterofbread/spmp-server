package spms.client.player

import GIT_COMMIT_HASH
import cinterop.mpv.MpvClientImpl
import cinterop.mpv.getCurrentStateJson
import cinterop.zmq.ZmqSocket
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import libzmq.*
import spms.Command
import spms.client.ClientOptions
import spms.client.cli.SpMsCommandLineClientError
import spms.localisation.loc
import spms.player.PlayerEvent
import spms.server.*
import spms.socketapi.ActionReply
import spms.socketapi.parseSocketMessage
import spms.socketapi.player.PlayerAction
import kotlin.system.getTimeMillis

private const val SERVER_REPLY_TIMEOUT_MS: Long = 10000
private const val SERVER_EVENT_TIMEOUT_MS: Long = 10000
private const val POLL_INTERVAL_MS: Long = 100
private const val CLIENT_REPLY_TIMEOUT_MS: Long = 1000

private fun getClientName(): String =
    "SpMs Player Client"

private abstract class PlayerImpl(headless: Boolean = true): MpvClientImpl(headless) {
    override fun onEvent(event: PlayerEvent, clientless: Boolean) {
        if (event.type == PlayerEvent.Type.READY_TO_PLAY) {
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
        setVolume(state.volume.toDouble())

        if (state.is_playing) {
            play()
        }
        else {
            pause()
        }
    }

    fun processServerEvent(event: PlayerEvent) {
        try {
            when (event.type) {
                PlayerEvent.Type.ITEM_TRANSITION -> {
                    seekToItem(event.properties["index"]!!.int)
                }
                PlayerEvent.Type.PROPERTY_CHANGED -> {
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
                        }
                        "volume" -> {
                            setVolume(value!!.double)
                        }
                        "duration_ms" -> {
                        }
                        else -> throw NotImplementedError("${event.properties} ($key)")
                    }
                }
                PlayerEvent.Type.SEEKED -> {
                    seekToTime(event.properties["position_ms"]!!.long)
                }
                PlayerEvent.Type.ITEM_ADDED -> {
                    val item_id: String = event.properties["item_id"]!!.content
                    val index: Int = event.properties["index"]!!.int
                    addItem(item_id, index)
                }
                PlayerEvent.Type.ITEM_REMOVED -> {
                    removeItem(event.properties["index"]!!.int)
                }
                PlayerEvent.Type.ITEM_MOVED -> {
                    val to: Int = event.properties["to"]!!.int
                    val from: Int = event.properties["from"]!!.int
                    moveItem(from, to)
                }
                PlayerEvent.Type.CLEARED -> {
                    clearQueue()
                }
                PlayerEvent.Type.READY_TO_PLAY -> {}
            }
        }
        catch (e: Throwable) {
            throw RuntimeException("Processing event $event failed", e)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class PlayerClient private constructor(): Command(
    name = "player",
    help = { "TODO" },
    is_default = true
) {
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
                delay(POLL_INTERVAL_MS)

                val handshake_message: List<String> =
                    socket.recvStringMultipart(CLIENT_REPLY_TIMEOUT_MS) ?: continue

                val handshake_reply: SpMsServerHandshake =
                    SpMsServerHandshake(
                        name = getClientName(),
                        device_name = getDeviceName(),
                        spms_commit_hash = GIT_COMMIT_HASH,
                        server_state = player.getCurrentStateJson(),
                        machine_id = SpMs.getMachineId()
                    )

                socket.sendStringMultipart(
                    listOf(Json.encodeToString(handshake_reply))
                )

                val message: List<String> =
                    socket.recvStringMultipart(CLIENT_REPLY_TIMEOUT_MS)!!

                val reply: List<ActionReply> =
                    parseSocketMessage(message) { action_name, action_params ->
                        PlayerAction.executeByName(player, action_name, action_params)
                    }

                socket.sendStringMultipart(listOf(Json.encodeToString(reply)))
            }
            catch (e: Throwable) {
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

        val reply: List<String>? = socket.recvStringMultipart(SERVER_REPLY_TIMEOUT_MS)
        if (reply == null) {
            throw SpMsCommandLineClientError(currentContext.loc.cli.errServerDidNotRespond(SERVER_REPLY_TIMEOUT_MS))
        }

        val server_handshake: SpMsServerHandshake = Json.decodeFromString(reply.first())
        player.applyServerState(server_handshake.server_state)

        if (player_options.mute_on_start) {
            player.setVolume(0.0)
        }

        log("Initial state: ${server_handshake.server_state}")

        val message: MutableList<String> = mutableListOf()

        while (!shutdown) {
//            delay(POLL_INTERVAL_MS)

            val events: List<PlayerEvent> = socket.pollEvents()
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
                message.add("")
            }

            socket.sendStringMultipart(message)
            message.clear()
        }
    }

    private fun ZmqSocket.pollEvents(): List<PlayerEvent> {
        val wait_end: Long = getTimeMillis() + SERVER_EVENT_TIMEOUT_MS
        var events: List<PlayerEvent>? = null

        while (events == null && getTimeMillis() < wait_end) {
            events =
                recvStringMultipart(
                    (wait_end - getTimeMillis()).coerceAtLeast(ZMQ_NOBLOCK.toLong())
                )
                ?.mapNotNull { event ->
                    val string: String = event.removeSuffix("\u0000").takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null
                    return@mapNotNull json.decodeFromString(string)
                }
        }

        if (events == null) {
            throw SpMsCommandLineClientError(currentContext.loc.cli.errServerDidNotSendEvents(SERVER_EVENT_TIMEOUT_MS))
        }

        return events
    }

    companion object {
        fun get(): PlayerClient = PlayerClient()
    }
}
