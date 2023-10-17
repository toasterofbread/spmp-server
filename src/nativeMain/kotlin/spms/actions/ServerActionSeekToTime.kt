package spms.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.long
import spms.SpMpServer

class ServerActionSeekToTime: ServerAction(
    identifier = "seekToTime",
    help = "Seek to the specified position within the playing item",
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "position_ms",
            "Position to seek to in milliseconds"
        )
    )
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        val position_ms: Long = context.getParameterValue("position_ms")!!.long
        server.mpv.seekToTime(position_ms)
        return null
    }
}
