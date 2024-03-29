package spms.client.cli.modes

import cinterop.zmq.ZmqSocket
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import libzmq.ZMQ_NOBLOCK
import spms.socketapi.Action
import spms.socketapi.shared.SpMsSocketApi
import spms.client.cli.CommandLineClientMode
import spms.client.cli.SERVER_REPLY_TIMEOUT_MS
import spms.localisation.loc
import spms.socketapi.shared.SpMsServerHandshake
import spms.socketapi.shared.SpMsActionReply
import spms.socketapi.shared.SPMS_EXPECT_REPLY_CHAR
import spms.socketapi.player.PlayerAction
import spms.socketapi.server.ServerAction
import toRed
import kotlin.system.getTimeMillis
import kotlinx.cinterop.ExperimentalForeignApi

private fun CommandLineClientMode.jsonModeOption() =
    option("-j", "--json").flag().help { context.loc.server_actions.option_help_json }

class Run private constructor(): CommandLineClientMode("run", { "TODO" }) {
    private val json_mode: Boolean by jsonModeOption()

    override fun run() {
        super.run()

        val mode: ActionCommandLineClientMode? = currentContext.invokedSubcommand as? ActionCommandLineClientMode
        mode?.parent_json_mode = json_mode
    }

    companion object {
        fun get(): Run {
            return Run().subcommands(
                listOf(
                    ActionCommandLineClientMode(null),
                    ActionCommandLineClientMode(null, "---Server---")
                )
                + ServerAction.getAll().map { ActionCommandLineClientMode(it) }
                + listOf(
                    ActionCommandLineClientMode(null),
                    ActionCommandLineClientMode(null, "---Player---")
                )
                + PlayerAction.getAll().map { ActionCommandLineClientMode(it) }
            )
        }
    }
}

class ActionCommandLineClientMode(
    val action: Action?,
    name: String = action?.identifier ?: ""
): CommandLineClientMode(
    name,
    action?.help,
    hidden = action?.hidden ?: false
) {
    private val json_mode: Boolean by jsonModeOption()
    internal var parent_json_mode: Boolean = false

    init {
        for (parameter in action?.parameters.orEmpty()) {
            val initial_argument: RawArgument = argument(
                name = parameter.identifier,
            )
            initial_argument.help { parameter.help(context.loc) }

            val argument: Argument =
                when (parameter.type) {
                    Action.Parameter.Type.String -> initial_argument
                    Action.Parameter.Type.Int -> initial_argument.int()
                    Action.Parameter.Type.Float -> initial_argument.float()
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

        if (action == null) {
            currentContext.parent?.command?.echoFormattedHelp()
            return
        }

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

        val reply: SpMsActionReply? = executeActionOnSocket(
            action,
            parameter_values,
            SERVER_REPLY_TIMEOUT_MS,
            currentContext, silent = silent
        )

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

    @OptIn(ExperimentalForeignApi::class)
    private fun executeActionOnSocket(
        action: Action,
        parameter_values: List<JsonPrimitive>,
        reply_timeout_ms: Long?,
        context: Context,
        silent: Boolean = false
    ): SpMsActionReply? {
        if (!silent) {
            println(context.loc.server_actions.sendingActionToServer(action.identifier))
        }

        val reply = connectSocket(
            handshake_actions = listOf(SPMS_EXPECT_REPLY_CHAR + action.identifier, Json.encodeToString(parameter_values))
        )

        if (!silent) {
            println(context.loc.server_actions.actionSentAndWaitingForReply(action.identifier))
        }

        val timeout_end: Long? = reply_timeout_ms?.let { getTimeMillis() + it }
        do {
            if (!reply.isNullOrEmpty()) {
                if (!silent) {
                    println(context.loc.server_actions.receivedReplyFromServer(action.identifier))
                }

                val decoded: String = SpMsSocketApi.decode(reply).first()
                try {
                    val parsed_reply: SpMsServerHandshake = Json.decodeFromString(decoded)
                    check(!parsed_reply.action_replies.isNullOrEmpty()) {
                        "Got empty reply from server"
                    }
                    return parsed_reply.action_replies.first()
                }
                catch (e: Throwable) {
                    throw RuntimeException("JSON decoding server reply failed $decoded", e)
                }
            }
            else if (!silent) {
                println(context.loc.server_actions.receivedEmptyReplyFromServer(action.identifier))
            }
        } while (timeout_end == null || getTimeMillis() < timeout_end)

        return null
    }
}
