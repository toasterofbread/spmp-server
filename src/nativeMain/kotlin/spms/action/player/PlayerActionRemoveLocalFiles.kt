package spms.action.player

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import cinterop.mpv.MpvClientImpl

class PlayerActionRemoveLocalFiles: PlayerAction(
    identifier = "removeLocalFiles",
    name = { server_actions.remove_local_files_name },
    help = { server_actions.remove_local_files_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.String,
            true,
            "ids",
            { server_actions.remove_local_files_param_ids }
        )
    )
) {
    override fun execute(base: MpvClientImpl, context: ActionContext): JsonElement? {
        TODO()
    }
}
