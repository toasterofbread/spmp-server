import com.github.ajalt.clikt.core.subcommands
import controller.Controller
import spms.SpMs
import kotlin.system.exitProcess

fun String.toRed() =
    "\u001b[31m$this\u001b[0m"

fun main(args: Array<String>) {
    try {
        SpMs().subcommands(Controller.get()).main(args)
    }
    catch (e: Throwable) {
        e.printStackTrace()
        exitProcess(1)
    }
}
