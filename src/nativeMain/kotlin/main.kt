import com.github.ajalt.clikt.core.subcommands
import controller.Controller
import spms.SpMs

fun String.toRed() =
    "\u001b[31m$this\u001b[0m"

fun main(args: Array<String>) = SpMs().subcommands(Controller.get()).main(args)
