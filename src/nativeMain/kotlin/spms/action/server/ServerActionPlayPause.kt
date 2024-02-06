package spms.action.server

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs

class ServerActionPlayPause: ServerAction(
    identifier = "playPause",
    name = { server_actions.play_pause_name },
    help = { server_actions.play_pause_help },
    parameters = emptyList()
) {
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        base.player.playPause()
        return null
    }
}
