package spms.serveraction

import kotlinx.serialization.json.JsonElement
import spms.SpMs

class ServerActionSeekToPrevious: ServerAction(
    identifier = "seekToPrevious",
    name = { server_actions.seek_to_previous_name },
    help = { server_actions.seek_to_previous_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        server.mpv.seekToPrevious()
        return null
    }
}
