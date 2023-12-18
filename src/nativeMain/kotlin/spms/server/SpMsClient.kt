package spms.server

import kotlinx.serialization.Serializable
import spms.localisation.Language
import spms.player.Player

typealias SpMsClientID = Int

enum class SpMsClientType {
    SPMP_PLAYER, SPMP_STANDALONE, PLAYER, COMMAND_LINE
}

@Serializable
data class SpMsClientHandshake(
    val name: String,
    val type: SpMsClientType,
    val language: String? = null
) {
    fun getLanguage(): Language =
        Language.fromCode(language) ?: Language.default
}

@Serializable
data class SpMsServerHandshake(
    val name: String,
    val device_name: String,
    val spms_commit_hash: String,
    val server_state: SpMsServerState
)

@Serializable
data class SpMsServerState(
    val queue: List<String>,
    val state: Player.State,
    val is_playing: Boolean,
    val current_item_index: Int,
    val current_position_ms: Int,
    val duration_ms: Int,
    val repeat_mode: Player.RepeatMode,
    val volume: Float
)

@Serializable
data class SpMsClientInfo(
    val name: String,
    val type: SpMsClientType,
    val language: Language,
    val is_caller: Boolean = false
)

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
