package dev.toastbits.spms.mpv

import dev.toastbits.spms.player.Player
import dev.toastbits.spms.socketapi.shared.SpMsServerState

fun Player.getCurrentStateJson(): SpMsServerState =
    SpMsServerState(
        (0 until item_count).map { getItem(it) ?: "" },
        state,
        is_playing,
        current_item_index,
        current_position_ms.toInt(),
        duration_ms.toInt(),
        repeat_mode
    )
