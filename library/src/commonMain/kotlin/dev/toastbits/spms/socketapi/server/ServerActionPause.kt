package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.player.Player

object ServerActionPause: ServerAction(
    name = { server_actions.pause_name },
    identifier = "pause",
    help = { server_actions.pause_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        execute(server.player)
        return null
    }

    fun execute(player: Player) {
        player.pause()
    }
}
