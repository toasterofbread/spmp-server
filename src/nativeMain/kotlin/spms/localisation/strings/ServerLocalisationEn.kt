package spms.localisation.strings

class ServerLocalisationEn: ServerLocalisation {
    override fun serverBoundToPort(server: String, port: Int): String =
        "Server $server bound to port $port"

    override val polling_started: String = "Polling started"
    override val polling_ended: String = "Polling ended"

    override val option_help_port: String = "The port on which to bind the server interface"
    override val option_help_gui: String = "Show mpv's graphical interface"
    override val option_help_mute: String = "Mute player on startup"
    override val option_help_headless: String = "Run without mpv"

    override val indicator_button_open_client: String = "Open client"
    override val indicator_button_stop_server: String = "Stop"
}
