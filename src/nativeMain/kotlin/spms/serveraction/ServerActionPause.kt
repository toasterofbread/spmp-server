package spms.serveraction

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs

class ServerActionPause: ServerAction(
    name = { server_actions.pause_name },
    identifier = "pause",
    help = { server_actions.pause_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        server.player.pause()
        return null
    }
}
