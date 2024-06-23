package dev.toastbits.spms.localisation.strings

import dev.toastbits.spms.socketapi.shared.SpMsLanguage
import dev.toastbits.spms.localisation.SpMsLocalisation

open class LocalisationJa: SpMsLocalisation {
    override val language: SpMsLanguage = SpMsLanguage.JA

    override val server: ServerLocalisation = ServerLocalisationJa()
    override val server_actions: ServerActionLocalisation = ServerActionLocalisationJa()
    override val player_actions: PlayerActionLocalisation = PlayerActionLocalisationJa()

    override fun versionInfoText(api_version: Int): String =
        "SpMs API バージョン v$api_version"
}
