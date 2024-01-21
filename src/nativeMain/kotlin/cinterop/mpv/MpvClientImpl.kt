package cinterop.mpv

import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonPrimitive
import libmpv.*
import spms.player.Player
import spms.player.PlayerEvent
import spms.player.StreamProviderServer
import kotlin.math.roundToInt

abstract class MpvClientImpl(server_port: Int, headless: Boolean = true): LibMpvClient(headless) {
    private val coroutine_scope = CoroutineScope(Dispatchers.IO)
    private val stream_provider_server = StreamProviderServer(server_port)

    private fun urlToId(url: String): String = stream_provider_server.urlToId(url)
    private fun idToUrl(item_id: String): String = stream_provider_server.idToUrl(item_id)

    override fun onShutdown() {
        super.onShutdown()
        stream_provider_server.stop()
    }

    init {
        coroutine_scope.launch {
            while (true) {
                val event: mpv_event_id? = waitForEvent()
                when (event) {
                    MPV_EVENT_START_FILE -> {
                        onEvent(PlayerEvent.ItemTransition(current_item_index), clientless = true)
                        setProperty("pause", true)
                        onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(false)))
                    }
                    MPV_EVENT_PLAYBACK_RESTART -> {
                        onEvent(PlayerEvent.PropertyChanged("state", JsonPrimitive(state.ordinal)), clientless = true)
                        onEvent(PlayerEvent.PropertyChanged("duration_ms", JsonPrimitive(duration_ms)), clientless = true)
                        onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(is_playing)), clientless = true)
                    }
                    MPV_EVENT_END_FILE -> {
                        onEvent(PlayerEvent.PropertyChanged("state", JsonPrimitive(state.ordinal)), clientless = true)
                        onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(is_playing)), clientless = true)
                    }
                    MPV_EVENT_FILE_LOADED -> {
                        onEvent(PlayerEvent.ReadyToPlay(), clientless = true)
                    }
                    MPV_EVENT_SHUTDOWN -> {
                        onShutdown()
                    }
                }
            }
        }
    }

    override fun release() {
        coroutine_scope.cancel()
        super.release()
    }

    override val state: Player.State
        get() =
            if (getProperty("eof-reached")) Player.State.ENDED
            else if (getProperty("idle-active")) Player.State.IDLE
            else if (getProperty("paused-for-cache")) Player.State.BUFFERING
            else Player.State.READY
    override val is_playing: Boolean
        get() = !getProperty<Boolean>("core-idle")
    override val item_count: Int
        get() = getProperty("playlist-count")
    override val current_item_index: Int
        get() = getProperty("playlist-playing-pos")
    override val current_position_ms: Long
        get() = (getProperty<Double>("playback-time") * 1000).toLong().coerceAtLeast(0)
    override val duration_ms: Long
        get() = (getProperty<Double>("duration") * 1000).toLong().coerceAtLeast(0)
    override val repeat_mode: Player.RepeatMode
        get() = Player.RepeatMode.NONE // TODO
    override val volume: Double
        get() = getProperty("volume")

    override fun play() {
        setProperty("pause", false)
        onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(is_playing)))
    }
    override fun pause() {
        setProperty("pause", true)
        onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(is_playing)))
    }
    override fun playPause() {
        if (is_playing) {
            pause()
        }
        else {
            play()
        }
    }

    override fun seekToTime(position_ms: Long) {
        var current: Long = position_ms

        val milliseconds: String = (current % 1000).toString().padStart(2, '0')
        current /= 1000

        val seconds = (current % 60).toString().padStart(2, '0')
        current /= 60

        val minutes: String = (current % 60).toString().padStart(2, '0')
        val hours: String = (current / 60).toString().padStart(2, '0')

        runCommand("seek", "$hours:$minutes:$seconds.$milliseconds", "absolute", "exact", check_result = false)
        onEvent(PlayerEvent.SeekedToTime(position_ms))
    }

    override fun seekToItem(index: Int) {
        val max: Int = item_count - 1
        val target_index: Int = if (index < 0) max else index.coerceAtMost(max)
        runCommand("playlist-play-index", target_index.toString())
        onEvent(PlayerEvent.ItemTransition(target_index))
    }

    override fun seekToNext(): Boolean {
        if (runCommand("playlist-next", check_result = false) == 0) {
            onEvent(PlayerEvent.ItemTransition(current_item_index))
            return true
        }
        return false
    }

    override fun seekToPrevious(): Boolean {
        if (runCommand("playlist-prev", check_result = false) == 0) {
            onEvent(PlayerEvent.ItemTransition(current_item_index))
            return true
        }
        return false
    }

    override fun getItem(): String? = getItem(current_item_index)

    override fun getItem(index: Int): String? {
        if (index < 0) {
            return null
        }

        try {
            return urlToId(getProperty("playlist/$index/filename"))
        }
        catch (_: Throwable) {
            return null
        }
    }

    override fun addItem(item_id: String, index: Int): Int {
        val filename: String = idToUrl(item_id)

        val sc: Int = item_count
        runCommand("loadfile", filename, if (sc == 0) "replace" else "append")

        if (index < 0 || index >= sc) {
            onEvent(PlayerEvent.ItemAdded(item_id, sc))
            return sc
        }

        runCommand("playlist-move", sc, index)
        onEvent(PlayerEvent.ItemAdded(item_id, index))

        return index
    }

    override fun moveItem(from: Int, to: Int) {
        require(from >= 0)
        require(to >= 0)

        if (from == to) {
            return
        }

        // https://mpv.io/manual/master/#command-interface-playlist-move
        if (from < to) {
            runCommand("playlist-move", from, to + 1)
        }
        else {
            runCommand("playlist-move", from, to)
        }

        onEvent(PlayerEvent.ItemMoved(from, to))
    }

    override fun removeItem(index: Int) {
        require(index >= 0)
        runCommand("playlist-remove", index)
        onEvent(PlayerEvent.ItemRemoved(index))
    }

    override fun clearQueue() {
        runCommand("playlist-clear")
        onEvent(PlayerEvent.QueueCleared())
    }

    override fun setVolume(value: Double) {
        setProperty("volume", (value * 100).roundToInt())
    }

    override fun toString(): String = "MpvClientImpl(headless=$headless)"
}
