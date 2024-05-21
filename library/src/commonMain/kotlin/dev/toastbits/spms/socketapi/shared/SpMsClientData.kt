package dev.toastbits.spms.socketapi.shared

import kotlinx.serialization.Serializable

typealias SpMsClientID = Int

enum class SpMsClientType {
    SPMP_PLAYER, SPMP_STANDALONE, PLAYER, COMMAND_LINE, SERVER, COMMAND_LINE_ACTION;

    fun receivesEvents(): Boolean =
        when (this) {
            SPMP_PLAYER, SPMP_STANDALONE, PLAYER, SERVER, COMMAND_LINE -> true
            else -> false
        }

    fun playsAudio(): Boolean =
        when (this) {
            SPMP_PLAYER, PLAYER -> true
            else -> false
        }
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
    val player_port: Int? = null,
    val actions: List<String>? = null
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
    val machine_id: String,
    val action_replies: List<SpMsActionReply>? = null
)

@Serializable
data class SpMsServerState(
    val queue: List<String>,
    val state: SpMsPlayerState,
    val is_playing: Boolean,
    val current_item_index: Int,
    val current_position_ms: Int,
    val duration_ms: Int,
    val repeat_mode: SpMsPlayerRepeatMode
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
