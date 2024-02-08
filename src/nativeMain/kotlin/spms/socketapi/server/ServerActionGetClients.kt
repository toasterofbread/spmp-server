package spms.socketapi.server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import spms.server.SpMs
import spms.server.SpMsClientID

class ServerActionGetClients: ServerAction(
    identifier = "clients",
    name = { server_actions.clients_name },
    help = { server_actions.clients_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement {
        val clients = server.getClients(client)
        println("CLIENTS: $clients")
        return Json.encodeToJsonElement(clients)
    }
}
