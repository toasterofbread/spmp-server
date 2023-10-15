package mpv

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlin.properties.Delegates

abstract class EventEmitterMpvClient: MpvClientImpl() {
    protected abstract fun onEvent(event: PlayerEvent)

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

@Serializable
data class PlayerEvent(private val type: String, private val properties: Map<String, JsonPrimitive> = emptyMap()) {
    var event_id: Int by Delegates.notNull()
        private set
    var client_id: Int? = null
        private set
    var pending_client_amount: Int by Delegates.notNull()
        private set

    fun init(event_id: Int, client_id: Int?, client_amount: Int) {
        this.event_id = event_id
        this.client_id = client_id
        this.pending_client_amount = client_amount
    }

    fun unassociateClient() {
        client_id = null
    }

    fun onConsumedByClient() {
        pending_client_amount--
    }

    override fun toString(): String {
        return "$type($properties)"
    }

    companion object {
        fun SongTransition(index: Int) = PlayerEvent("SongTransition", mapOf("index" to JsonPrimitive(index)))
        fun Seeked(position_ms: Long) = PlayerEvent("Seeked", mapOf("position_ms" to JsonPrimitive(position_ms)))
        fun SongRemoved(index: Int) = PlayerEvent("SongRemoved", mapOf("index" to JsonPrimitive(index)))
        fun PropertyChanged(key: String, value: JsonPrimitive) = PlayerEvent("PropertyChanged", mapOf("key" to JsonPrimitive(key), "value" to value))
        fun SongAdded(song_id: String, index: Int) = PlayerEvent("SongAdded", mapOf("song_id" to JsonPrimitive(song_id), "index" to JsonPrimitive(index)))
        fun SongMoved(from: Int, to: Int) = PlayerEvent("SongMoved", mapOf("from" to JsonPrimitive(from), "to" to JsonPrimitive(to)))
        fun Cleared() = PlayerEvent("Cleared")
    }
}
