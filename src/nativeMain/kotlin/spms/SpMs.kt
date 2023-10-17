package spms

import Command
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

const val DEFAULT_PORT: Int = 3973
private const val POLL_INTERVAL_MS: Long = 100
private const val CLIENT_REPLY_TIMEOUT_MS: Long = 1000

@OptIn(ExperimentalForeignApi::class)
class SpMs: Command(
    name = "spms",
    is_default = true
) {
    private val port: Int by option("-p", "--port").int().default(DEFAULT_PORT).help("The port on which to bind the server interface")
    private val enable_gui: Boolean by option("-g", "--gui").flag()
    private val mute_on_start: Boolean by option("-m", "--mute").flag()

    override fun run() {
        if (currentContext.invokedSubcommand != null) {
            return
        }

        memScoped {
            val server = SpMpServer(this, !enable_gui)
            server.bind(port)

            if (mute_on_start) {
                server.mpv.setVolume(0f)
            }

            runBlocking {
                println("--- Polling started ---")
                while (server.poll(CLIENT_REPLY_TIMEOUT_MS)) {
                    delay(POLL_INTERVAL_MS)
                }
                println("--- Polling ended ---")
            }

            server.release()
        }
    }
}
