package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.player.Player

object ServerActionSetRepeatMode: ServerAction(
    identifier = "setRepeatMode",
    name = { server_actions.set_repeat_mode_name },
    help = { server_actions.set_repeat_mode_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "repeat_mode",
            { server_actions.set_repeat_mode_param_repeat_mode }
        )
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val repeat_mode_index: Int = context.getParameterValue("repeat_mode")!!.jsonPrimitive.int
        val repeat_mode: SpMsPlayerRepeatMode = SpMsPlayerRepeatMode.entries[repeat_mode_index]

        execute(server.player, repeat_mode)

        return null
    }

    fun execute(player: Player, repeat_mode: SpMsPlayerRepeatMode) {
        player.setRepeatMode(repeat_mode)
    }
}
