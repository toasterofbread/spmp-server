package spms.action.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import spms.server.SpMs

class ServerActionPlay: ServerAction(
    identifier = "play",
    name = { server_actions.play_name },
    help = { server_actions.play_help },
    parameters = emptyList()
) {
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        base.player.play()
        return null
    }
}
