package dev.toastbits.spms.localisation.strings

class ServerLocalisationEn: ServerLocalisation {
    override fun serverBoundToPort(server: String, port: Int): String =
        "Server $server bound to port $port"

    override val polling_started: String = "Polling started"
    override val polling_ended: String = "Polling ended"

    override val option_help_port: String = "The port on which to bind the server interface"
    override val option_help_gui: String = "Show mpv's graphical interface"
    override val option_help_mute: String = "Mute player on startup"
    override val option_help_headless: String = "Run without mpv"
    override val option_no_media_session: String = "Don't broadcast media sessiont to system"
    override val option_help_icon: String = "Path to icon"
}
