package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.*
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID

class ServerActionSeekToPrevious: ServerAction(
    identifier = "seekToPrevious",
    name = { server_actions.seek_to_previous_name },
    help = { server_actions.seek_to_previous_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            false,
            "repeat_threshold_ms",
            { server_actions.seek_to_previous_param_repeat_threshold_ms }
        )
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val repeat_threshold_ms: Long = context.getParameterValue("repeat_threshold_ms")?.jsonPrimitive?.longOrNull ?: -1

        if (
            repeat_threshold_ms == 0L
            || (repeat_threshold_ms > 0L && server.player.current_position_ms >= repeat_threshold_ms)
        ) {
            server.player.seekToTime(0)
        }
        else {
            server.player.seekToPrevious()
        }

        return null
    }
}
