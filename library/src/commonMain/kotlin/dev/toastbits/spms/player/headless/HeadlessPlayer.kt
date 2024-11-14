package dev.toastbits.spms.player.headless

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import dev.toastbits.spms.socketapi.shared.SpMsPlayerEvent
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import dev.toastbits.spms.player.*
import dev.toastbits.spms.ReentrantLock
import kotlin.time.*
import kotlin.time.Duration

abstract class HeadlessPlayer(private val enable_logging: Boolean = true): Player {
    protected abstract fun getCachedItemDuration(item_id: String): Duration?
    protected abstract suspend fun loadItemDuration(item_id: String): Duration
    fun onDurationLoaded(item_id: String, item_duration: Duration) {
        if (item_id == getItem()) {
            onEvent(SpMsPlayerEvent.PropertyChanged("duration_ms", JsonPrimitive(item_duration.inWholeMilliseconds)))
        }
    }

    override fun release() {}

    private var _state: SpMsPlayerState = SpMsPlayerState.IDLE
    final override var state: SpMsPlayerState
        get() = _state
        private set(value) {
            log("Setting state to $value")
            _state = value
            onEvent(SpMsPlayerEvent.PropertyChanged("state", JsonPrimitive(value.ordinal)))
        }
    final override val is_playing: Boolean get() = playback_state.is_playing

    final override val item_count: Int get() = queue.size

    final override var current_item_index: Int = -1
        private set

    private var playback_state: PlaybackState = PlaybackState.Paused(Duration.ZERO)

    final override val current_position_ms: Long
        get() = playback_state.current_position.inWholeMilliseconds
    final override val duration_ms: Long get() = queue.getOrNull(current_item_index)?.let { getCachedItemDuration(it)?.inWholeMilliseconds } ?: 0
    final override var repeat_mode: SpMsPlayerRepeatMode = SpMsPlayerRepeatMode.NONE
        private set

    private val queue: MutableList<String> = mutableListOf()

    private val playback_scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private val playback_lock: ReentrantLock = ReentrantLock()
    private inline fun <T> withLock(block: () -> T): T = playback_lock.withLock(block)

    private val player_running: Boolean get() = is_playing || state == SpMsPlayerState.BUFFERING

    private fun log(message: Any?) {
        if (enable_logging) {
            println("HeadlessPlayer (state=$playback_state): $message")
        }
    }

    private fun onItemPlaybackEnded() {
        log("Item playback ended")

        if (repeat_mode == SpMsPlayerRepeatMode.ONE) {
            seekToTime(0)
            play()
            return
        }

        if (current_item_index + 1 == queue.size) {
            if (repeat_mode == SpMsPlayerRepeatMode.ALL) {
                seekToItem(0)
            }
            else {
                state = SpMsPlayerState.ENDED
            }
        }
        else {
            current_item_index++
            playback_state = PlaybackState.Paused(Duration.ZERO)
            onItemTransition(current_item_index)
        }
    }

    private fun onItemTransition(to: Int) {
        onEvent(SpMsPlayerEvent.ItemTransition(to))

        val item_id: String = queue.getOrNull(to) ?: return

        val duration: Duration? = getCachedItemDuration(item_id)
        onEvent(SpMsPlayerEvent.PropertyChanged("duration_ms", JsonPrimitive(duration?.inWholeMilliseconds ?: 0)))
    }

    override fun play() {
        if (!canPlay()) {
            return
        }

        withLock {
            log("play(): Running=$player_running")

            if (!player_running) {
                if (current_item_index < 0) {
                    if (queue.isNotEmpty()) {
                        current_item_index = 0
                        onItemTransition(0)
                    }
                    else {
                        log("play(): No items added, cannot play")
                        return
                    }
                }

                val item_id: String = queue[current_item_index]
                var duration: Duration? = getCachedItemDuration(item_id)

                if (duration == null) {
                    state = SpMsPlayerState.BUFFERING
                    playback_state = playback_state.toPaused()
                }
                else {
                    state = SpMsPlayerState.READY
                    playback_state = playback_state.toPlaying()
                    onEvent(SpMsPlayerEvent.PropertyChanged("is_playing", JsonPrimitive(true)))
                }

                log("play() $item_id: Launching timer")

                playback_scope.launch {
                    if (duration == null) {
                        log("play() $item_id: loadItemDuration()")

                        duration = loadItemDuration(item_id)

                        withLock {
                            state = SpMsPlayerState.READY
                            playback_state = playback_state.toPlaying()
                            onEvent(SpMsPlayerEvent.PropertyChanged("is_playing", JsonPrimitive(true)))
                        }
                    }
                    else {
                        log("play() $item_id: Using existing duration")
                    }

                    val wait_duration: Duration = duration!! - playback_state.current_position
                    log("play() will wait for ${wait_duration}ms ($duration)")
                    delay(wait_duration)
                    log("play() resumed after delay")

                    withLock {
                        state = SpMsPlayerState.IDLE
                        playback_state = PlaybackState.Paused(duration)
                        onEvent(SpMsPlayerEvent.PropertyChanged("is_playing", JsonPrimitive(false)))
                        onItemPlaybackEnded()
                    }
                }
            }
        }
    }

    override fun pause() {
        withLock {
            log("pause()")
            if (player_running) {
                if (state == SpMsPlayerState.READY) {
                    onEvent(SpMsPlayerEvent.PropertyChanged("is_playing", JsonPrimitive(false)))
                }
                stop()
            }
            else {
                state = SpMsPlayerState.IDLE
            }
        }
    }

