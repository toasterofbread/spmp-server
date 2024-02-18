package spms.socketapi.player

import cinterop.mpv.MpvClientImpl
import kotlinx.serialization.json.*

class PlayerActionSetAuthInfo: PlayerAction(
    identifier = "setAuthInfo",
    name = { player_actions.set_auth_info_name },
    help = { player_actions.set_auth_info_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.String,
            true,
            "headers",
            { player_actions.set_auth_info_param_headers }
        )
    )
) {
    override fun execute(player: MpvClientImpl, context: ActionContext): JsonElement? {
        val param: JsonElement = context.getParameterValue("headers")!!
        if (param is JsonNull) {
            player.setAuthHeaders(null)
            return null
        }
        
        val headers: Map<String, String> =
            param.jsonObject.entries.associate {
                it.key to it.value.jsonPrimitive.content
            }
        player.setAuthHeaders(headers)
        return null
    }
}
