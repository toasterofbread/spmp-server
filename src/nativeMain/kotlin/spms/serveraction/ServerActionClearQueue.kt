package spms.serveraction

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs

class ServerActionClearQueue: ServerAction(
    identifier = "clearQueue",
    name = { server_actions.clear_queue_name },
    help = { server_actions.clear_queue_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        server.player.clearQueue()
        return null
    }
}
