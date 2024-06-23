package dev.toastbits.spms.localisation.strings

import dev.toastbits.spms.server.BUG_REPORT_URL
import kotlin.time.Duration

class CliLocalisationEn: CliLocalisation {
    override val bug_report_notice: String = "Report bugs at $BUG_REPORT_URL"

    override fun commandHelpRoot(mpv_enabled: Boolean): String =
        "Desktop server component for SpMp. " + (
            if (mpv_enabled) "Uses mpv for audio streaming and playback."
            else "This binary was built without support for mpv. Headless mode will always be enabled."
        )

    override val command_help_ctrl: String = "Command-line interface for interacting with other servers"

    override val option_group_help_controller: String = "Controller options"
    override val option_help_version: String = "Output version information and exit"
    override val option_help_silent: String = "Disable logging"
    override val option_help_language: String = "Language of outputted logs"
    override val option_help_halt: String = "Halt on start"
    override val option_help_server_ip: String = "The IP address of the server to connect to"
    override val option_help_server_port: String = "The port of the server to connect to"

    override val indicator_button_open_client: String = "Open client"
    override val indicator_button_stop_server: String = "Stop"

    override fun connectingToSocket(address: String): String =
        "Connecting to socket at $address..."
    override val releasing_socket: String = "Releasing socket..."

    override val sending_handshake: String = "Sending handshake..."
    override val handshake_reply_received: String = "Got handshake reply from server"

    override val poll_polling_server_for_events: String = "Polling server for events..."

    override fun errServerDidNotRespond(timeout: Duration): String =
        "Server did not respond within timeout ($timeout)"
    override fun errServerDidNotSendEvents(timeout: Duration): String =
        "Server did not send events within timeout ($timeout)"
}
