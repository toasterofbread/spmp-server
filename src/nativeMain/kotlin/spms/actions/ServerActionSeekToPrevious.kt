package spms.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.SpMpServer

class ServerActionSeekToPrevious: ServerAction(
    identifier = "seekToPrevious",
    help = "Seek to the previous queue item",
    parameters = emptyList()
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        server.mpv.seekToPrevious()
        return null
    }
}
