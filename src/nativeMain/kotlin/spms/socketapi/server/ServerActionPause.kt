package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs
import spms.server.SpMsClientID

class ServerActionPause: ServerAction(
    name = { server_actions.pause_name },
    identifier = "pause",
    help = { server_actions.pause_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        server.player.pause()
        return null
    }
}
