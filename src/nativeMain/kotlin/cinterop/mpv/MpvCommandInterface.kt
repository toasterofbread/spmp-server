package cinterop.mpv

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import spms.player.Player

@Serializable
data class ServerStatusData(
    val queue: List<String>,
    val state: Player.State,
    val is_playing: Boolean,
    val current_item_index: Int,
    val current_position_ms: Long,
    val duration_ms: Long,
    val repeat_mode: Player.RepeatMode,
    val volume: Double
)

fun Player.getCurrentStateJson(): JsonElement =
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
