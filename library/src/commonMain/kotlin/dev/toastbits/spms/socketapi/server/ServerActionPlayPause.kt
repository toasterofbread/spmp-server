package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID

class ServerActionPlayPause: ServerAction(
    identifier = "playPause",
    name = { server_actions.play_pause_name },
    help = { server_actions.play_pause_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        server.player.playPause()
        return null
    }
}
