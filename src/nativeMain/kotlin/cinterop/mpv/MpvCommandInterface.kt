package cinterop.mpv

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

@Serializable
data class ServerStatusData(
    val queue: List<String>,
    val state: MpvClient.State,
    val is_playing: Boolean,
    val current_item_index: Int,
    val current_position_ms: Long,
    val duration_ms: Long,
    val repeat_mode: MpvClient.RepeatMode,
    val volume: Double
)

fun MpvClient.getCurrentStatusJson(): JsonElement =
    Json.encodeToJsonElement(
        ServerStatusData(
            (0 until item_count).map { getItem(it) ?: "" },
            state,
            is_playing,
            current_item_index,
            current_position_ms,
            duration_ms,
            repeat_mode,
            volume
        )
    )
