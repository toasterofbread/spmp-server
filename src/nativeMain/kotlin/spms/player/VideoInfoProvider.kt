package spms.player

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val MAX_RETRIES: Int = 3

@Serializable
private data class PipedStreamsResponse(val audioStreams: List<AudioStream>) {
    @Serializable
    data class AudioStream(
        val url: String,
        val bitrate: Int
    )
}

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


object VideoInfoProvider {
    private val http_client: HttpClient = HttpClient()
    private val json: Json = Json { ignoreUnknownKeys = true }

    suspend fun getVideoStreamUrl(video_id: String, account_headers: List<Pair<String, String>>? = null): String {
        val response: HttpResponse = http_client.post {
            url("https://music.youtube.com/youtubei/v1/player?key=AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI")

            headers {
                set("accept", "*/*")
                set("content-type", "application/json")
                set("x-youtube-client-name", "67")
                set("x-youtube-client-version", "1.20221019.01.00")
                set("x-goog-authuser", "0")
                set("x-origin", "https://music.youtube.com")
                set("origin", "https://music.youtube.com")
                set("user-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0")
            }

            for (header in account_headers ?: emptyList()) {
                header(header.first, header.second)
            }

            setBody(
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
                    "videoId": "$video_id"
                }
                """.trimIndent()
            )
        }

        val formats: YoutubeFormatsResponse = json.decodeFromString(response.bodyAsText())!!
        val best_format: YoutubeVideoFormat = formats.streamingData.adaptiveFormats.filter { it.audio_only }.maxBy { it.bitrate }

        return best_format.url!!
    }

    private suspend fun getPipedVideoStreamUrl(video_id: String): String {
        var retries: Int = MAX_RETRIES
        while (retries-- > 0) {
            val response: HttpResponse
            try {
                response = withTimeout(2000) {
                    http_client.get("https://pipedapi.kavin.rocks/streams/$video_id")
                }
            }
            catch (_: TimeoutCancellationException) {
                delay(1000)
                continue
            }

            val streams: PipedStreamsResponse = json.decodeFromString(response.bodyAsText())!!
            return streams.audioStreams.maxBy { it.bitrate }.url
        }

        throw RuntimeException("Could not get streams after $MAX_RETRIES attempts")
    }
}
