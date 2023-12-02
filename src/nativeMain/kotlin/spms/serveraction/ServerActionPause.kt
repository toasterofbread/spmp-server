package spms.serveraction

import kotlinx.serialization.json.JsonElement
import spms.SpMs

class ServerActionPause: ServerAction(
    name = { server_actions.pause_name },
    identifier = "pause",
    help = { server_actions.pause_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        server.mpv.pause()
        return null
    }
}
