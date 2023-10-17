package spms.actions

import kotlinx.serialization.json.JsonElement
import spms.SpMpServer

class ServerActionPlay: ServerAction(
    identifier = "play",
    help = "Resume playback",
    parameters = emptyList()
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        server.mpv.play()
        return null
    }
}
