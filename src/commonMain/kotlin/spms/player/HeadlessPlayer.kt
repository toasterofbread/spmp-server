package spms.player

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import spms.socketapi.shared.SpMsPlayerEvent
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState
import kotlin.system.getTimeNanos

abstract class HeadlessPlayer(private val enable_logging: Boolean = true): Player {
    protected abstract fun getCachedItemDuration(item_id: String): Long?
    protected abstract suspend fun loadItemDuration(item_id: String): Long
    fun onDurationLoaded(item_id: String, item_duration_ms: Long) {
        if (item_id == getItem()) {
            onEvent(SpMsPlayerEvent.PropertyChanged("duration_ms", JsonPrimitive(item_duration_ms)))
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
    final override var is_playing: Boolean = false
        private set

    final override val item_count: Int get() = queue.size

    final override var current_item_index: Int = -1
        private set

    final override val current_position_ms: Long
        get() =
            if (is_playing) (getTimeNanos() - playback_mark) / 1000000
            else playback_mark
    final override val duration_ms: Long get() = queue.getOrNull(current_item_index)?.let { getCachedItemDuration(it) } ?: 0
    final override var repeat_mode: SpMsPlayerRepeatMode = SpMsPlayerRepeatMode.NONE
        private set
    final override var volume: Double = 1.0
        private set

    private val queue: MutableList<String> = mutableListOf()

    private val playback_scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private var playback_mark: Long = 0

    private val playback_lock: ReentrantLock = ReentrantLock()
    private inline fun <T> withLock(block: () -> T): T = playback_lock.withLock(block)

    private val player_running: Boolean get() = is_playing || state == SpMsPlayerState.BUFFERING

    private fun log(message: Any?) {
        if (enable_logging) {
            println("HeadlessPlayer: $message")
        }
    }

    private fun onItemPlaybackEnded() {
        log("Item playback ended")

        if (current_item_index + 1 == queue.size) {
            state = SpMsPlayerState.ENDED
        }
        else {
            current_item_index++
            playback_mark = 0
            onQueueOrPositionChanged()
            onItemTransition(current_item_index)
        }
    }

    private fun onItemTransition(to: Int) {
        onEvent(SpMsPlayerEvent.ItemTransition(to))

        val item_id: String = queue.getOrNull(to) ?: return

        val duration: Long? = getCachedItemDuration(item_id)
        onEvent(SpMsPlayerEvent.PropertyChanged("duration_ms", JsonPrimitive(duration ?: 0)))
    }

    private fun onQueueOrPositionChanged() {
    }

    override fun play() {
        withLock {
            log("play(): Running=$player_running")

            if (!player_running) {
                if (current_item_index < 0) {
                    if (queue.isNotEmpty()) {
                        current_item_index = 0
                        onQueueOrPositionChanged()
                        onItemTransition(0)
                    }
                    else {
                        log("play(): No items added, cannot play")
                        return
                    }
                }

                val current_position: Long = playback_mark
                playback_mark = getTimeNanos() - current_position

                val item_id: String = queue[current_item_index]
                var duration: Long? = getCachedItemDuration(item_id)

                if (duration == null) {
                    state = SpMsPlayerState.BUFFERING
                }
                else {
                    state = SpMsPlayerState.READY
                    is_playing = true
                    onEvent(SpMsPlayerEvent.PropertyChanged("is_playing", JsonPrimitive(true)))
                }

                log("play() $item_id: Launching timer")

                playback_scope.launch {
                    if (duration == null) {
                        log("play() $item_id: loadItemDuration()")

                        duration = loadItemDuration(item_id)

                        if (duration == null) {
                            state = SpMsPlayerState.IDLE
                            is_playing = false
                            playback_mark = 0
                            println("play(): Duration is null, cannot play")
                            TODO()
                        }

                        withLock {
                            state = SpMsPlayerState.READY
                            is_playing = true
                            onEvent(SpMsPlayerEvent.PropertyChanged("is_playing", JsonPrimitive(true)))
                        }
                    }
                    else {
                        log("play() $item_id: Using existing duration")
                    }

                    log("play() will wait for ${duration!! - current_position}ms (${duration} $current_position)")
                    delay(duration!! - current_position)
                    log("play() resumed after delay")

                    withLock {
                        is_playing = false
                        state = SpMsPlayerState.IDLE
                        playback_mark = duration!!
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

                playback_mark = current_position_ms
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
        is_playing = false
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
                playback_mark = target_position
            }

            onEvent(SpMsPlayerEvent.SeekedToTime(target_position))
        }
    }

    private fun performSeekToItem(index: Int) {
        stop()
        playback_mark = 0
        current_item_index = index
        onEvent(SpMsPlayerEvent.PropertyChanged("is_playing", JsonPrimitive(false)))
    }

    override fun seekToItem(index: Int) {
        withLock {
            log("seekToItem($index)")

            val target_index: Int = formatQueueIndex(index) ?: return
            performSeekToItem(target_index)

            onQueueOrPositionChanged()
            onItemTransition(target_index)
        }
    }

    override fun seekToNext(): Boolean {
        withLock {
            log("seekToNext()")
            if (current_item_index + 1 == queue.size) {
                return false
            }

            performSeekToItem(current_item_index + 1)

            onQueueOrPositionChanged()
            onItemTransition(current_item_index)

            return true
        }
    }

    override fun seekToPrevious(): Boolean {
        withLock {
            log("seekToPrevious()")
            if (current_item_index == 0) {
                return false
            }

            performSeekToItem(current_item_index - 1)

            onQueueOrPositionChanged()
            onItemTransition(current_item_index)

            return true
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

            onQueueOrPositionChanged()
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

            onQueueOrPositionChanged()
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
                    playback_mark = 0
                }
            }
            else if (target_index < current_item_index) {
                current_item_index--
            }

            onQueueOrPositionChanged()
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
            playback_mark = 0

            onEvent(SpMsPlayerEvent.QueueCleared())
            onItemTransition(-1)
        }
    }

    override fun setVolume(value: Double) {
        log("setVolume($value)")
        volume = value
    }

    override fun toString(): String =
        "HeadlessPlayer()"
}
