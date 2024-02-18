package spms.socketapi.player

import cinterop.mpv.MpvClientImpl
import kotlinx.serialization.json.JsonElement
import spms.LocalisedMessageProvider
import spms.socketapi.Action

sealed class PlayerAction(
    override val identifier: String,
    override val name: LocalisedMessageProvider,
    override val help: LocalisedMessageProvider,
    override val parameters: List<Parameter>,
    override val hidden: Boolean = false
): Action() {
    override val type: Type = Type.PLAYER

    protected abstract fun execute(player: MpvClientImpl, context: ActionContext): JsonElement?

    fun execute(player: MpvClientImpl, parameter_values: List<JsonElement>): JsonElement? =
        execute(player, ActionContext(parameter_values))
    
    companion object {
        private val ALL: List<PlayerAction> = listOf(
            PlayerActionSetAuthInfo(),
            PlayerActionAddLocalFiles(),
            PlayerActionRemoveLocalFiles(),
            PlayerActionSetLocalFiles()
        )

        fun getAll(): List<PlayerAction> = ALL
        fun getByName(action_name: String): PlayerAction? = ALL.firstOrNull { it.identifier == action_name }

        fun executeByName(
            player: MpvClientImpl,
            action_name: String,
            parameter_values: List<JsonElement>
        ): JsonElement? {
            val action: PlayerAction? = getByName(action_name)
            if (action == null) {
                throw NotImplementedError("Unknown action '$action_name'")
            }

            return action.execute(player, parameter_values)
        }
    }
}
