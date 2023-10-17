package spms.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import mpv.getCurrentStatusJson
import spms.SpMpServer

class ServerActionStatus: ServerAction(
    identifier = "status",
    help = "Get detailed information about the server's current status",
    parameters = emptyList()
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement {
        return server.mpv.getCurrentStatusJson()
    }

    override fun formatResult(result: JsonElement): String {
        val string = StringBuilder("--- Server status ---\n")
        for ((key, value) in result.jsonObject.entries) {
            val key_text: String = key.replaceFirstChar { it.uppercaseChar() }.replace('_', ' ')
            string.append("$key_text: $value\n")
        }
        string.append("---------------------")

        return string.toString()
    }
}
