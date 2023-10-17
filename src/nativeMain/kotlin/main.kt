import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import controller.SpMsController
import spms.SpMs

val TEST_SONGS = listOf(
    "dQw4w9WgXcQ",
    "7JANm3jOb2k",
    "BeLsFW4m194",
    "0MZJduzi1OU",
    "PWbRleMGagU"
)

abstract class Command(name: String, is_default: Boolean = false): CliktCommand(
    name = name,
    invokeWithoutSubcommand = is_default,
    epilog = "Report bugs at https://github.com/toasterofbread/spmp-server/issues"
)

fun main(args: Array<String>) {
    println("--- main(${args.toList()}) ---")

    SpMs().subcommands(SpMsController.get()).main(args)

    println("--- main() finished ---")
}

fun String.toRed() =
    "\u001b[31m$this\u001b[0m"
