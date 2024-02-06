package spms.action.player

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import cinterop.mpv.MpvClientImpl

class PlayerActionAddLocalFiles: PlayerAction(
    identifier = "addLocalFiles",
    name = { server_actions.add_local_files_name },
    help = { server_actions.add_local_files_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.String,
            true,
            "files",
            { server_actions.add_local_files_param_files }
        )
    )
) {
    override fun execute(base: MpvClientImpl, context: ActionContext): JsonElement? {
        TODO()
    }
}
