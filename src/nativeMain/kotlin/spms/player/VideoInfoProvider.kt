package spms.player

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import libcurl.*

@Serializable
private data class YoutubeVideoFormat(
    val itag: Int?,
    val mimeType: String,
    val bitrate: Int,
    val url: String?
) {
    val audio_only: Boolean get() = mimeType.startsWith("audio")

    override fun toString(): String {
        return "YoutubeVideoFormat(itag=$itag, mimeType=$mimeType, bitrate=$bitrate)"
    }
}

@Serializable
private data class YoutubeFormatsResponse(
    val playabilityStatus: PlayabilityStatus,
    val streamingData: StreamingData
) {
    @Serializable
    data class StreamingData(val formats: List<YoutubeVideoFormat>, val adaptiveFormats: List<YoutubeVideoFormat>)
    @Serializable
    data class PlayabilityStatus(val status: String)
}


@OptIn(ExperimentalForeignApi::class)
object VideoInfoProvider {
    private val curl = curl_easy_init()
    private val json: Json = Json { ignoreUnknownKeys = true }

    init {
        curl_easy_setopt(curl, CURLOPT_URL, "https://music.youtube.com/youtubei/v1/player?key=AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI")
        curl_easy_setopt(curl, CURLOPT_POST, 1)
        curl_easy_setopt(curl, CURLOPT_NOPROGRESS, 1)
    }

    private val default_headers: Map<String, String> = mapOf(
        "accept" to "*/*",
        "content-type" to "application/json",
        "x-youtube-client-name" to "67",
        "x-youtube-client-version" to "1.20221019.01.00",
        "x-goog-authuser" to "0",
        "x-origin" to "https://music.youtube.com",
        "origin" to "https://music.youtube.com",
        "user-agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"
    )
    private val body_template: String =
        """
        {
            "context": {
                "client":{
                    "hl": "en",
                    "platform": "MOBILE",
                    "clientName": "ANDROID_MUSIC",
                    "clientVersion": "5.28.1",
                    "userAgent": "com.google.android.apps.youtube.music/5.28.1 (Linux; U; Android 11) gzip",
                    "acceptHeader": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                },
                "user": {},
            },
            "videoId": ""
        }
        """.trimIndent()
    private val body_template_video_id_index: Int = body_template.indexOf("\"videoId\": \"\"") + 13

    private fun getPostBody(video_id: String): String =
        body_template.replaceRange(body_template_video_id_index, body_template_video_id_index, video_id)

    // Convert the following Kotlin function to C, using libcurl
    suspend fun getVideoStreamUrl(video_id: String, account_headers: Map<String, String>? = null): String = withContext(Dispatchers.IO) { memScoped {
        var headers: CValuesRef<curl_slist>? = null
        for (header in default_headers + account_headers.orEmpty()) {
            headers = curl_slist_append(headers, "${header.key}: ${header.value}")
        }
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers)

        val post_body: String = getPostBody(video_id)
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, post_body)
        curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE_LARGE, post_body.length)

        val result_builder: StringBuilder = StringBuilder()
        val result_builder_ref: StableRef<StringBuilder> = StableRef.create(result_builder)
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, result_builder_ref.asCPointer())

        val result: CURLcode =
            try {
                curl_easy_perform(curl)
            }
            finally {
                result_builder_ref.dispose()
            }
        if (result != CURLE_OK) {
            throw RuntimeException("getVideoStreamUrl for $video_id with ${account_headers?.size ?: 0} account headers failed ($result)")
        }

        val body: String = result_builder.toString()
        try {
            val formats: YoutubeFormatsResponse = json.decodeFromString(body)!!
            val best_format: YoutubeVideoFormat = formats.streamingData.adaptiveFormats.filter { it.audio_only }.maxBy { it.bitrate }

            return@withContext best_format.url!!
        }
        catch (e: Throwable) {
            throw RuntimeException(body, e)
        }
    }}
}
