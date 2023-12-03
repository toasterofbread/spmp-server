package spms.server

import kotlinx.serialization.Serializable

typealias SpMsClientID = Int

enum class SpMsClientType {
    HEADLESS, PLAYER
}

@Serializable
data class SpMsClientInfo(
    val name: String,
    val type: SpMsClientType
)

internal class SpMsClient(
    val id_bytes: ByteArray,
    val name: String,
    val type: SpMsClientType,
    var event_head: Int
) {
    val id: SpMsClientID = id_bytes.contentHashCode()
    var ready_to_play: Boolean = false

    override fun toString(): String =
        "Client(id=$id, name=$name, type=$type, event_head=$event_head)"
}
