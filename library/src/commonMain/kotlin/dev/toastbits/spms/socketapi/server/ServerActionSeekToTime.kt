package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.player.Player

object ServerActionSeekToTime: ServerAction(
    identifier = "seekToTime",
    name = { server_actions.seek_to_time_name },
    help = { server_actions.seek_to_time_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "position_ms",
            { server_actions.seek_to_time_param_position_ms }
        )
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val position_ms: Long = context.getParameterValue("position_ms")!!.jsonPrimitive.long

        execute(server.player, position_ms)

        return null
    }

    fun execute(player: Player, position_ms: Long) {
        player.seekToTime(position_ms)
    }
}
