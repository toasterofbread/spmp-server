package dev.toastbits.spms.client.cli.modes

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.client.cli.CommandLineClientMode
import dev.toastbits.spms.client.cli.SpMsCommandLineClientError
import dev.toastbits.spms.localisation.loc
import dev.toastbits.spms.socketapi.shared.SpMsSocketApi
import kotlin.time.*

private val SERVER_EVENT_TIMEOUT: Duration = with (Duration) { 10000.milliseconds }
private const val POLL_INTERVAL: Long = 100

class Poll: CommandLineClientMode("poll", { "TODO" }) {
    override fun run() {
        super.run()

        runBlocking {
            connectSocket()

            log(currentContext.loc.cli.poll_polling_server_for_events)

            while (true) {
                delay(POLL_INTERVAL)

                val wait_start: TimeMark = TimeSource.Monotonic.markNow()
                var events: List<JsonElement>? = null

                while (events == null && wait_start.elapsedNow() < SERVER_EVENT_TIMEOUT) {
                    val message: List<String>? = with (Duration) {
                        socket.recvStringMultipart(
                            (SERVER_EVENT_TIMEOUT - wait_start.elapsedNow()).inWholeMilliseconds.coerceAtLeast(1L).milliseconds
                        )?.let {
                            SpMsSocketApi.decode(it)
                        }
                    }

                    events = message?.map {
                        Json.decodeFromString(it)
                    }
                }

                if (events == null) {
                    throw SpMsCommandLineClientError(currentContext.loc.cli.errServerDidNotSendEvents(SERVER_EVENT_TIMEOUT))
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
