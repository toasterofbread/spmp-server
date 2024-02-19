package spms.socketapi.shared

import kotlinx.serialization.Serializable

typealias SpMsClientID = Int

enum class SpMsClientType {
    SPMP_PLAYER, SPMP_STANDALONE, PLAYER, COMMAND_LINE, SERVER
}

enum class SpMsPlayerState {
    IDLE,
    BUFFERING,
    READY,
    ENDED
}

enum class SpMsPlayerRepeatMode {
    NONE,
    ONE,
    ALL
}

enum class SpMsLanguage {
    EN, JA;
    
    companion object {
        val default: SpMsLanguage = EN
        
        fun fromCode(code: String?): SpMsLanguage? =
            when (code?.split('_', limit = 2)?.firstOrNull()?.uppercase()) {
                "EN" -> EN
                "JA" -> JA
                else -> null
            }
    }
}

@Serializable
data class SpMsClientHandshake(
    val name: String,
    val type: SpMsClientType,
    val machine_id: String,
    val language: String? = null,
    val player_port: Int? = null
) {
    fun getLanguage(): SpMsLanguage =
        SpMsLanguage.fromCode(language) ?: SpMsLanguage.default
}

@Serializable
data class SpMsServerHandshake(
    val name: String,
    val device_name: String,
    val spms_api_version: Int,
    val server_state: SpMsServerState,
    val machine_id: String
)

@Serializable
data class SpMsServerState(
    val queue: List<String>,
    val state: SpMsPlayerState,
    val is_playing: Boolean,
    val current_item_index: Int,
    val current_position_ms: Int,
    val duration_ms: Int,
    val repeat_mode: SpMsPlayerRepeatMode,
    val volume: Float
)

@Serializable
data class SpMsClientInfo(
    val name: String,
    val type: SpMsClientType,
    val language: SpMsLanguage,
    val machine_id: String,
    val is_caller: Boolean = false,
    val player_port: Int? = null
)
