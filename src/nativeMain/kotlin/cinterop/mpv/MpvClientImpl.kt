package cinterop.mpv

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonPrimitive
import libmpv.*
import okio.FileSystem
import okio.Path.Companion.toPath
import spms.player.Player
import spms.player.PlayerEvent
import spms.player.VideoInfoProvider
import spms.server.SpMs
import kotlin.math.roundToInt

private const val URL_PREFIX: String = "spmp://"

@OptIn(ExperimentalForeignApi::class)
inline fun <reified T: CPointed> CPointer<*>?.pointedAs(): T =
    this!!.reinterpret<T>().pointed

@OptIn(ExperimentalForeignApi::class)
abstract class MpvClientImpl(headless: Boolean = true): LibMpvClient(headless) {
    private val coroutine_scope = CoroutineScope(Job())
    private var auth_headers: Map<String, String>? = null
    private val local_files: MutableMap<String, String> = mutableMapOf()

    private fun urlToId(url: String): String? = if (url.startsWith(URL_PREFIX)) url.drop(URL_PREFIX.length) else null
    private fun idToUrl(item_id: String): String = URL_PREFIX + item_id

    init {
        initialise()
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

    private val current_item_playlist_id: Int
        get() = getProperty("playlist/${current_item_index}/id")

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

    fun setAuthHeaders(headers: Map<String, String>?) {
        auth_headers = headers
    }

    fun clearLocalFiles() {
        local_files.clear()
    }

    fun addLocalFile(item_id: String, file_path: String) {
        local_files[item_id] = file_path
    }

    fun removeLocalFile(file_id: String) {
        local_files.remove(file_id)
    }

    override fun toString(): String = "MpvClientImpl(headless=$headless)"

    private fun initialise() {
        //        requestLogMessages()

        addHook("on_load")
        observeProperty<Boolean>("core-idle")

        coroutine_scope.launch {
            eventLoop()
        }
    }

    private suspend fun eventLoop() = withContext(Dispatchers.IO) {
        while (true) {
            val event: mpv_event? = waitForEvent()

            when (event?.event_id) {
                MPV_EVENT_START_FILE -> {
                    val data: mpv_event_start_file = event.data.pointedAs()
                    if (data.playlist_entry_id.toInt() != current_item_playlist_id) {
                        continue
                    }

                    onEvent(PlayerEvent.ItemTransition(current_item_index), clientless = true)
                }
                MPV_EVENT_PLAYBACK_RESTART -> {
                    onEvent(PlayerEvent.PropertyChanged("state", JsonPrimitive(state.ordinal)), clientless = true)
                    onEvent(PlayerEvent.PropertyChanged("duration_ms", JsonPrimitive(duration_ms)), clientless = true)
                }
                MPV_EVENT_END_FILE -> {
                    onEvent(PlayerEvent.PropertyChanged("state", JsonPrimitive(state.ordinal)), clientless = true)
                }
                MPV_EVENT_FILE_LOADED -> {
                    onEvent(PlayerEvent.ReadyToPlay(), clientless = true)
                }
                MPV_EVENT_SHUTDOWN -> {
                    onShutdown()
                }
                MPV_EVENT_PROPERTY_CHANGE -> {
                    val data: mpv_event_property = event.data.pointedAs()

                    when (data.name?.toKString()) {
                        "core-idle" -> {
                            val playing: Boolean = !data.data.pointedAs<BooleanVar>().value
                            onEvent(PlayerEvent.PropertyChanged("is_playing", JsonPrimitive(playing)), clientless = true)
                        }
                    }
                }

                MPV_EVENT_HOOK -> {
                    val data: mpv_event_hook = event.data.pointedAs()
                    launch {
                        onMpvHook(data.name?.toKString(), data.id)
                    }
                }

                MPV_EVENT_LOG_MESSAGE -> {
                    if (SpMs.logging_enabled) {
                        val message: mpv_event_log_message = event.data.pointedAs()
                        SpMs.log("From mpv (${message.prefix?.toKString()}): ${message.text?.toKString()}")
                    }
                }
            }
        }
    }

    private suspend fun onMpvHook(hook_name: String?, hook_id: ULong) {
        try {
            when (hook_name) {
                "on_load" -> {
                    val video_id: String = urlToId(getProperty<String>("stream-open-filename")) ?: return
                    val stream_url: String

                    val local_file_path: String? = local_files[video_id]
                    if (local_file_path != null && FileSystem.SYSTEM.exists(local_file_path.toPath())) {
                        stream_url = "file://" + local_file_path
                    }
                    else {
                        stream_url =
                            try {
                                VideoInfoProvider.getVideoStreamUrl(video_id, auth_headers.orEmpty())
                            }
                            catch (e: Throwable) {
                                RuntimeException("Getting video stream url for $video_id failed", e).printStackTrace()
                                return
                            }
                    }

                    setProperty("stream-open-filename", stream_url)
                }
                else -> throw NotImplementedError("Unknown mpv hook name '$hook_name'")
            }
        }
        catch (e: Throwable) {
            RuntimeException("Processing mpv hook $hook_name failed", e).printStackTrace()
        }
        finally {
            continueHook(hook_id)
        }
    }
}
