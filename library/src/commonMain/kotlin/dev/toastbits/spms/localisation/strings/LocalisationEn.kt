package dev.toastbits.spms.localisation.strings

import dev.toastbits.spms.socketapi.shared.SpMsLanguage
import dev.toastbits.spms.localisation.SpMsLocalisation

open class LocalisationEn: SpMsLocalisation {
    override val language: SpMsLanguage = SpMsLanguage.EN

    override val server: ServerLocalisation = ServerLocalisationEn()
    override val server_actions: ServerActionLocalisation = ServerActionLocalisationEn()
    override val player_actions: PlayerActionLocalisation = PlayerActionLocalisationEn()

    override fun versionInfoText(api_version: Int): String =
        "SpMs API v$api_version"
}
