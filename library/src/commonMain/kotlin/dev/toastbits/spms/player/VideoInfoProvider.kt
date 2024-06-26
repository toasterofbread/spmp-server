package dev.toastbits.spms.player

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.model.external.YoutubeVideoFormat
import dev.toastbits.ytmkt.formats.PipedVideoFormatsEndpoint
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint

object VideoInfoProvider {
    private val api: YoutubeiApi = YoutubeiApi()
    private val piped_formats_endpoint: PipedVideoFormatsEndpoint = PipedVideoFormatsEndpoint(api)

    fun setAuthHeaders(headers: Map<String, String>?) {
        api.user_auth_state = YoutubeiAuthenticationState.fromMap(api, headers.orEmpty(), null)
    }

    suspend fun getVideoStreamUrl(video_id: String): String {
        try {
            return api.VideoFormats.getVideoStreamUrl(video_id)
        }
        catch (e: Throwable) {
            try {
                return piped_formats_endpoint.getVideoStreamUrl(video_id)
            }
            catch (_: Throwable) {
                throw e
            }
        }
    }

    private suspend fun VideoFormatsEndpoint.getVideoStreamUrl(video_id: String): String {
        val formats: List<YoutubeVideoFormat> = getVideoFormats(video_id).getOrThrow()
        val best_format: YoutubeVideoFormat = formats.filter { it.isAudioOnly() && it.url != null }.maxBy { it.bitrate }
        return best_format.url!!
    }
}
