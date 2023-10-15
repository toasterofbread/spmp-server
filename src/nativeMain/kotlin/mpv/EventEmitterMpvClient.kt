package mpv

import kotlin.properties.Delegates

abstract class EventEmitterMpvClient: MpvClientImpl() {
    protected abstract fun onEvent(event: PlayerEvent)

    override var is_playing: Boolean
        get() = super.is_playing
        set(value) {
            super.is_playing = value
        }

    override fun seekTo(position_ms: Long) {
        super.seekTo(position_ms)
        onEvent(PlayerEvent.Seeked(position_ms))
    }
    override fun seekToSong(index: Int) {
        super.seekToSong(index)
        onEvent(PlayerEvent.SongTransition(index))
    }
    override fun seekToNext(): Boolean {
        if (super.seekToNext()) {
            onEvent(PlayerEvent.SongTransition(current_song_index))
            return true
        }
        return false
    }
    override fun seekToPrevious(): Boolean {
        if (super.seekToPrevious()) {
            onEvent(PlayerEvent.SongTransition(current_song_index))
            return true
        }
        return false
    }

    override fun addSong(song_id: String, index: Int): Int {
        val added_index: Int = super.addSong(song_id, index)
        onEvent(PlayerEvent.SongAdded(song_id, added_index))
        return added_index
    }
    override fun moveSong(from: Int, to: Int) {
        super.moveSong(from, to)
        onEvent(PlayerEvent.SongMoved(from, to))
    }
    override fun removeSong(index: Int) {
        super.removeSong(index)
        onEvent(PlayerEvent.SongRemoved(index))
    }
    override fun clear() {
        super.clear()
        onEvent(PlayerEvent.Cleared())
    }
}

abstract class PlayerEvent(vararg properties: Pair<String, Any>) {
    private val properties: Map<String, Any> = properties.toMap()

    private var event_id: Int by Delegates.notNull()
    private var client_id: Int? = null
    private var client_amount: Int by Delegates.notNull()

    fun init(event_id: Int, client_id: Int, client_amount: Int) {
        this.event_id = event_id
        this.client_id = client_id
        this.client_amount = client_amount
    }

    fun unassociateClient() {
        client_id = null
    }

    override fun toString(): String {
        return "${getType()}($properties)"
    }

    private fun getType(): String = this::class.simpleName!!

    class SongTransition(index: Int): PlayerEvent("index" to index)
    class PropertyChanged(key: String, value: Any): PlayerEvent("key" to key, "value" to value)
    class Seeked(position_ms: Long): PlayerEvent("position_ms" to position_ms)
    class SongAdded(song_id: String, index: Int): PlayerEvent("song_id" to song_id, "index" to index)
    class SongMoved(from: Int, to: Int): PlayerEvent("from" to from, "to" to to)
    class SongRemoved(index: Int): PlayerEvent("index" to index)
    class Cleared(): PlayerEvent()
}
