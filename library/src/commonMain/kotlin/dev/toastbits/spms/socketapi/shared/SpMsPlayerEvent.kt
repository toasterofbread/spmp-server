package dev.toastbits.spms.socketapi.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class SpMsPlayerEvent(val type: Type, val properties: Map<String, JsonPrimitive> = emptyMap()) {
    enum class Type {
        ITEM_TRANSITION,
        PROPERTY_CHANGED,
        SEEKED,
        ITEM_ADDED,
        ITEM_REMOVED,
        ITEM_MOVED,
        CLEARED,
        READY_TO_PLAY,
        CANCEL_RADIO
    }

    var event_id: Int = -1
    var client_id: Int? = null
    var pending_client_amount: Int = -1

    fun init(event_id: Int, client_id: Int?, client_amount: Int) {
        this.event_id = event_id
        this.client_id = client_id
        this.pending_client_amount = client_amount
    }

    fun overrides(other: SpMsPlayerEvent): Boolean {
        if (this == other) {
            return true
        }

        if (type == Type.PROPERTY_CHANGED && properties["key"] == other.properties["key"]) {
            return true
        }

        return false
    }

    fun onConsumedByClient() {
        pending_client_amount--
    }

    fun shouldSendToInstigatingClient(): Boolean =
        when (type) {
            Type.CANCEL_RADIO -> false
            else -> true
        }

    override fun toString(): String {
        return "$type($properties)"
    }

    @Suppress("FunctionName")
    companion object {
        fun ItemTransition(index: Int) = SpMsPlayerEvent(Type.ITEM_TRANSITION, mapOf("index" to JsonPrimitive(index)))
        fun PropertyChanged(key: String, value: JsonPrimitive) = SpMsPlayerEvent(Type.PROPERTY_CHANGED, mapOf("key" to JsonPrimitive(key), "value" to value))
        fun SeekedToTime(position_ms: Long) = SpMsPlayerEvent(Type.SEEKED, mapOf("position_ms" to JsonPrimitive(position_ms)))
        fun ItemAdded(item_id: String, index: Int) = SpMsPlayerEvent(Type.ITEM_ADDED, mapOf("item_id" to JsonPrimitive(item_id), "index" to JsonPrimitive(index)))
        fun ItemRemoved(index: Int) = SpMsPlayerEvent(Type.ITEM_REMOVED, mapOf("index" to JsonPrimitive(index)))
        fun ItemMoved(from: Int, to: Int) = SpMsPlayerEvent(Type.ITEM_MOVED, mapOf("from" to JsonPrimitive(from), "to" to JsonPrimitive(to)))
        fun QueueCleared() = SpMsPlayerEvent(Type.CLEARED)
        fun ReadyToPlay() = SpMsPlayerEvent(Type.READY_TO_PLAY)
        fun CancelRadio() = SpMsPlayerEvent(Type.CANCEL_RADIO)
    }
}
