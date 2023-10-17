package spms.actions

import kotlinx.serialization.json.JsonElement
import spms.SpMpServer

class ServerActionPause: ServerAction(
    identifier = "pause",
    help = "Pause playback",
    parameters = emptyList()
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        server.mpv.pause()
        return null
    }
}
