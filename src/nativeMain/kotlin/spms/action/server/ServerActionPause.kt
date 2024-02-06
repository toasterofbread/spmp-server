package spms.action.server

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs

class ServerActionPause: ServerAction(
    name = { server_actions.pause_name },
    identifier = "pause",
    help = { server_actions.pause_help },
    parameters = emptyList()
) {
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        base.player.pause()
        return null
    }
}
