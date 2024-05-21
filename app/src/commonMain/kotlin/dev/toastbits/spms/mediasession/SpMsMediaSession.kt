package dev.toastbits.spms.mediasession

import dev.toastbits.mediasession.MediaSession
import dev.toastbits.mediasession.MediaSessionMetadata
import dev.toastbits.mediasession.MediaSessionPlaybackStatus
import dev.toastbits.mediasession.MediaSessionLoopMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerEvent
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.player.Player
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.long
import kotlinx.serialization.json.int

class SpMsMediaSession private constructor(val player: Player, val session: MediaSession) {
    init {
        session.setIdentity("spmp")

        session.onPlayPause = {
            player.playPause()
        }
        session.onPlay = {
            player.play()
        }
        session.onPause = {
            player.pause()
        }
        session.onNext = {
            player.seekToNext()
        }
        session.onPrevious = {
            player.seekToPrevious()
        }
        session.onSeek = { by_ms ->
            player.seekToTime(player.current_position_ms + by_ms)
        }
        session.onSetPosition = { to_ms ->
            player.seekToTime(to_ms)
        }
        session.onSetLoop = { loop_mode ->
            player.setRepeatMode(
                when (loop_mode) {
                    MediaSessionLoopMode.NONE -> SpMsPlayerRepeatMode.NONE
                    MediaSessionLoopMode.ONE -> SpMsPlayerRepeatMode.ONE
                    MediaSessionLoopMode.ALL -> SpMsPlayerRepeatMode.ALL
                }
            )
        }

        onSongChanged(player.getItem())
        session.setPlaybackStatus(
            if (player.is_playing) MediaSessionPlaybackStatus.PLAYING
            else MediaSessionPlaybackStatus.PAUSED
        )

        session.setEnabled(true)
    }

    fun onPlayerEvent(event: SpMsPlayerEvent) {
        when (event.type) {
            SpMsPlayerEvent.Type.ITEM_TRANSITION -> {
                session.onPositionChanged()
                onSongChanged(player.getItem())
            }
            SpMsPlayerEvent.Type.PROPERTY_CHANGED -> {
                val key: String = event.properties["key"]!!.content
                val value: JsonPrimitive = event.properties["value"]!!
                when (key) {
                    "is_playing" -> {
                        session.setPlaybackStatus(
                            if (value.boolean) MediaSessionPlaybackStatus.PLAYING
                            else MediaSessionPlaybackStatus.PAUSED
                        )
                    }
                    "duration_ms" -> {
                        session.setMetadata(
                            session.metadata.copy(
                                length_ms = value.long
                            )
                        )
                    }
                    "repeat_mode" -> {
                        session.setLoopMode(
                            when (SpMsPlayerRepeatMode.entries[value.int]) {
                                SpMsPlayerRepeatMode.NONE -> MediaSessionLoopMode.NONE
                                SpMsPlayerRepeatMode.ONE -> MediaSessionLoopMode.ONE
                                SpMsPlayerRepeatMode.ALL -> MediaSessionLoopMode.ALL
                            }
                        )
                    }
                    else -> {}
                }
            }
            SpMsPlayerEvent.Type.SEEKED -> {
                session.onPositionChanged()
            }
            else -> {}
        }
    }

    private fun onSongChanged(song_id: String?) {
        session.setMetadata(
            MediaSessionMetadata(
                length_ms = player.duration_ms,
                art_url = song_id?.let { "https://img.youtube.com/vi/$it/maxresdefault.jpg" },
                url = song_id?.let { "https://music.youtube.com/watch?v=$it" },
                track_number = player.current_item_index
            )
        )
    }

    companion object {
        fun create(player: Player): SpMsMediaSession? {
            val session: MediaSession =
                MediaSession.create(
                    getPositionMs = { player.current_position_ms }
                )
            ?: return null

            return SpMsMediaSession(player, session)
        }
    }
}
