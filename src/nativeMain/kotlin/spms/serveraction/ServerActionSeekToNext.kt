package spms.serveraction

import kotlinx.serialization.json.JsonElement
import spms.SpMs

class ServerActionSeekToNext: ServerAction(
    identifier = "seekToNext",
    name = { server_actions.seek_to_next_name },
    help = { server_actions.seek_to_next_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        server.mpv.seekToNext()
        return null
    }
}
