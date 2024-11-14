package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.player.Player

object ServerActionClearQueue: ServerAction(
    identifier = "clearQueue",
    name = { server_actions.clear_queue_name },
    help = { server_actions.clear_queue_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        execute(server.player)
        return null
    }

    fun execute(player: Player) {
        player.clearQueue()
    }
}
