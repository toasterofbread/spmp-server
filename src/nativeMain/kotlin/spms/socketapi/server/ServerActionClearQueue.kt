package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientID

class ServerActionClearQueue: ServerAction(
    identifier = "clearQueue",
    name = { server_actions.clear_queue_name },
    help = { server_actions.clear_queue_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        server.player.clearQueue()
        return null
    }
}
