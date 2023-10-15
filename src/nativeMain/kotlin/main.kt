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

    memScoped {
        val server = SpMpServer(this)
        server.bind(PORT)

        runBlocking {
            println("--- Polling started ---")
            while (true) {
                server.poll(CLIENT_REPLY_TIMEOUT_MS)
                delay(POLL_INTERVAL_MS)
            }
        }

        server.release()
    }

    println("--- main() finished ---")
}
