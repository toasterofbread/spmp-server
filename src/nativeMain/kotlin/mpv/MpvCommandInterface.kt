package mpv

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

fun MpvClient.getCurrentStatusJson(): JsonElement =
    buildJsonObject {
        val sc: Int = item_count
        putJsonArray("queue") {
            for (i in 0 until sc) {
                add(getItem(i))
            }
        }

        put("state", state.ordinal)
        put("is_playing", is_playing)
        put("current_item_index", current_item_index)
        put("current_position_ms", current_position_ms)
        put("duration_ms", duration_ms)
        put("repeat_mode", repeat_mode.ordinal)
        put("volume", volume)
    }
