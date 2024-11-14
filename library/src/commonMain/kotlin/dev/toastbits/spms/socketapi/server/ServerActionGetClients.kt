package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.socketapi.shared.SpMsClientInfo

object ServerActionGetClients: ServerAction(
    identifier = "clients",
    name = { server_actions.clients_name },
    help = { server_actions.clients_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement {
        val clients: List<SpMsClientInfo> = execute(server, client)
        return Json.encodeToJsonElement(clients)
    }

    fun execute(server: SpMs, client: SpMsClientID): List<SpMsClientInfo> {
        return server.getClients(client)
    }
}
