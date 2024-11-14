package dev.toastbits.spms.socketapi.server

import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.localisation.LocalisedMessageProvider
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.socketapi.Action

sealed class ServerAction(
    override val identifier: String,
    override val name: LocalisedMessageProvider,
    override val help: LocalisedMessageProvider,
    override val parameters: List<Parameter>,
    override val hidden: Boolean = false
): Action() {
    override val type: Type = Type.SERVER

    protected abstract fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement?

    fun execute(server: SpMs, client: SpMsClientID, parameter_values: List<JsonElement>): JsonElement? =
        execute(server, client, ActionContext(parameter_values))

    companion object {
        private val ALL: List<ServerAction> = listOf(
            ServerActionGetStatus,
            ServerActionGetClients,
            ServerActionGetProperty,

            ServerActionPause,
            ServerActionPlay,
            ServerActionPlayPause,

            ServerActionSeekToItem,
            ServerActionSeekToNext,
            ServerActionSeekToPrevious,
            ServerActionSeekToTime,
            ServerActionSetRepeatMode,

            ServerActionAddItem,
            ServerActionMoveItem,
            ServerActionRemoveItem,
            ServerActionClearQueue,

            ServerActionReadyToPlay
        )

        fun getAll(): List<ServerAction> = ALL
        fun getByName(action_name: String): ServerAction? = ALL.firstOrNull { it.identifier == action_name }

        fun executeByName(server: SpMs, client: SpMsClientID, action_name: String, parameter_values: List<JsonElement>): JsonElement? {
            val action: ServerAction? = getByName(action_name)
            if (action == null) {
                throw NotImplementedError("Unknown action '$action_name'")
            }

            return action.execute(server, client, parameter_values)
        }
    }
}
