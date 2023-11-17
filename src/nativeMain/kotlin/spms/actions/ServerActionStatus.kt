package spms.actions

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mpv.getCurrentStatusJson
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv
import spms.SpMpServer
import spms.SpMs

@OptIn(ExperimentalForeignApi::class)
fun getCacheDir(): Path =
    when (Platform.osFamily) {
        OsFamily.LINUX -> "/home/${getenv("USER")!!.toKString()}/.cache/".toPath()
        else -> throw NotImplementedError(Platform.osFamily.name)
    }.resolve(SpMs.applicationName)

class ServerActionStatus: ServerAction(
    identifier = "status",
    help = "Get detailed information about the server's current status",
    parameters = emptyList()
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement {
        return server.mpv.getCurrentStatusJson()
    }

    private val cache_files: MutableMap<String, JsonElement> = mutableMapOf()

    private fun loadCacheFiles() {
        cache_files.clear()
        val files = FileSystem.SYSTEM.listOrNull(getCacheDir()) ?: return
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

    override fun formatResult(result: JsonElement): String {

        val string: StringBuilder = StringBuilder("--- Server status ---\n")
        runBlocking {
            val http_client: HttpClient = HttpClient()

            val entries: Collection<Map.Entry<String, JsonElement>> = result.jsonObject.entries
            val formatted_values: Array<String?> = arrayOfNulls(entries.size)

            loadCacheFiles()
            entries.mapIndexed { index, entry ->
                launch(Dispatchers.Default) {
                    formatted_values[index] = http_client.formatKeyValue(entry.key, entry.value)
                }
            }.joinAll()
            saveCacheFiles()

            for ((index, entry) in entries.withIndex()) {
                val key_text: String = entry.key.replaceFirstChar { it.uppercaseChar() }.replace('_', ' ')
                string.append("$key_text: ${formatted_values[index]}\n")
            }

            http_client.close()
        }
        string.append("---------------------")

        return string.toString()
    }

    private suspend fun HttpClient.formatKeyValue(key: String, value: JsonElement): String {
        when (key) {
            "queue" -> {
                return buildString {
                    appendLine()

                    for ((index, item) in value.jsonArray.withIndex()) {
                        append(index)
                        append(": ")
                        appendLine(getVideoInfo(item.jsonPrimitive.content))
                    }
                }
            }
            else -> return value.toString()
        }
    }

    @Serializable
    private data class PipedStreamsResponse(val title: String)
    private val json: Json = Json { ignoreUnknownKeys = true }

    private suspend fun HttpClient.getVideoInfo(video_id: String): String {
        suspend fun tryRequest(): String {
            var parsed: PipedStreamsResponse? = cache_files["video_info"]?.jsonObject?.get(video_id)?.jsonObject?.let { Json.decodeFromJsonElement(it) }
            if (parsed == null) {
                val response: HttpResponse = get("https://pipedapi.kavin.rocks/streams/$video_id")
                parsed = json.decodeFromString(response.bodyAsText())!!

                val array: MutableMap<String, JsonElement> = cache_files.getOrPut("video_info") { JsonObject(emptyMap()) }.jsonObject.toMutableMap()
                array[video_id] = json.encodeToJsonElement(parsed)
                cache_files["video_info"] = JsonObject(array)
            }
            return "$video_id - ${parsed.title}"
        }

        for (i in 0 until 5) {
            try {
                return tryRequest()
            }
            catch (_: Throwable) {}
        }
        return video_id
    }
}
