package spms.socketapi.player

import cinterop.mpv.MpvClientImpl
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class PlayerActionRemoveLocalFiles: PlayerAction(
    identifier = "removeLocalFiles",
    name = { player_actions.remove_local_files_name },
    help = { player_actions.remove_local_files_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.String,
            true,
            "ids",
            { player_actions.remove_local_files_param_ids }
        )
    )
) {
    override fun execute(player: MpvClientImpl, context: ActionContext): JsonElement? {
        val item_ids: JsonArray = context.getParameterValue("ids")!!.jsonArray
        for (id in item_ids) {
            player.removeLocalFile(id.jsonPrimitive.content)
        }
        return null
    }
}
