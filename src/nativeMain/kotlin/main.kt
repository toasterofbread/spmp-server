import com.github.ajalt.clikt.core.subcommands
import spms.controller.Controller
import spms.SpMsCommand
import kotlin.system.exitProcess

fun String.toRed() =
    "\u001b[31m$this\u001b[0m"

fun main(args: Array<String>) {
    try {
        SpMsCommand().subcommands(Controller.get()).main(args)
    }
    catch (e: Throwable) {
        e.printStackTrace()
        exitProcess(1)
    }
}
