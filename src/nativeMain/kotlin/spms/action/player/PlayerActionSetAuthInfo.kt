package spms.action.player

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import cinterop.mpv.MpvClientImpl

class PlayerActionSetAuthInfo: PlayerAction(
    identifier = "setAuthInfo",
    name = { server_actions.set_auth_info_name },
    help = { server_actions.set_auth_info_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.String,
            true,
            "headers",
            { server_actions.set_auth_info_param_headers }
        )
    )
) {
    override fun execute(base: MpvClientImpl, context: ActionContext): JsonElement? {
        TODO()
    }
}
