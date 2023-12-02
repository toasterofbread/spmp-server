package cinterop.mpv

interface MpvClient {
    fun release()

    val state: State
    val is_playing: Boolean
    val item_count: Int
    val current_item_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val repeat_mode: RepeatMode
    val volume: Double

    fun play()
    fun pause()
    fun playPause()

    fun seekToTime(position_ms: Long)
    fun seekToItem(index: Int)
    fun seekToNext(): Boolean
    fun seekToPrevious(): Boolean

    fun getItem(): String?
    fun getItem(index: Int): String?
    /** Returns the actual index of the added item **/
    fun addItem(item_id: String, index: Int): Int
    fun moveItem(from: Int, to: Int)
    fun removeItem(index: Int)
    fun clearQueue()

    fun setVolume(value: Float)

    enum class State {
        IDLE,
        BUFFERING,
        READY,
        ENDED
    }

    enum class RepeatMode {
        NONE,
        ONE,
        ALL
    }
}

fun Boolean.toInt(): Int =
    if (this) 1
    else 0
