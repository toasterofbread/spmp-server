import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import libzmq.*
import spms.SpMpServer

val TEST_SONGS = listOf(
    "dQw4w9WgXcQ",
    "7JANm3jOb2k",
    "BeLsFW4m194",
    "0MZJduzi1OU",
    "PWbRleMGagU"
)

const val PORT: Int = 3973
const val POLL_INTERVAL_MS: Long = 100
const val CLIENT_REPLY_TIMEOUT_MS: Long = 1000 // TODO | Test this

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    println("--- main(${args.toList()}) ---")

    val enable_gui: Boolean = args.contains("--enable-gui") || args.contains("-g")
    val mute_on_start: Boolean = args.contains("--mute-on-start") || args.contains("-m")

    memScoped {
        val server = SpMpServer(this, !enable_gui)
        server.bind(PORT)

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

    println("--- main() finished ---")
}
