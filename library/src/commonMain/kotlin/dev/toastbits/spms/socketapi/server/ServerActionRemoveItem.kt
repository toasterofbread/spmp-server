package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.player.Player

object ServerActionRemoveItem: ServerAction(
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
        execute(server.player, from)
        return null
    }

    fun execute(player: Player, from: Int) {
        player.removeItem(from)
    }
}
