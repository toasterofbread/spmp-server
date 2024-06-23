package dev.toastbits.spms.socketapi.server

import dev.toastbits.spms.mpv.getCurrentStateJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsClientID
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import dev.toastbits.spms.getCacheDir
import dev.toastbits.spms.localisation.SpMsLocalisation
import dev.toastbits.spms.PLATFORM
import kotlin.time.Duration

class ServerActionGetStatus: ServerAction(
    identifier = "status",
    name = { server_actions.status_name },
    help = { server_actions.status_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement {
        return Json.encodeToJsonElement(server.player.getCurrentStateJson())
    }

    override fun formatResult(result: JsonElement, localisation: SpMsLocalisation): String {
        val string: StringBuilder = StringBuilder("--- ${localisation.server_actions.status_output_start} ---\n")
        runBlocking {
            val entries: Map<String, String> =
                result.jsonObject.entries.associate { entry ->
                    val key: String =
                        with (localisation.server_actions) {
                            when (entry.key) {
                                "queue" -> status_key_queue
                                "state" -> status_key_state
                                "is_playing" -> status_key_is_playing
                                "current_item_index" -> status_key_current_item_index
                                "current_position_ms" -> status_key_current_position
                                "duration_ms" -> status_key_duration
                                "repeat_mode" -> status_key_repeat_mode
                                else -> entry.key.replaceFirstChar { it.uppercaseChar() }.replace('_', ' ')
                            }
                        }
                    val value: String = formatKeyValue(entry.key, entry.value)

                    key to value
                }

            for ((key, value) in entries) {
                string.append("$key: $value\n")
            }
        }
        string.append("---------------------")

        return string.toString()
    }

    private fun formatKeyValue(key: String, value: JsonElement): String {
        return when (key) {
            "queue" ->
                buildString {
                    println("QUEUE $value ${value.jsonArray}")
                    if (value.jsonArray.isNotEmpty()) {
                        appendLine()

                        for ((index, item) in value.jsonArray.withIndex()) {
                            append(index)
                            append(": ")
                            appendLine(item.jsonPrimitive.content)
                        }
                    }
                }
            "state" -> value.jsonPrimitive.intOrNull?.let { SpMsPlayerState.values().getOrNull(it)?.name } ?: value.jsonPrimitive.toString()
            "repeat_mode" -> value.jsonPrimitive.intOrNull?.let { SpMsPlayerRepeatMode.values().getOrNull(it)?.name } ?: value.jsonPrimitive.toString()
            "current_position_ms", "duration_ms" -> with (Duration) {
                value.jsonPrimitive.longOrNull?.let { it.milliseconds }.toString()
            }
            else -> value.toString()
        }
    }
}
