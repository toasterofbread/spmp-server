package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientID

class ServerActionRemoveItem: ServerAction(
    identifier = "removeItem",
    name = { server_actions.remove_item_name },
    help = { server_actions.remove_item_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "from",
            { server_actions.remove_param_from }
        )
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val from: Int = context.getParameterValue("from")!!.jsonPrimitive.int
        server.player.removeItem(from)
        return null
    }
}
