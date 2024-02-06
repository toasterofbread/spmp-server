package spms.action.server

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs

class ServerActionSeekToNext: ServerAction(
    identifier = "seekToNext",
    name = { server_actions.seek_to_next_name },
    help = { server_actions.seek_to_next_help },
    parameters = emptyList()
) {
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        base.player.seekToNext()
        return null
    }
}
