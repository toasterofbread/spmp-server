package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.*
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.player.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object ServerActionSeekToPrevious: ServerAction(
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
        val repeat_threshold_ms: Long? = context.getParameterValue("repeat_threshold_ms")?.jsonPrimitive?.longOrNull

        val repeat_threshold: Duration? =
            if (repeat_threshold_ms == null || repeat_threshold_ms < 0) null
            else repeat_threshold_ms.milliseconds

        execute(server.player, repeat_threshold)

        return null
    }

    fun execute(player: Player, repeat_threshold: Duration? = null) {
        player.seekToPrevious(repeat_threshold)
    }
}
