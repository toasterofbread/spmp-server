package spms.action.server

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs

class ServerActionGetProperty: ServerAction(
    identifier = "getProperty",
    name = { "TODO" },
    help = { "TODO" },
    hidden = true,
    parameters = listOf(
        // TODO
    )
) {
    override fun execute(base: SpMs, context: ActionContext): JsonElement? {
        TODO("Not yet implemented")
    }
}
