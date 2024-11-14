package dev.toastbits.spms.player

import dev.toastbits.spms.socketapi.shared.SpMsPlayerEvent
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class PlayerStreamInfo(
    val url: String,
    val duration: Long
)

interface Player {
    fun onShutdown()
    fun onEvent(event: SpMsPlayerEvent, clientless: Boolean = false) {}

    fun release()

    val state: SpMsPlayerState
    val is_playing: Boolean
    val item_count: Int
    val current_item_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val repeat_mode: SpMsPlayerRepeatMode

    fun canPlay(): Boolean

    fun play()
    fun pause()
    fun playPause()

    fun seekToTime(position_ms: Long)
    fun seekToItem(index: Int, position_ms: Long = 0)
    fun seekToNext(): Boolean
    fun seekToPrevious(repeat_threshold: Duration? = null): Boolean
    fun setRepeatMode(repeat_mode: SpMsPlayerRepeatMode)

    fun getItem(): String?
    fun getItem(index: Int): String?
    /** Returns the actual index of the added item **/
    fun addItem(item_id: String, index: Int): Int
    fun moveItem(from: Int, to: Int)
    fun removeItem(index: Int)
    fun clearQueue()

    fun setVolume(value: Double)
}

fun Player.shouldRepeatOnSeekToPrevious(repeat_threshold: Duration?): Boolean =
    repeat_threshold != null
    && (
        repeat_threshold == Duration.ZERO
        || current_position_ms.milliseconds >= repeat_threshold
    )
