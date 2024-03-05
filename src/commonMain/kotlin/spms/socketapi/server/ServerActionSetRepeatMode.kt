package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientID
import spms.socketapi.shared.SpMsPlayerRepeatMode

class ServerActionSetRepeatMode: ServerAction(
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
        server.player.setRepeatMode(SpMsPlayerRepeatMode.entries[repeat_mode_index])
        return null
    }
}
