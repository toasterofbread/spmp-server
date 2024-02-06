package spms.action.server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import spms.server.SpMs

class ServerActionGetClients: ServerAction(
    identifier = "clients",
    name = { server_actions.clients_name },
    help = { server_actions.clients_help },
    parameters = emptyList()
) {
    override fun execute(base: SpMs, context: ActionContext): JsonElement {
        return Json.encodeToJsonElement(base.getClients(context.client))
    }
}
