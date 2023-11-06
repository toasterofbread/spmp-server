package spms.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.SpMpServer

class ServerActionSeekToNext: ServerAction(
    identifier = "seekToNext",
    help = "Seek to the next queue item",
    parameters = emptyList()
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        server.mpv.seekToNext()
        return null
    }
}
