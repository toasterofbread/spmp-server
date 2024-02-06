package spms.action.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.float
import spms.server.SpMs

class ServerActionSetVolume: ServerAction(
    identifier = "setVolume",
    name = { server_actions.set_volume_name },
    help = { server_actions.set_volume_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Float,
            true,
            "volume",
            { server_actions.set_volume_param_volume }
        )
    )
) {
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        val volume: Float = context.getParameterValue("volume")!!.float
        base.player.setVolume(volume.toDouble())
        return null
    }
}
