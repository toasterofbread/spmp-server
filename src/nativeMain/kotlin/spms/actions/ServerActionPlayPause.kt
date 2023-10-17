package spms.actions

import kotlinx.serialization.json.JsonElement
import spms.SpMpServer

class ServerActionPlayPause: ServerAction(
    identifier = "playPause",
    help = "Toggle playback pause",
    parameters = emptyList()
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        server.mpv.playPause()
        return null
    }
}
