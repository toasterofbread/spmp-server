package spms.serveraction

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.server.SpMs

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
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        val from: Int = context.getParameterValue("from")!!.int
        server.player.removeItem(from)
        return null
    }
}
