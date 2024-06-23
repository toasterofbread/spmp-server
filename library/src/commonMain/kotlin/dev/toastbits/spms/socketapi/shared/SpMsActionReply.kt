package dev.toastbits.spms.socketapi.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SpMsActionReply(
    val success: Boolean,
    val error: String? = null,
    val error_cause: String? = null,
    val result: JsonElement? = null
)
    