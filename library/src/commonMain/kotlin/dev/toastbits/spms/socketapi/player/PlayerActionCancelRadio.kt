package dev.toastbits.spms.socketapi.player

import dev.toastbits.spms.mpv.MpvClientImpl
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PlayerActionCancelRadio: PlayerAction(
    identifier = "cancelRadio",
    name = { player_actions.cancel_radio_name },
    help = { player_actions.cancel_radio_help },
    parameters = emptyList()
) {
    override fun execute(player: MpvClientImpl, context: ActionContext): JsonElement? {
        player.cancelRadio()
        return null
    }
}
