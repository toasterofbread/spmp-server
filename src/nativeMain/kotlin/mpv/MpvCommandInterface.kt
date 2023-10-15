package mpv

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

fun MpvClient.executeActionByName(action_name: String, params: List<JsonPrimitive>) {
    when (action_name) {
        "play" -> play()
        "pause" -> pause()
        "playPause" -> playPause()

        "seekTo" -> seekTo(params.first().long)
        "seekToSong" -> seekToSong(params.first().int)
        "seekToNext" -> seekToNext()
        "seekToPrevious" -> seekToPrevious()

        "addSong" -> addSong(params[0].content, params.getOrNull(1)?.intOrNull ?: -1)
        "moveSong" -> moveSong(params[0].int, params[1].int)
        "removeSong" -> removeSong(params.first().int)
        "clear" -> clear()

        "setVolume" -> setVolume(params[0].float)

        else -> throw NotImplementedError("Unknown action '$action_name'")
    }
}

fun MpvClient.getSerialisedProperties(): String {
    val properties = buildJsonObject {
        val sc: Int = song_count
        putJsonArray("queue") {
            for (i in 0 until sc) {
                add(getSong(i))
            }
        }

        put("state", state.ordinal)
        put("is_playing", is_playing)
        put("current_song_index", current_song_index)
        put("current_position_ms", current_position_ms)
        put("duration_ms", duration_ms)
        put("repeat_mode", repeat_mode.ordinal)
        put("volume", volume)
    }
    return Json.encodeToString(properties)
}
