package dev.toastbits.spms.localisation.strings

import kotlin.time.Duration

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

    val indicator_button_open_client: String
    val indicator_button_stop_server: String

    fun connectingToSocket(address: String): String
    val releasing_socket: String

    val sending_handshake: String
    val handshake_reply_received: String

    val poll_polling_server_for_events: String

    fun errServerDidNotRespond(timeout: Duration): String
    fun errServerDidNotSendEvents(timeout: Duration): String
}