    override fun playPause() {
        withLock {
            log("playPause()")
            if (player_running) {
                pause()
            }
            else {
                play()
            }
        }
    }

    private fun stop() {
        playback_scope.coroutineContext.cancelChildren()
        playback_state = playback_state.toPaused()
        state = SpMsPlayerState.IDLE
    }

    private inline fun modifyPlayback(resume: Boolean = true, action: () -> Unit) {
        val was_playing: Boolean = player_running
        log("Modifying playback (playing=$was_playing)")
        if (was_playing) {
            stop()
        }
        action()
        if (resume && was_playing) {
            play()
        }
    }

    override fun seekToTime(position_ms: Long) {
        log("seekToTime($position_ms)")
        withLock {
            val target_position: Long = position_ms.coerceIn(0, duration_ms)
            modifyPlayback {
                playback_state = with (Duration) {
                    PlaybackState.Paused(target_position.milliseconds)
                }
            }

            onEvent(SpMsPlayerEvent.SeekedToTime(target_position))
        }
    }

    private fun performSeekToItem(index: Int) {
        stop()
        playback_state = PlaybackState.Paused(Duration.ZERO)
        current_item_index = index
        onEvent(SpMsPlayerEvent.PropertyChanged("is_playing", JsonPrimitive(false)))
    }

    override fun seekToItem(index: Int, position_ms: Long) {
        withLock {
            log("seekToItem($index)")

            val target_index: Int = formatQueueIndex(index) ?: return
            performSeekToItem(target_index)
            seekToTime(position_ms)

            onItemTransition(target_index)
        }
    }

    override fun seekToNext(): Boolean {
        withLock {
            log("seekToNext()")

            var seek_target: Int = current_item_index + 1
            if (seek_target >= queue.size) {
                if (repeat_mode != SpMsPlayerRepeatMode.ALL) {
                    return false
                }
                seek_target = 0
            }

            performSeekToItem(seek_target)

            onItemTransition(current_item_index)

            return true
        }
    }

    override fun seekToPrevious(repeat_threshold: Duration?): Boolean {
        withLock {
            log("seekToPrevious()")

            if (shouldRepeatOnSeekToPrevious(repeat_threshold)) {
                seekToTime(0)
                return true
            }
            else if (current_item_index == 0) {
                return false
            }
            else {
                performSeekToItem(current_item_index - 1)
                onItemTransition(current_item_index)

                return true
            }
        }
    }

    override fun setRepeatMode(repeat_mode: SpMsPlayerRepeatMode) {
        withLock {
            log("setRepeatMode($repeat_mode)")
            this.repeat_mode = repeat_mode
            onEvent(SpMsPlayerEvent.PropertyChanged("repeat_mode", JsonPrimitive(repeat_mode.ordinal)))
        }
    }

    override fun getItem(): String? {
        withLock {
            log("getItem()")
            return queue.getOrNull(current_item_index)
        }
    }

    override fun getItem(index: Int): String? {
        withLock {
            log("getItem($index)")
            val target_index: Int = formatQueueIndex(index) ?: return null
            return queue.getOrNull(target_index)
        }
    }

    private fun formatQueueIndex(index: Int, allow_new: Boolean = false): Int? {
        val max: Int = if (allow_new) queue.size else queue.size - 1
        if (index < 0) {
            return max
        }
        else if (index > max) {
            return null
        }
        return index
    }

    override fun addItem(item_id: String, index: Int): Int {
        withLock {
            log("addItem($item_id, $index)")
            val target_index: Int = formatQueueIndex(index, true) ?: queue.size
            queue.add(target_index, item_id)

            if (queue.size == 1) {
                current_item_index = 0
            }
            else if (target_index < current_item_index) {
                current_item_index++
            }

            onEvent(SpMsPlayerEvent.ItemAdded(item_id, target_index))

            if (queue.size == 1) {
                onItemTransition(0)
            }

            return target_index
        }
    }

    override fun moveItem(from: Int, to: Int) {
        withLock {
            log("moveItem($from, $to)")
            val target_from: Int = formatQueueIndex(from) ?: return
            val target_to: Int = formatQueueIndex(to) ?: return

            if (target_from == target_to) {
                return
            }

            val item: String = queue.removeAt(target_from)
            queue.add(target_to, item)

            if (target_from == current_item_index) {
                current_item_index = target_to
            }

            onEvent(SpMsPlayerEvent.ItemMoved(target_from, target_to))
        }
    }

    override fun removeItem(index: Int) {
        withLock {
            log("removeItem($index)")
            val target_index: Int = formatQueueIndex(index) ?: return

            if (queue.size == 1) {
                clearQueue()
                return
            }

            queue.removeAt(target_index)

            if (target_index == current_item_index) {
                if (current_item_index + 1 == queue.size) {
                    current_item_index--
                }
                modifyPlayback {
                    playback_state = PlaybackState.Paused(Duration.ZERO)
                }
            }
            else if (target_index < current_item_index) {
                current_item_index--
            }

            onEvent(SpMsPlayerEvent.ItemRemoved(target_index))

            if (target_index == current_item_index) {
                onItemTransition(current_item_index)
            }
        }
    }

    override fun clearQueue() {
        withLock {
            log("clearQueue()")
            stop()
            queue.clear()
            current_item_index = -1
            playback_state = PlaybackState.Paused(Duration.ZERO)

            onEvent(SpMsPlayerEvent.QueueCleared())
            onItemTransition(-1)
        }
    }

    override fun setVolume(value: Double) {}

    override fun toString(): String =
        "HeadlessPlayer()"
}
