package spms.action.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.server.SpMs

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
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        val from: Int = context.getParameterValue("from")!!.int
        val to: Int = context.getParameterValue("to")!!.int

        bsae.player.moveItem(from, to)
        return null
    }
}
