package spms.serveraction

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs

class ServerActionPlayPause: ServerAction(
    identifier = "playPause",
    name = { server_actions.play_pause_name },
    help = { server_actions.play_pause_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        server.player.playPause()
        return null
    }
}
