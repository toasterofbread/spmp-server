package mpv

private fun filenameToId(filename: String): String {
    val index = filename.indexOf("v=")
    return filename.substring(index + 2)
}

private fun idToFilename(video_id: String): String {
    return "https://www.youtube.com/watch?v=$video_id"
}

open class MpvClientImpl(headless: Boolean = true): LibMpvClient(headless) {
    override val state: MpvClient.State
        get() =
            if (getProperty("eof-reached")) MpvClient.State.ENDED
            else if (getProperty("idle-active")) MpvClient.State.IDLE
            else if (getProperty("paused-for-cache")) MpvClient.State.BUFFERING
            else MpvClient.State.READY
    override val is_playing: Boolean
        get() = !getProperty<Boolean>("core-idle")
    override val item_count: Int
        get() = getProperty("playlist-count")
    override val current_item_index: Int
        get() = getProperty("playlist-playing-pos")
    override val current_position_ms: Long
        get() = (getProperty<Double>("playback-time") * 1000).toLong()
    override val duration_ms: Long
        get() = (getProperty<Double>("duration") * 1000).toLong()
    override val repeat_mode: MpvClient.RepeatMode
        get() = MpvClient.RepeatMode.NONE // TODO
    override val volume: Double
        get() = getProperty("volume")

    override fun play() {
        setProperty("pause", false)
    }
    override fun pause() {
        setProperty("pause", true)
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
    }

    override fun seekToItem(index: Int) {
        require(index >= 0)
        runCommand("playlist-play-index", index.toString())
    }

    override fun seekToNext(): Boolean {
        return runCommand("playlist-next", check_result = false) == 0
    }

    override fun seekToPrevious(): Boolean {
        return runCommand("playlist-prev", check_result = false) == 0
    }

    override fun getItem(): String? = getItem(current_item_index)

    override fun getItem(index: Int): String? {
        if (index < 0) {
            return null
        }

        try {
            return filenameToId(getProperty("playlist/$index/filename"))
        }
        catch (_: Throwable) {
            return null
        }
    }

    override fun addItem(item_id: String, index: Int): Int {
        val filename: String = idToFilename(item_id)

        val sc = item_count
        runCommand("loadfile", filename, if (sc == 0) "replace" else "append")

        if (index < 0 || index >= sc) {
            return sc
        }

        runCommand("playlist-move", sc, index)
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
    }

    override fun removeItem(index: Int) {
        require(index >= 0)
        runCommand("playlist-remove", index)
    }

    override fun clearQueue() {
        runCommand("playlist-clear")
    }

    override fun setVolume(value: Float) {
        setProperty("volume", value.toDouble())
    }
}
