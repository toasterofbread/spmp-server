package mpv

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long

fun MpvClient.executeActionByName(action_name: String, params: List<JsonPrimitive>) {
    when (action_name) {
        "play" -> play()
        "pause" -> pause()
        "playPause" -> playPause()

        "seekTo" -> seekTo(params.first().long)
        "seekToSong" -> seekToSong(params.first().int)
        "seekToNext" -> seekToNext()
        "seekToPrevious" -> seekToPrevious()

        "addSong" -> addSong(params[0].content, params[1].int)
        "moveSong" -> moveSong(params[0].int, params[1].int)
        "removeSong" -> removeSong(params.first().int)
        "clear" -> clear()

        else -> throw NotImplementedError("Unknown action")
    }
}

fun MpvClient.getSerialisedProperties(): String {
    val properties = buildJsonArray {
        add(state.ordinal)
        add(is_playing)
        add(song_count)
        add(current_song_index)
        add(current_position_ms)
        add(duration_ms)
        add(repeat_mode.ordinal)
        add(volume)
    }
    return Json.encodeToString(properties)
}
