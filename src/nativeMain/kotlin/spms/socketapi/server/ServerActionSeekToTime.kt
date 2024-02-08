package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientID

class ServerActionSeekToTime: ServerAction(
    identifier = "seekToTime",
    name = { server_actions.seek_to_time_name },
    help = { server_actions.seek_to_time_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "position_ms",
            { server_actions.seek_to_time_param_position_ms }
        )
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val position_ms: Long = context.getParameterValue("position_ms")!!.jsonPrimitive.long
        server.player.seekToTime(position_ms)
        return null
    }
}
