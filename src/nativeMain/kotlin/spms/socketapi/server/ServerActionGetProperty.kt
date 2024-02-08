package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientID

class ServerActionGetProperty: ServerAction(
    identifier = "getProperty",
    name = { "TODO" },
    help = { "TODO" },
    hidden = true,
    parameters = listOf(
        // TODO
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        TODO("Not yet implemented")
    }
}
