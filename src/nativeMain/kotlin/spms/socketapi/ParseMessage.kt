package spms.socketapi

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

const val SERVER_EXPECT_REPLY_CHAR: Char = '!'

fun parseSocketMessage(
    parts: List<String>,
    onError: (Throwable) -> Unit = { it.printStackTrace() },
    executeAction: (name: String, params: List<JsonElement>) -> JsonElement?
): List<ActionReply> {
    if (parts.size < 2) {
        return emptyList()
    }

    require(parts.size % 2 == 0) {
        "Message size (${parts.size}) is invalid"
    }

    val reply: MutableList<ActionReply> = mutableListOf()

    var i: Int = 0
    while (i < parts.size) {
        val first: String = parts[i++]
        val expects_reply: Boolean = first.first() == SERVER_EXPECT_REPLY_CHAR

        val action_name: String = if (expects_reply) first.substring(1) else first
        val action_params: List<JsonElement> =
            try {
                Json.decodeFromString(parts[i++])
            }
            catch (e: Throwable) {
                throw RuntimeException("Parse data: ${parts[i - 1]}", e)
            }

        val result: JsonElement?
        try {
            result = executeAction(action_name, action_params)
//            result = Action.executeByName(this@SpMs, client.id, action_name, action_params)
        }
        catch (e: Throwable) {
            val message: String = "Executing action $action_name(${action_params.map { it.toString() }}) failed"

            if (expects_reply) {
                reply.add(
                    ActionReply(
                        success = false,
                        error = message,
                        error_cause = e.message
                    )
                )
            }

            onError(RuntimeException(message, e))
            continue
        }

        if (expects_reply) {
            reply.add(
                ActionReply(
                    success = true,
                    result = result
                )
            )
        }
    }
    
    return reply
}
