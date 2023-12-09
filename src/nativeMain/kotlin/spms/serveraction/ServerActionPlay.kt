package spms.serveraction

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import spms.server.SpMs

class ServerActionPlay: ServerAction(
    identifier = "play",
    name = { server_actions.play_name },
    help = { server_actions.play_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        server.player.play()
        return null
    }
}
