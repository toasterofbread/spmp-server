package spms.player

import kotlinx.cinterop.ExperimentalForeignApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.external.YoutubeVideoFormat
import dev.toastbits.ytmkt.formats.PipedVideoFormatsEndpoint
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint

@OptIn(ExperimentalForeignApi::class)
object VideoInfoProvider {
    private val api: YoutubeiApi = YoutubeiApi()
    private val piped_formats_endpoint: PipedVideoFormatsEndpoint = PipedVideoFormatsEndpoint(api)

    suspend fun getVideoStreamUrl(video_id: String, account_headers: Map<String, String>? = null): String {
        try {
            return api.VideoFormats.getVideoStreamUrl(video_id, account_headers)
        }
        catch (e: Throwable) {
            try {
                return piped_formats_endpoint.getVideoStreamUrl(video_id, account_headers)
            }
            catch (_: Throwable) {
                throw e
            }
        }
    }

    private suspend fun VideoFormatsEndpoint.getVideoStreamUrl(video_id: String, account_headers: Map<String, String>? = null): String {
        val formats: List<YoutubeVideoFormat> = getVideoFormats(video_id).getOrThrow()
        val best_format: YoutubeVideoFormat = formats.filter { it.isAudioOnly() && it.url != null }.maxBy { it.bitrate }
        return best_format.url!!
    }
}
