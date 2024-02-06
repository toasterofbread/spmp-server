package spms.client.cli.modes

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.Argument
import com.github.ajalt.clikt.parameters.arguments.ProcessedArgumentImpl
import com.github.ajalt.clikt.parameters.arguments.RawArgument
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.serialization.json.JsonPrimitive
import spms.client.cli.CommandLineClientMode
import spms.client.cli.SERVER_REPLY_TIMEOUT_MS
import spms.localisation.loc
import spms.server.SpMs
import spms.action.server.ServerAction
import toRed

private fun CommandLineClientMode.jsonModeOption() =
    option("-j", "--json").flag().help { context.loc.server_actions.option_help_json }

class Run private constructor(): CommandLineClientMode("run", { "TODO" }) {
    private val json_mode: Boolean by jsonModeOption()

    override fun run() {
        super.run()

        val mode: ServerActionCommandLineClientMode? = currentContext.invokedSubcommand as? ServerActionCommandLineClientMode
        mode?.parent_json_mode = json_mode
    }

    companion object {
        fun get(): Run =
            Run().subcommands(
                ServerAction.getAll().map { action: ServerAction ->
                    ServerActionCommandLineClientMode(action)
                }
            )
    }
}

class ServerActionCommandLineClientMode(
    val action: ServerAction
): CommandLineClientMode(action.identifier, action.help, hidden = action.hidden) {
    private val json_mode: Boolean by jsonModeOption()
    internal var parent_json_mode: Boolean = false

    init {
        for (parameter in action.parameters) {
            val initial_argument: RawArgument = argument(
                name = parameter.identifier,
            )
            initial_argument.help { parameter.help(context.loc) }

            val argument: Argument =
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
        super.run()

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

        connectSocket()

        val reply: SpMs.ActionReply? = action.executeOnRemoteServer(socket, parameter_values, SERVER_REPLY_TIMEOUT_MS, currentContext, silent = silent)
        if (reply == null) {
            throw CliktError(currentContext.loc.server_actions.replyNotReceived(SERVER_REPLY_TIMEOUT_MS).toRed())
        }
        else if (reply.success) {
            if (json_mode || parent_json_mode) {
                println(reply.result)
            }
            else {
                if (!silent) {
                    println(currentContext.loc.server_actions.server_completed_request_successfully)
                }
                if (reply.result != null) {
                    println(action.formatResult(reply.result, currentContext))
                }
            }
        }
        else {
            releaseSocket()
            throw CliktError(currentContext.loc.server_actions.serverDidNotCompleteRequest(reply.error.toString(), reply.error_cause.toString()).toRed())
        }

        releaseSocket()
    }
}
