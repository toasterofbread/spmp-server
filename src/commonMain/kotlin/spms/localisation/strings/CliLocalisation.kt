package spms.localisation.strings

interface CliLocalisation {
    val bug_report_notice: String

    fun commandHelpRoot(mpv_enabled: Boolean): String
    val command_help_ctrl: String

    val option_group_help_controller: String
    val option_help_version: String
    val option_help_silent: String
    val option_help_language: String
    val option_help_halt: String
    val option_help_server_ip: String
    val option_help_server_port: String

    val status_key_queue: String
    val status_key_state: String
    val status_key_is_playing: String
    val status_key_current_item_index: String
    val status_key_current_position_ms: String
    val status_key_duration_ms: String
    val status_key_repeat_mode: String

    fun connectingToSocket(address: String): String
    val releasing_socket: String

    val sending_handshake: String
    val handshake_reply_received: String

    val poll_polling_server_for_events: String

    fun errServerDidNotRespond(timeout_ms: Long): String
    fun errServerDidNotSendEvents(timeout_ms: Long): String
}
