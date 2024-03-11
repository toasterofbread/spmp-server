package spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientID
import spms.socketapi.shared.SpMsPlayerRepeatMode

class ServerActionSetPauseAfterSongs: ServerAction(
    identifier = "setPauseAfterSongs",
    name = { server_actions.set_pause_after_songs_name },
    help = { server_actions.set_pause_after_songs_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "song_count",
            { server_actions.set_pause_after_songs_param_song_count }
        )
    )
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement? {
        val song_count: Int? = context.getParameterValue("song_count")!!.jsonPrimitive.int
        server.player.setPauseAfterSongs(song_count)
        return null
    }
}
