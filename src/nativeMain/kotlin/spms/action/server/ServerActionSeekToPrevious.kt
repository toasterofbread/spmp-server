package spms.action.server

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs

class ServerActionSeekToPrevious: ServerAction(
    identifier = "seekToPrevious",
    name = { server_actions.seek_to_previous_name },
    help = { server_actions.seek_to_previous_help },
    parameters = emptyList()
) {
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        base.player.seekToPrevious()
        return null
    }
}
