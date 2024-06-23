package dev.toastbits.spms.socketapi.player

import dev.toastbits.spms.mpv.MpvClientImpl
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PlayerActionSetLocalFiles: PlayerAction(
    identifier = "setLocalFiles",
    name = { player_actions.set_local_files_name },
    help = { player_actions.set_local_files_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.String,
            true,
            "files",
            { player_actions.set_local_files_param_files }
        )
    )
) {
    override fun execute(player: MpvClientImpl, context: ActionContext): JsonElement? {
        player.clearLocalFiles()
        val files: JsonObject = context.getParameterValue("files")!!.jsonObject
        for (file in files) {
            player.addLocalFile(file.key, file.value.jsonPrimitive.content)
        }
        return null
    }
}
