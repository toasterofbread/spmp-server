package mpv

interface MpvClient {
    fun release()

    var is_playing: Boolean
    val song_count: Int
    val current_song_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val repeat_mode: RepeatMode
    val volume: Double

    fun seekTo(position_ms: Long)
    fun seekToSong(index: Int)
    fun seekToNext(): Boolean
    fun seekToPrevious(): Boolean

    fun getSong(): String?
    fun getSong(index: Int): String?
    /** Returns the actual index of the added song **/
    fun addSong(song_id: String, index: Int): Int
    fun moveSong(from: Int, to: Int)
    fun removeSong(index: Int)
    fun clear()

    enum class RepeatMode {
        NONE,
        ONE,
        ALL
    }
}

fun Boolean.toInt(): Int =
    if (this) 1
    else 0
