package spms.serveraction

import kotlinx.serialization.json.JsonElement
import spms.SpMs

class ServerActionGetProperty: ServerAction(
    identifier = "getProperty",
    name = { "TODO" },
    help = { "TODO" },
    parameters = listOf(
        // TODO
    )
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        TODO("Not yet implemented")
    }
}
