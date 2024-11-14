package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.player.Player

object ServerActionAddItem: ServerAction(
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
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val item_id: String = context.getParameterValue("item_id")!!.jsonPrimitive.content
        val index: Int = context.getParameterValue("index")?.jsonPrimitive?.int ?: -1

        execute(server.player, item_id, index)

        return null
    }

    fun execute(player: Player, item_id: String, index: Int) {
        player.addItem(item_id, index)
    }
}
