package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.*
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.player.Player

object ServerActionSeekToItem: ServerAction(
    identifier = "seekToItem",
    name = { server_actions.seek_to_item_name },
    help = { server_actions.seek_to_item_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "index",
            { server_actions.seek_to_item_param_index }
        ),
        Parameter(
            Parameter.Type.Int,
            false,
            "position_ms",
            { server_actions.seek_to_item_param_position_ms }
        )
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val index: Int = context.getParameterValue("index")!!.jsonPrimitive.int
        val position_ms: Long? = context.getParameterValue("position_ms")?.jsonPrimitive?.longOrNull

        execute(server.player, index, position_ms)

        return null
    }

    fun execute(player: Player, index: Int, position_ms: Long? = null) {
        player.seekToItem(index, position_ms ?: 0)
    }
}
