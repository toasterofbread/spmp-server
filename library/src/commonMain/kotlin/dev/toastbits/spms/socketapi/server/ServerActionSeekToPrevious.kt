package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID

class ServerActionSeekToPrevious: ServerAction(
    identifier = "seekToPrevious",
    name = { server_actions.seek_to_previous_name },
    help = { server_actions.seek_to_previous_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        server.player.seekToPrevious()
        return null
    }
}
