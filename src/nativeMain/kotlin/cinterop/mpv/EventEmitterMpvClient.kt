package cinterop.mpv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import libmpv.MPV_EVENT_END_FILE
import libmpv.MPV_EVENT_PLAYBACK_RESTART
import libmpv.MPV_EVENT_PROPERTY_CHANGE
import libmpv.MPV_EVENT_SHUTDOWN
import libmpv.MPV_EVENT_START_FILE
import libmpv.mpv_event_id
import kotlin.properties.Delegates

abstract class EventEmitterMpvClient(headless: Boolean = false): MpvClientImpl(headless) {
    protected abstract fun onEvent(event: PlayerEvent, clientless: Boolean = false)
    protected abstract fun onShutdown()

    private val coroutine_scope = CoroutineScope(Dispatchers.IO)

    init {
        coroutine_scope.launch {
            while (true) {
                val event: mpv_event_id? = waitForEvent()
                when (event) {
                    MPV_EVENT_START_FILE -> {
                        onEvent(PlayerEvent.ItemTransition(current_item_index), clientless = true)
                    }
                    MPV_EVENT_PLAYBACK_RESTART -> {
                        onEvent(PlayerEvent.PropertyChanged("state", JsonPrimitive(state.ordinal)), clientless = true)
                        onEvent(PlayerEvent.PropertyChanged("duration_ms", JsonPrimitive(duration_ms)), clientless = true)
                        onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(is_playing)), clientless = true)
                    }
                    MPV_EVENT_END_FILE -> {
                        onEvent(PlayerEvent.PropertyChanged("state", JsonPrimitive(state.ordinal)), clientless = true)
                        onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(is_playing)), clientless = true)
                    }
                    MPV_EVENT_SHUTDOWN -> {
                        onShutdown()
                    }
                }
            }
        }
    }

    override fun release() {
        coroutine_scope.cancel()
        super.release()
    }

    override fun play() {
        super.play()
        onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(is_playing)))
    }

    override fun pause() {
        super.pause()
        onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(is_playing)))
    }

    override fun seekToTime(position_ms: Long) {
        super.seekToTime(position_ms)
        onEvent(PlayerEvent.SeekedToTime(position_ms))
    }
    override fun seekToItem(index: Int) {
        super.seekToItem(index)
        onEvent(PlayerEvent.ItemTransition(index))
    }
    override fun seekToNext(): Boolean {
        if (super.seekToNext()) {
            onEvent(PlayerEvent.ItemTransition(current_item_index))
            return true
        }
        return false
    }
    override fun seekToPrevious(): Boolean {
        if (super.seekToPrevious()) {
            onEvent(PlayerEvent.ItemTransition(current_item_index))
            return true
        }
        return false
    }

    override fun addItem(item_id: String, index: Int): Int {
        val added_index: Int = super.addItem(item_id, index)
        onEvent(PlayerEvent.ItemAdded(item_id, added_index))
        return added_index
    }
    override fun moveItem(from: Int, to: Int) {
        super.moveItem(from, to)
        onEvent(PlayerEvent.ItemMoved(from, to))
    }
    override fun removeItem(index: Int) {
        super.removeItem(index)
        onEvent(PlayerEvent.ItemRemoved(index))
    }
    override fun clearQueue() {
        super.clearQueue()
        onEvent(PlayerEvent.QueueCleared())
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
        require(client_amount > 0)

        this.event_id = event_id
        this.client_id = client_id
        this.pending_client_amount = client_amount
    }

    fun onConsumedByClient() {
        pending_client_amount--
    }

    override fun toString(): String {
        return "$type($properties)"
    }

    @Suppress("FunctionName")
    companion object {
        fun ItemTransition(index: Int) = PlayerEvent("ItemTransition", mapOf("index" to JsonPrimitive(index)))
        fun SeekedToTime(position_ms: Long) = PlayerEvent("Seeked", mapOf("position_ms" to JsonPrimitive(position_ms)))
        fun ItemRemoved(index: Int) = PlayerEvent("ItemRemoved", mapOf("index" to JsonPrimitive(index)))
        fun PropertyChanged(key: String, value: JsonPrimitive) = PlayerEvent("PropertyChanged", mapOf("key" to JsonPrimitive(key), "value" to value))
        fun ItemAdded(item_id: String, index: Int) = PlayerEvent("ItemAdded", mapOf("item_id" to JsonPrimitive(item_id), "index" to JsonPrimitive(index)))
        fun ItemMoved(from: Int, to: Int) = PlayerEvent("ItemMoved", mapOf("from" to JsonPrimitive(from), "to" to JsonPrimitive(to)))
        fun QueueCleared() = PlayerEvent("Cleared")
    }
}
