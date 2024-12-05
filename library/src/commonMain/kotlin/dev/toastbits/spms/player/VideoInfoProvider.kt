package dev.toastbits.spms.player

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.model.external.YoutubeVideoFormat
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint

object VideoInfoProvider {
    private val api: YoutubeiApi = YoutubeiApi()

    fun setAuthHeaders(headers: Map<String, String>?) {
        api.user_auth_state = YoutubeiAuthenticationState.fromMap(api, headers.orEmpty(), null)
    }

    suspend fun getVideoStreamUrl(video_id: String): String =
        api.VideoFormats.getVideoStreamUrl(video_id)

    private suspend fun VideoFormatsEndpoint.getVideoStreamUrl(video_id: String): String {
        val formats: List<YoutubeVideoFormat> = 
            getVideoFormats(
                video_id,
                filter = { it.isAudioOnly() && it.url != null }
            ).getOrThrow()
        
        return formats.maxBy { it.bitrate }.url!!
    }
}
