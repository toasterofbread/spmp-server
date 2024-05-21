package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import kotlin.time.Duration

class ServerActionReadyToPlay: ServerAction(
    identifier = "readyToPlay",
    name = { server_actions.ready_to_play_name },
    help = { server_actions.ready_to_play_help },
    hidden = true,
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "item_index",
            { server_actions.ready_to_play_param_item_index }
        ),
        Parameter(
            Parameter.Type.String,
            true,
            "item_id",
            { server_actions.ready_to_play_param_item_id }
        ),
        Parameter(
            Parameter.Type.Int,
            true,
            "item_duration_ms",
            { server_actions.ready_to_play_param_item_duration_ms }
        )
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        server.onClientReadyToPlay(
            client,
            context.getParameterValue("item_index")!!.jsonPrimitive.int,
            context.getParameterValue("item_id")!!.jsonPrimitive.content,
            with (Duration) {
                context.getParameterValue("item_duration_ms")!!.jsonPrimitive.long.milliseconds
            }
        )
        return null
    }
}
