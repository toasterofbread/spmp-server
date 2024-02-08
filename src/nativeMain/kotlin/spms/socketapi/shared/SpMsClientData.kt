package spms.socketapi.shared

import kotlinx.serialization.Serializable
import spms.localisation.Language
import spms.player.Player

typealias SpMsClientID = Int

enum class SpMsClientType {
    SPMP_PLAYER, SPMP_STANDALONE, PLAYER, COMMAND_LINE, SERVER
}

@Serializable
data class SpMsClientHandshake(
    val name: String,
    val type: SpMsClientType,
    val machine_id: String,
    val language: String? = null,
    val player_port: Int? = null
) {
    fun getLanguage(): Language =
        Language.fromCode(language) ?: Language.default
}

@Serializable
data class SpMsServerHandshake(
    val name: String,
    val device_name: String,
    val spms_commit_hash: String,
    val server_state: SpMsServerState,
    val machine_id: String
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
    val machine_id: String,
    val is_caller: Boolean = false,
    val player_port: Int? = null
)
