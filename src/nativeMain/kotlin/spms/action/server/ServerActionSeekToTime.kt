package spms.action.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.long
import spms.server.SpMs

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
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        val position_ms: Long = context.getParameterValue("position_ms")!!.long
        base.player.seekToTime(position_ms)
        return null
    }
}
