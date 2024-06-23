package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID

class ServerActionSeekToNext: ServerAction(
    identifier = "seekToNext",
    name = { server_actions.seek_to_next_name },
    help = { server_actions.seek_to_next_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        server.player.seekToNext()
        return null
    }
}
