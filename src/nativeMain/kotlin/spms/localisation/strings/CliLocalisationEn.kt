package spms.localisation.strings

import spms.BUG_REPORT_URL

class CliLocalisationEn: CliLocalisation {
    override val bug_report_notice: String = "Report bugs at $BUG_REPORT_URL"

    override val command_help_root: String = "Desktop server component for SpMp. Uses MPV for audio streaming and playback."
    override val command_help_ctrl: String = "Command-line interface for interacting with other servers"

    override val option_group_help_controller: String = "Controller options"
    override val option_help_silent: String = "Disable logging"
    override val option_help_language: String = "Language of outputted logs"
    override val option_help_halt: String = "Halt on start"
    override val option_help_server_address: String = "The IP address of the server to connect to"
    override val option_help_server_port: String = "The port of the server to connect to"

    override val status_key_queue: String = "Queue"
    override val status_key_state: String = "Playback state"
    override val status_key_is_playing: String = "Is playing"
    override val status_key_current_item_index: String = "Current item index"
    override val status_key_current_position_ms: String = "Item position (ms)"
    override val status_key_duration_ms: String = "Item duration (ms)"
    override val status_key_repeat_mode: String = "Repeat mode"
    override val status_key_volume: String = "Volume"

    override fun connectingToSocket(address: String): String =
        "Connecting to socket at $address..."
    override val releasing_socket: String = "Releasing socket..."

    override val sending_handshake: String = "Sending handshake..."
    override val handshake_reply_received: String = "Got handshake reply from server"

    override val poll_polling_server_for_events: String = "Polling server for events..."

    override fun errServerDidNotRespond(timeout_ms: Long): String =
        "Server did not respond within timeout (${timeout_ms}ms)"
    override fun errServerDidNotSendEvents(timeout_ms: Long): String =
        "Server did not send events within timeout (${timeout_ms}ms)"
}
