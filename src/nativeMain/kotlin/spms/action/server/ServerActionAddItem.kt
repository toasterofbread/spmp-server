package spms.action.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.server.SpMs

class ServerActionAddItem: ServerAction(
    identifier = "addItem",
    name = { server_actions.add_item_name },
    help = { server_actions.add_item_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.String,
            true,
            "item_id",
            { server_actions.add_item_param_item_id }
        ),
        Parameter(
            Parameter.Type.Int,
            true,
            "index",
            { server_actions.add_item_param_index }
        )
    )
) {
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        val item_id: String = context.getParameterValue("item_id")!!.content
        val index: Int = context.getParameterValue("index")?.int ?: -1

        base.player.addItem(item_id, index)
        return null
    }
}
