import com.github.ajalt.clikt.core.CliktCommand

abstract class Command(name: String, is_default: Boolean = false, help: String? = null): CliktCommand(
    name = name,
    invokeWithoutSubcommand = is_default,
    help = help ?: "",
    epilog = "Report bugs at https://github.com/toasterofbread/spmp-server/issues"
)
