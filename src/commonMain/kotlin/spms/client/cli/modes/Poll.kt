package spms.client.cli.modes

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import libzmq.ZMQ_NOBLOCK
import spms.client.cli.CommandLineClientMode
import spms.client.cli.SpMsCommandLineClientError
import spms.localisation.loc
import kotlin.system.getTimeMillis
import spms.socketapi.shared.SpMsSocketApi
import kotlinx.cinterop.ExperimentalForeignApi

private const val SERVER_EVENT_TIMEOUT_MS: Long = 10000
private const val POLL_INTERVAL: Long = 100

@OptIn(ExperimentalForeignApi::class)
class Poll: CommandLineClientMode("poll", { "TODO" }) {
    override fun run() {
        super.run()

        runBlocking {
            connectSocket()

            log(currentContext.loc.cli.poll_polling_server_for_events)

            while (true) {
                delay(POLL_INTERVAL)

                val wait_end: Long = getTimeMillis() + SERVER_EVENT_TIMEOUT_MS
                var events: List<JsonElement>? = null

                while (events == null && getTimeMillis() < wait_end) {
                    val message: List<String>? =
                        socket.recvStringMultipart(
                            (wait_end - getTimeMillis()).coerceAtLeast(ZMQ_NOBLOCK.toLong())
                        )?.let {
                            SpMsSocketApi.decode(it)
                        }

                    events = message?.map {
                        Json.decodeFromString(it)
                    }
                }

                if (events == null) {
                    throw SpMsCommandLineClientError(currentContext.loc.cli.errServerDidNotSendEvents(SERVER_EVENT_TIMEOUT_MS))
                }

                if (events.isNotEmpty()) {
                    println(events)
                }

                socket.sendStringMultipart(listOf(""))
            }
        }

        releaseSocket()
    }
}
