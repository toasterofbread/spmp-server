package spms.socketapi.player

import cinterop.mpv.MpvClientImpl
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PlayerActionAddLocalFiles: PlayerAction(
    identifier = "addLocalFiles",
    name = { player_actions.add_local_files_name },
    help = { player_actions.add_local_files_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.String,
            true,
            "files",
            { player_actions.add_local_files_param_files }
        )
    )
) {
    override fun execute(player: MpvClientImpl, context: ActionContext): JsonElement? {
        val files: JsonObject = context.getParameterValue("files")!!.jsonObject
        for (file in files) {
            player.addLocalFile(file.key, file.value.jsonPrimitive.content)
        }
        return null
    }
}
