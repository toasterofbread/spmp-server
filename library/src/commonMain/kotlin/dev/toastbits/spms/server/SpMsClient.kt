package dev.toastbits.spms.server

import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.socketapi.shared.SpMsClientInfo
import dev.toastbits.spms.socketapi.shared.SpMsClientType
import dev.toastbits.spms.socketapi.shared.SpMsLanguage

internal class SpMsClient(
    val id_bytes: ByteArray,
    val info: SpMsClientInfo,
    var event_head: Int
) {
    val name: String get() = info.name
    val type: SpMsClientType get() = info.type
    val language: SpMsLanguage get() = info.language

    val id: SpMsClientID = id_bytes.contentHashCode()
    var ready_to_play: Boolean = false
    var failed_connection_attempts: Int = 0

    override fun toString(): String =
        "Client(id=$id, name=$name, type=$type, language=$language, event_head=$event_head)"
}
