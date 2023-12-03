package spms.serveraction

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import spms.server.SpMs

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
        )
    )
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        server.onClientReadyToPlay(
            context.client,
            context.getParameterValue("item_index")!!.jsonPrimitive.int,
            context.getParameterValue("item_id")!!.jsonPrimitive.content
        )
        return null
    }
}
