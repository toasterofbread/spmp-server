package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientID

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
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val volume: Float = context.getParameterValue("volume")!!.jsonPrimitive.float
        server.player.setVolume(volume.toDouble())
        return null
    }
}
