package dev.toastbits.spms.client.cli.modes

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.client.cli.CommandLineClientMode
import dev.toastbits.spms.client.cli.SpMsCommandLineClientError
import dev.toastbits.spms.localisation.loc
import dev.toastbits.spms.socketapi.shared.SpMsSocketApi
import dev.toastbits.spms.server.CLIENT_HEARTBEAT_TARGET_PERIOD
import kotlin.time.*

private val SERVER_EVENT_TIMEOUT: Duration = with (Duration) { 10000.milliseconds }
private const val POLL_INTERVAL: Long = 100

class Poll: CommandLineClientMode("poll", { "TODO" }) {
    override fun run() {
        super.run()

        runBlocking {
            connectSocket()

            log(currentContext.loc.cli.poll_polling_server_for_events)

            var last_heartbeat: TimeMark = TimeSource.Monotonic.markNow()

            while (true) {
                val message: List<String>? =
                    socket.recvStringMultipart(null)?.let {
                        SpMsSocketApi.decode(it)
                    }

                val events: List<JsonElement> =
                    message.orEmpty().map { part ->
                        Json.decodeFromString(part)
                    }

                if (events.isNotEmpty()) {
                    println(events)
                }

                if (last_heartbeat.elapsedNow() > CLIENT_HEARTBEAT_TARGET_PERIOD) {
                    socket.sendStringMultipart(listOf(""))
                    last_heartbeat = TimeSource.Monotonic.markNow()
                }
            }
        }

        releaseSocket()
    }
}
