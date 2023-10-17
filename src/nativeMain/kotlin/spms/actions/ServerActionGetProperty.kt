package spms.actions

import kotlinx.serialization.json.JsonElement
import spms.SpMpServer

class ServerActionGetProperty: ServerAction(
    identifier = "getProperty",
    help = "TODO",
    parameters = listOf(
        // TODO
    )
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        TODO("Not yet implemented")
    }
}
