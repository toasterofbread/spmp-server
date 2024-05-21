package dev.toastbits.spms.localisation.strings

interface ServerLocalisation {
    fun serverBoundToPort(server: String, port: Int): String

    val polling_started: String
    val polling_ended: String

    val option_help_port: String
    val option_help_gui: String
    val option_help_mute: String
    val option_help_headless: String
    val option_no_media_session: String
    val option_help_icon: String
}
