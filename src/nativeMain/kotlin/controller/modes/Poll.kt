package controller.modes

import controller.SpMsControllerError
import controller.SpMsControllerModeCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import libzmq.ZMQ_NOBLOCK
import kotlin.system.getTimeMillis

private const val SERVER_EVENT_TIMEOUT_MS: Long = 10000
private const val POLL_INTERVAL: Long = 100

class Poll: SpMsControllerModeCommand("poll") {
    override fun run() {
        runBlocking {
            println("Polling server for events...")
            while (true) {
                delay(POLL_INTERVAL)

                val wait_end: Long = getTimeMillis() + SERVER_EVENT_TIMEOUT_MS
                var events: List<String>? = null

                while (events == null && getTimeMillis() < wait_end) {
                    events = socket
                        .recvStringMultipart(
                            (wait_end - getTimeMillis()).coerceAtLeast(ZMQ_NOBLOCK.toLong())
                        )
                        ?.mapNotNull { event ->
                            event.removeSuffix("\u0000").takeIf { it.isNotEmpty() }
                        }
                }

                if (events == null) {
                    throw SpMsControllerError("Server did not send events within timeout (${SERVER_EVENT_TIMEOUT_MS}ms)")
                }

                if (events.isNotEmpty()) {
                    println("Events: $events")
                }

                socket.sendStringMultipart(listOf(""))
            }
        }

        releaseSocket()
    }
}
