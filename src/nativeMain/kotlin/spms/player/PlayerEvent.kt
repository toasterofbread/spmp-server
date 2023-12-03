package spms.player

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlin.properties.Delegates

@Serializable
data class PlayerEvent(val type: Type, val properties: Map<String, JsonPrimitive> = emptyMap()) {
    enum class Type {
        ITEM_TRANSITION,
        PROPERTY_CHANGED,
        SEEKED,
        ITEM_ADDED,
        ITEM_REMOVED,
        ITEM_MOVED,
        CLEARED,
        READY_TO_PLAY
    }

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
        fun ItemTransition(index: Int) = PlayerEvent(Type.ITEM_TRANSITION, mapOf("index" to JsonPrimitive(index)))
        fun PropertyChanged(key: String, value: JsonPrimitive) = PlayerEvent(Type.PROPERTY_CHANGED, mapOf("key" to JsonPrimitive(key), "value" to value))
        fun SeekedToTime(position_ms: Long) = PlayerEvent(Type.SEEKED, mapOf("position_ms" to JsonPrimitive(position_ms)))
        fun ItemAdded(item_id: String, index: Int) = PlayerEvent(Type.ITEM_ADDED, mapOf("item_id" to JsonPrimitive(item_id), "index" to JsonPrimitive(index)))
        fun ItemRemoved(index: Int) = PlayerEvent(Type.ITEM_REMOVED, mapOf("index" to JsonPrimitive(index)))
        fun ItemMoved(from: Int, to: Int) = PlayerEvent(Type.ITEM_MOVED, mapOf("from" to JsonPrimitive(from), "to" to JsonPrimitive(to)))
        fun QueueCleared() = PlayerEvent(Type.CLEARED)
        fun ReadyToPlay() = PlayerEvent(Type.READY_TO_PLAY)
    }
}
