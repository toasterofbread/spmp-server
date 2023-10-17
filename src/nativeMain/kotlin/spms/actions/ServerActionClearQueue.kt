package spms.actions

import kotlinx.serialization.json.JsonElement
import spms.SpMpServer

class ServerActionClearQueue: ServerAction(
    identifier = "clearQueue",
    help = "Clear the current player queue",
    parameters = emptyList()
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        server.mpv.clearQueue()
        return null
    }
}
