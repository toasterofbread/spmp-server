package spms.socketapi.server

import cinterop.mpv.getCurrentStateJson
import com.github.ajalt.clikt.core.Context
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv
import spms.localisation.loc
import spms.server.SpMs
import spms.socketapi.shared.SpMsClientID
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState

@Suppress("OPT_IN_USAGE")
@OptIn(ExperimentalForeignApi::class)
fun getCacheDir(): Path =
    when (Platform.osFamily) {
        OsFamily.LINUX -> "/home/${getenv("USER")!!.toKString()}/.cache/".toPath().resolve(SpMs.application_name)
        OsFamily.WINDOWS -> "${getenv("USERPROFILE")!!.toKString()}/AppData/Local/${SpMs.application_name}/cache".toPath()
        else -> throw NotImplementedError(Platform.osFamily.name)
    }

class ServerActionGetStatus: ServerAction(
    identifier = "status",
    name = { server_actions.status_name },
    help = { server_actions.status_help },
    parameters = emptyList()
) {
    override fun execute(server: SpMs, client: SpMsClientID, context: ActionContext): JsonElement {
        return Json.encodeToJsonElement(server.player.getCurrentStateJson())
    }

    private val cache_files: MutableMap<String, JsonElement> = mutableMapOf()

    private fun loadCacheFiles() {
        cache_files.clear()
        val files: List<Path> = FileSystem.SYSTEM.listOrNull(getCacheDir()) ?: return
        for (file in files) {
            val content: String = FileSystem.SYSTEM.read(file) { readUtf8() }
            cache_files[file.name.removeSuffix(".json")] = Json.parseToJsonElement(content)
        }
    }

    private fun saveCacheFiles() {
        val cache_dir: Path = getCacheDir()
        FileSystem.SYSTEM.createDirectories(cache_dir)
        for ((file, data) in cache_files) {
            FileSystem.SYSTEM.write(cache_dir.resolve("$file.json")) {
                writeUtf8(Json.encodeToString(data))
            }
        }
    }

    override fun formatResult(result: JsonElement, context: Context): String {
        val string: StringBuilder = StringBuilder("--- ${context.loc.server_actions.status_output_start} ---\n")
        runBlocking {
            val entries: Collection<Map.Entry<String, JsonElement>> = result.jsonObject.entries
            val formatted_values: Array<String?> = arrayOfNulls(entries.size)

            loadCacheFiles()
            entries.mapIndexed { index, entry ->
                launch(Dispatchers.Default) {
                    formatted_values[index] = formatKeyValue(entry.key, entry.value)
                }
            }.joinAll()
            saveCacheFiles()

            for ((index, entry) in entries.withIndex()) {
                val key_text: String = with (context.loc.cli) {
                    when (entry.key) {
                        "queue" -> status_key_queue
                        "state" -> status_key_state
                        "is_playing" -> status_key_is_playing
                        "current_item_index" -> status_key_current_item_index
                        "current_position_ms" -> status_key_current_position_ms
                        "duration_ms" -> status_key_duration_ms
                        "repeat_mode" -> status_key_repeat_mode
                        "pause_after_songs" -> status_key_pause_after_songs
                        else -> entry.key.replaceFirstChar { it.uppercaseChar() }.replace('_', ' ')
                    }
                }

                string.append("$key_text: ${formatted_values[index]}\n")
            }
        }
        string.append("---------------------")

        return string.toString()
    }

    private fun formatKeyValue(key: String, value: JsonElement): String =
        when (key) {
            "queue" ->
                buildString {
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
            else -> value.toString()
        }
}
