package spms.action.player

import cinterop.mpv.MpvClientImpl

sealed class PlayerAction(
    override val identifier: String,
    override val name: LocalisedMessageProvider,
    override val help: LocalisedMessageProvider,
    override val parameters: List<Parameter>,
    override val hidden: Boolean = false
): Action<MpvClientImpl> {
    override val type: Type = type.CLIENT

    fun executeOnRemoteServer(
        socket: ZmqSocket,
        parameter_values: List<JsonPrimitive>,
        reply_timeout_ms: Long?,
        context: Context,
        silent: Boolean = false
    ): SpMs.ActionReply? {
        socket.recvMultipart(reply_timeout_ms) ?: return null

        if (!silent) {
            println(context.loc.server_actions.sendingActionToServer(identifier))
        }

        socket.sendStringMultipart(
            listOf(SERVER_EXPECT_REPLY_CHAR + identifier, Json.encodeToString(parameter_values))
        )

        if (!silent) {
            println(context.loc.server_actions.actionSentAndWaitingForReply(identifier))
        }

        val timeout_end: Long? = reply_timeout_ms?.let { getTimeMillis() + it }
        do {
            val reply: List<String>? = socket.recvStringMultipart(timeout_end?.let { (it - getTimeMillis()).coerceAtLeast(ZMQ_NOBLOCK.toLong()) })
            if (!reply.isNullOrEmpty()) {
                if (!silent) {
                    println(context.loc.server_actions.receivedReplyFromServer(identifier))
                }

                // Hacky workaround, but if it works who cares?
                val joined: String = reply.joinToString().replace("\u0000, ", "")
                return Json.decodeFromString<List<SpMs.ActionReply>>(joined).first()
            }
            else if (!silent) {
                println(context.loc.server_actions.receivedEmptyReplyFromServer(identifier))
            }
        } while (timeout_end == null || getTimeMillis() < timeout_end)

        return null
    }

    companion object {
        private val ALL: List<PlayerAction> = listOf(
            
        )

        fun getAll(): List<PlayerAction> = ALL
        fun getByName(action_name: String): PlayerAction? = ALL.firstOrNull { it.identifier == action_name }

        fun executeByName(player: MpvClientImpl, client: SpMsClientID, action_name: String, parameter_values: List<JsonPrimitive>): JsonElement? {
            val action: ServerAction? = getByName(action_name)
            if (action == null) {
                throw NotImplementedError("Unknown action '$action_name'")
            }

            return action.execute(player, client, parameter_values)
        }
    }
}
