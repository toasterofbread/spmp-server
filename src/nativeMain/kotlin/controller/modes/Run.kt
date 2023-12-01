package controller.modes

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.ProcessedArgumentImpl
import com.github.ajalt.clikt.parameters.arguments.RawArgument
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.int
import controller.ControllerMode
import controller.SERVER_REPLY_TIMEOUT_MS
import kotlinx.serialization.json.JsonPrimitive
import spms.SpMpServer
import spms.actions.ServerAction
import toRed

private fun ControllerMode.jsonModeOption() = 
    option("-j", "--json", help = "Output result data in JSON format if possible").flag()

class Run private constructor(): ControllerMode("run") {
    private val json_mode: Boolean by jsonModeOption()

    override fun run() {
        val mode: ServerActionControllerMode? = currentContext.invokedSubcommand as? ServerActionControllerMode
        mode?.parent_json_mode = json_mode
        mode?.verbose = context.verbose
    }

    companion object {
        fun get(): Run =
            Run().subcommands(
                ServerAction.getAll().map { action: ServerAction ->
                    ServerActionControllerMode(action)
                }
            )
    }
}

class ServerActionControllerMode(val action: ServerAction): ControllerMode(action.identifier, action.help) {
    private val json_mode: Boolean by jsonModeOption()

    internal var parent_json_mode: Boolean = false
    internal var verbose: Boolean = false

    init {
        for (parameter in action.parameters) {
            val initial_argument: RawArgument = argument(
                name = parameter.identifier,
                help = parameter.help
            )

            val argument =
                when (parameter.type) {
                    ServerAction.Parameter.Type.String -> initial_argument
                    ServerAction.Parameter.Type.Int -> initial_argument.int()
                    ServerAction.Parameter.Type.Float -> initial_argument.float()
                }
                .let {
                    if (parameter.required) it
                    else it.optional()
                }

            registerArgument(argument)
        }
    }

    override fun run() {
        val parameter_values: List<JsonPrimitive> = registeredArguments().mapNotNull { argument ->
            val value: Any = (argument as ProcessedArgumentImpl<*, *>).value ?: return@mapNotNull null
            when (value) {
                is String -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Float -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> throw NotImplementedError(value::class.toString())
            }
        }

        val reply: SpMpServer.ActionReply? = action.executeOnRemoteServer(socket, parameter_values, SERVER_REPLY_TIMEOUT_MS, verbose = verbose)
        if (reply == null) {
            throw CliktError("Did not receive valid reply from server within timeout (${SERVER_REPLY_TIMEOUT_MS}ms)".toRed())
        }
        else if (reply.success) {
            if (json_mode || parent_json_mode) {
                println(reply.result)
            }
            else {
                if (verbose) {
                    println("The server completed the request successfully")
                }
                if (reply.result != null) {
                    println(action.formatResult(reply.result))
                }
            }
        }
        else {
            releaseSocket()
            throw CliktError("The server was not able to complete the request\nError: ${reply.error}\nCause: ${reply.error_cause}")
        }

        releaseSocket()
    }
}
