package dev.toastbits.spms.localisation

import dev.toastbits.spms.localisation.strings.LocalisationEn
import dev.toastbits.spms.localisation.strings.LocalisationJa
import dev.toastbits.spms.localisation.strings.ServerActionLocalisation
import dev.toastbits.spms.localisation.strings.PlayerActionLocalisation
import dev.toastbits.spms.localisation.strings.ServerLocalisation
import dev.toastbits.spms.socketapi.shared.SpMsLanguage

typealias LocalisedMessageProvider = SpMsLocalisation.() -> String

interface SpMsLocalisation {
    val language: SpMsLanguage

    val server: ServerLocalisation
    val server_actions: ServerActionLocalisation
    val player_actions: PlayerActionLocalisation

    fun versionInfoText(api_version: Int): String

    companion object {
        fun get(language: SpMsLanguage): SpMsLocalisation =
            when (language) {
                SpMsLanguage.EN -> LocalisationEn()
                SpMsLanguage.JA -> LocalisationJa()
            }
    }
}
