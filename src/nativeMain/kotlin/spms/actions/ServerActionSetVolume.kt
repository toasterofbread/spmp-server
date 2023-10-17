package spms.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.float
import spms.SpMpServer

class ServerActionSetVolume: ServerAction(
    identifier = "setVolume",
    help = "Set playback volume",
    parameters = listOf(
        Parameter(
            Parameter.Type.Float,
            true,
            "volume",
            "Target volume level where 0 is silent and 1 is the maximum volume"
        )
    )
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        val volume: Float = context.getParameterValue("volume")!!.float
        server.mpv.setVolume(volume)
        return null
    }
}
