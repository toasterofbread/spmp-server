package spms.socketapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ActionReply(
    val success: Boolean,
    val error: String? = null,
    val error_cause: String? = null,
    val result: JsonElement? = null
)
    