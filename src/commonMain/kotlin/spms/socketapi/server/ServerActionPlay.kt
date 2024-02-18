package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientID

class ServerActionPlay: ServerAction(
    identifier = "play",
    name = { server_actions.play_name },
    help = { server_actions.play_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        server.player.play()
        return null
    }
}
