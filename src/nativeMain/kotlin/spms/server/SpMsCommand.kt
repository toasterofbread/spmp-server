package spms.server

import ICON_BYTES
import cinterop.indicator.createTrayIndicator
import cinterop.indicator.TrayIndicator
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import spms.localisation.SpMsLocalisation
import spms.localisation.loc
import kotlin.system.exitProcess
import platform.posix.getenv
import kotlinx.cinterop.toKString
import spms.*
import spms.socketapi.shared.SPMS_DEFAULT_PORT

const val PROJECT_URL: String = "https://github.com/toasterofbread/spmp-server"
const val BUG_REPORT_URL: String = PROJECT_URL + "/issues"

const val DEFAULT_ADDRESS: String = "127.0.0.1"
private const val POLL_INTERVAL_MS: Long = 100
private const val CLIENT_REPLY_TIMEOUT_MS: Long = 1000

@Suppress("OPT_IN_USAGE")
@OptIn(ExperimentalForeignApi::class)
fun createIndicator(coroutine_scope: CoroutineScope, loc: SpMsLocalisation, port: Int, endProgram: () -> Unit): TrayIndicator? {
    val icon_path: Path =
        when (Platform.osFamily) {
            OsFamily.LINUX -> "/tmp/ic_spmp.png".toPath()
            OsFamily.WINDOWS -> "${getenv("USERPROFILE")!!.toKString()}/AppData/Local/Temp/ic_spmp.png".toPath()
            else -> throw NotImplementedError(Platform.osFamily.name)
        }

    if (!FileSystem.SYSTEM.exists(icon_path)) {
        val parent: Path = icon_path.parent!!
        if (!FileSystem.SYSTEM.exists(parent)) {
            FileSystem.SYSTEM.createDirectories(parent, true)
        }

        FileSystem.SYSTEM.write(icon_path) {
            write(ICON_BYTES.toByteString())
        }
    }

    val indicator: TrayIndicator? = createTrayIndicator("SpMs (port $port)", icon_path.segments)
    indicator?.apply {
        addButton("Running on port $port", null)

        if (canOpenProcess()) {
            addButton(loc.server.indicator_button_open_client) {
                coroutine_scope.launch(Dispatchers.Default) {
                    openProcess("spmp", "r")
                }
            }
        }

        addButton(loc.server.indicator_button_stop_server) {
            endProgram()
        }
    }

    return indicator
}

class PlayerOptions: OptionGroup() {
    val enable_gui: Boolean by option("-g", "--gui").flag().help { context.loc.server.option_help_gui }
    val mute_on_start: Boolean by option("-m", "--mute").flag().help { context.loc.server.option_help_mute }
}

@OptIn(ExperimentalForeignApi::class)
class SpMsCommand: Command(
    name = "spms",
    help = { cli.command_help_root },
    is_default = true
) {
    private val port: Int by option("-p", "--port").int().default(SPMS_DEFAULT_PORT).help { context.loc.server.option_help_port }
    private val headless: Boolean by option("-x", "--headless").flag().help { context.loc.server.option_help_headless }
    private val player_options: PlayerOptions by PlayerOptions()

    override fun run() {
        super.run()

        if (halt) {
            exitProcess(0)
        }

        if (currentContext.invokedSubcommand != null) {
            return
        }

        var stop: Boolean = false

        memScoped {
            val server: SpMs = SpMs(this, headless, player_options.enable_gui)
            server.bind(port)

            println(localisation.server.serverBoundToPort(server.toString(), port))

            if (player_options.mute_on_start) {
                server.player.setVolume(0.0)
            }

            runBlocking {
                try {
                    val indicator: TrayIndicator? = createIndicator(this, localisation, port) {
                        stop = true
                    }

                    if (indicator != null) {
                        launch(Dispatchers.Default) {
                            indicator.show()
                        }
                    }

                    println("--- ${localisation.server.polling_started} ---")
                    while (!stop) {
                        server.poll(CLIENT_REPLY_TIMEOUT_MS)
                        delay(POLL_INTERVAL_MS)
                    }
                    println("--- ${localisation.server.polling_ended} ---")

                    server.release()
                    indicator?.release()

                    if (canEndProcess()) {
                        endProcess()
                    }
                }
                catch (e: Throwable) {
                    e.printStackTrace()
                    throw e
                }
            }
        }
    }
}
