package dev.toastbits.spms.socketapi

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import dev.toastbits.spms.socketapi.shared.SPMS_EXPECT_REPLY_CHAR
import dev.toastbits.spms.socketapi.shared.SpMsActionReply

fun parseSocketMessage(
    parts: List<String>,
    onError: (Throwable) -> Unit = { it.printStackTrace() },
    executeAction: (name: String, params: List<JsonElement>) -> JsonElement?
): List<SpMsActionReply> {
    if (parts.size < 2) {
        return emptyList()
    }

    require(parts.size % 2 == 0) {
        "Message size (${parts.size}) is invalid"
    }

    val reply: MutableList<SpMsActionReply> = mutableListOf()

    var i: Int = 0
    while (i < parts.size) {
        val first: String = parts[i++]
        val expects_reply: Boolean = first.first() == SPMS_EXPECT_REPLY_CHAR

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
        }
        catch (e: Throwable) {
            val message: String = "Executing action $action_name(${action_params.map { it.toString() }}) failed"

            if (expects_reply) {
                reply.add(
                    SpMsActionReply(
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
                SpMsActionReply(
                    success = true,
                    result = result
                )
            )
        }
    }

    return reply
}
