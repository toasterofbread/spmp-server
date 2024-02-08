package spms.server

import spms.localisation.Language
import spms.socketapi.shared.SpMsClientID
import spms.socketapi.shared.SpMsClientInfo
import spms.socketapi.shared.SpMsClientType

internal class SpMsClient(
    val id_bytes: ByteArray,
    val info: SpMsClientInfo,
    var event_head: Int
) {
    val name: String get() = info.name
    val type: SpMsClientType get() = info.type
    val language: Language get() = info.language

    val id: SpMsClientID = id_bytes.contentHashCode()
    var ready_to_play: Boolean = false

    override fun toString(): String =
        "Client(id=$id, name=$name, type=$type, language=$language, event_head=$event_head)"
}
