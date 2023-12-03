package spms.player

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val MAX_RETRIES: Int = 3

@Serializable
private data class YoutubePlayerResponse(val streamingData: StreamingData) {
    @Serializable
    data class StreamingData(val formats: List<Format>)
    @Serializable
    data class Format(val approxDurationMs: String)
}

@Serializable
private data class PipedStreamsResponse(val audioStreams: List<AudioStream>) {
    @Serializable
    data class AudioStream(
        val url: String,
        val bitrate: Int
    )
}

object VideoInfoProvider {
    private val http_client: HttpClient = HttpClient()
    private val json: Json = Json { ignoreUnknownKeys = true }

    suspend fun getVideoDuration(video_id: String): Long {
        var retries: Int = MAX_RETRIES
        while (retries-- > 0) {
            val response: HttpResponse
            try {
                response = withTimeout(2000) {
                    http_client.post("https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8&prettyPrint=false") {
                        headers {
                            set("accept", "*")
                            set("content-type", "application/json")
                            set("x-youtube-client-name", "67")
                            set("x-youtube-client-version", "1.20221019.01.00")
                            set("x-goog-authuser", "1")
                            set("x-origin", "https://www.youtube.com")
                            set("origin", "https://www.youtube.com")
                        }

                        setBody(
                            """
                            { 
                                "context": {
                                    "client":{
                                        "hl": "en",
                                        "platform": "DESKTOP",
                                        "clientName": "WEB_REMIX",
                                        "clientVersion": "1.20230306.01.00",
                                        "userAgent": "Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0",
                                        "acceptHeader": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                                    },
                                    "user": {}
                                },
                                "videoId": "$video_id"
                            }
                            """.trimIndent()
                        )
                    }
                }
            }
            catch (_: TimeoutCancellationException) {
                delay(1000)
                continue
            }

            val streams: YoutubePlayerResponse = json.decodeFromString(response.bodyAsText())!!
            return streams.streamingData.formats.firstNotNullOf { it.approxDurationMs.toLongOrNull() }
        }

        throw RuntimeException("Could not get duration after $MAX_RETRIES attempts")
    }

    suspend fun getVideoStreamUrl(video_id: String): String {
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
