package spms

import Command
import ICON_BYTES
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import indicator.TrayIndicator
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import libappindicator.*
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

const val DEFAULT_PORT: Int = 3973
private const val POLL_INTERVAL_MS: Long = 100
private const val CLIENT_REPLY_TIMEOUT_MS: Long = 1000

@Suppress("OPT_IN_USAGE")
@OptIn(ExperimentalForeignApi::class)
fun createIndicator(coroutine_scope: CoroutineScope, endProgram: () -> Unit): TrayIndicator? {
    val icon_path: Path =
        when (Platform.osFamily) {
            OsFamily.LINUX -> "/tmp/ic_spmp.png".toPath()
            else -> throw NotImplementedError(Platform.osFamily.name)
        }

    if (!FileSystem.SYSTEM.exists(icon_path)) {
        FileSystem.SYSTEM.write(icon_path) {
            write(ICON_BYTES.toByteString())
        }
    }

    val indicator: TrayIndicator? = TrayIndicator.create("SpMs", icon_path.segments)
    indicator?.apply {
        addButton("Open client") {
            coroutine_scope.launch(Dispatchers.Default) {
                popen("spmp", "r")
            }
        }

        addButton("Stop") {
            endProgram()
        }
    }

    return indicator
}

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

        var stop: Boolean = false

        memScoped {
            val server = SpMpServer(this, !enable_gui)
            server.bind(port)

            if (mute_on_start) {
                server.mpv.setVolume(0f)
            }

            runBlocking {
                val indicator: TrayIndicator? = createIndicator(this) {
                    stop = true
                }

                if (indicator != null) {
                    launch(Dispatchers.Default) {
                        indicator.show()
                    }
                }

                println("--- Polling started ---")
                while (server.poll(CLIENT_REPLY_TIMEOUT_MS) && !stop) {
                    delay(POLL_INTERVAL_MS)
                }
                println("--- Polling ended ---")

                server.release()
                indicator?.release()
                kill(0, SIGTERM)
            }
        }
    }

    companion object {
        val applicationName: String = "spmp-server"
    }
}
