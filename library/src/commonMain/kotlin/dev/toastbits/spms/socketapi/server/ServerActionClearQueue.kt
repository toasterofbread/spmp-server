package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID

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
