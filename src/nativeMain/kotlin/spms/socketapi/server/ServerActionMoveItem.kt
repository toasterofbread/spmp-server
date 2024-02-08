package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import spms.server.SpMs
import spms.server.SpMsClientID

class ServerActionMoveItem: ServerAction(
    identifier = "moveItem",
    name = { server_actions.move_item_name },
    help = { server_actions.move_item_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "from",
            { server_actions.move_item_param_from }
        ),
        Parameter(
            Parameter.Type.Int,
            true,
            "to",
            { server_actions.move_item_param_to }
        )
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val from: Int = context.getParameterValue("from")!!.jsonPrimitive.int
        val to: Int = context.getParameterValue("to")!!.jsonPrimitive.int

        server.player.moveItem(from, to)
        return null
    }
}
