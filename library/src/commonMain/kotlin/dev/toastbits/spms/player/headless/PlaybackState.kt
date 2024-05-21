package dev.toastbits.spms.player.headless

import kotlin.time.*

internal interface PlaybackState {
    val is_playing: Boolean
    val current_position: Duration

    fun toPlaying(): PlaybackState
    fun toPaused(): PlaybackState

    class Playing(initial_position: Duration): PlaybackState {
        private val start_time: ComparableTimeMark = TimeSource.Monotonic.markNow() - initial_position

        override val is_playing: Boolean = true
        override val current_position: Duration get() = TimeSource.Monotonic.markNow() - start_time

        override fun toPlaying(): PlaybackState = this
        override fun toPaused(): PlaybackState = Paused(current_position)

        override fun toString(): String =
            "PlaybackState.Playing(current_position=$current_position)"
    }

    class Paused(override val current_position: Duration): PlaybackState {
        override val is_playing: Boolean = false

        override fun toPlaying(): PlaybackState = Playing(current_position)
        override fun toPaused(): PlaybackState = this

        override fun toString(): String =
            "PlaybackState.Paused(current_position=$current_position)"
    }
}
