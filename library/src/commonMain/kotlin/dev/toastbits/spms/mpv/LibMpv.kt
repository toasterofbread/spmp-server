package dev.toastbits.spms.mpv

import kotlin.reflect.KClass

expect class MpvHandle
expect class MpvEventData

expect class LibMpv {
    companion object {
        fun create(): LibMpv?
    }

    fun create(): MpvHandle?
    fun terminateDestroy(handle: MpvHandle)
    fun initialize(handle: MpvHandle): Int
    fun command(handle: MpvHandle, args: Array<String?>): Int
    inline fun <reified T> getProperty(handle: MpvHandle, name: String): T
    fun getPropertyString(handle: MpvHandle, name: String): String?
    inline fun <reified T> setProperty(handle: MpvHandle, name: String, data: T?): Int
    fun setPropertyString(handle: MpvHandle, name: String, data: String): Int
    inline fun <reified T> setOption(handle: MpvHandle, name: String, data: T?): Int
    fun setOptionString(handle: MpvHandle, name: String, data: String): Int
    fun observeProperty(handle: MpvHandle, reply_userdata: Long, name: String, format: Int): Int
    fun waitEvent(handle: MpvHandle, timeout: Double): MpvEvent?
    fun requestLogMessages(handle: MpvHandle, min_level: String): Int
    fun hookAdd(handle: MpvHandle, reply_userdata: Long, name: String, priority: Int): Int
    fun hookContinue(handle: MpvHandle, id: Long): Int
}

fun getMpvFormatOf(cls: KClass<*>): Int =
    when (cls) {
        Boolean::class -> MPV_FORMAT_FLAG
        Int::class -> MPV_FORMAT_INT64
        Double::class -> MPV_FORMAT_DOUBLE
        else -> throw NotImplementedError(cls.toString())
    }

const val MPV_FORMAT_STRING: Int = 1
const val MPV_FORMAT_FLAG: Int = 3
const val MPV_FORMAT_INT64: Int = 4
const val MPV_FORMAT_DOUBLE: Int = 5

const val MPV_EVENT_NONE: Int = 0
const val MPV_EVENT_SHUTDOWN: Int = 1
const val MPV_EVENT_LOG_MESSAGE: Int = 2
const val MPV_EVENT_GET_PROPERTY_REPLY: Int = 3
const val MPV_EVENT_SET_PROPERTY_REPLY: Int = 4
const val MPV_EVENT_COMMAND_REPLY: Int = 5
const val MPV_EVENT_START_FILE: Int = 6
const val MPV_EVENT_END_FILE: Int = 7
const val MPV_EVENT_FILE_LOADED: Int = 8
const val MPV_EVENT_CLIENT_MESSAGE: Int = 16
const val MPV_EVENT_VIDEO_RECONFIG: Int = 17
const val MPV_EVENT_AUDIO_RECONFIG: Int = 18
const val MPV_EVENT_SEEK: Int = 20
const val MPV_EVENT_PLAYBACK_RESTART: Int = 21
const val MPV_EVENT_PROPERTY_CHANGE: Int = 22
const val MPV_EVENT_QUEUE_OVERFLOW: Int = 24
const val MPV_EVENT_HOOK: Int = 25
