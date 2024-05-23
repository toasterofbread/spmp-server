package dev.toastbits.spms.mpv

import libmpv.*
import libmpv.client.*
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.foreign.Arena

actual class MpvHandle(val pointer: MemorySegment)

actual class LibMpv private constructor() {
    actual companion object {
        actual fun isAvailable(): Boolean {
            try {
                mpv_client_api_version()
            }
            catch (_: Throwable) {
                return false
            }

            return true
        }

        actual fun create(throw_on_fail: Boolean): LibMpv? {
            try {
                mpv_client_api_version()
            }
            catch (e: Throwable) {
                if (throw_on_fail) {
                    throw e
                }
                return null
            }
            return LibMpv()
        }
    }

    actual fun create(): MpvHandle? {
        setlocale(
            1, // LC_NUMERIC
            "C"
        )
        return MpvHandle(mpv_create_client(MemorySegment.NULL, MemorySegment.NULL))
    }

    actual fun terminateDestroy(handle: MpvHandle) {
        val arena: Arena = Arena.ofConfined()
        return mpv_terminate_destroy(handle.pointer)
    }

    actual fun initialize(handle: MpvHandle): Int {
        return mpv_initialize(handle.pointer)
    }

    actual fun command(handle: MpvHandle, args: Array<String?>): Int {
        val arena: Arena = Arena.ofConfined()
        return mpv_command(handle.pointer, args.memorySegment(arena))
    }

    actual inline fun <reified T> getProperty(handle: MpvHandle, name: String): T {
        if (T::class == String::class) {
            return getPropertyString(handle, name) as T
        }

        val arena: Arena = Arena.global()

        val pointer: MemorySegment
        val format: Int
        val extractor: () -> T

        when (T::class) {
            Boolean::class -> {
                pointer = arena.allocate(ValueLayout.JAVA_BOOLEAN.byteSize(), 1L)
                format = MPV_FORMAT_FLAG
                extractor = { pointer.get(ValueLayout.JAVA_BOOLEAN, 0) as T }
            }
            Int::class -> {
                pointer = arena.allocate(ValueLayout.JAVA_INT.byteSize(), 1L)
                format = MPV_FORMAT_INT64
                extractor = { pointer.get(ValueLayout.JAVA_INT, 0) as T }
            }
            Double::class -> {
                pointer = arena.allocate(ValueLayout.JAVA_DOUBLE.byteSize(), 1L)
                format = MPV_FORMAT_DOUBLE
                extractor = { pointer.get(ValueLayout.JAVA_DOUBLE, 0) as T }
            }
            else -> throw NotImplementedError(T::class.toString())
        }

        mpv_get_property(handle.pointer, name.memorySegment(arena), format, pointer)

        return extractor()
    }
    actual fun getPropertyString(handle: MpvHandle, name: String): String? {
        val arena: Arena = Arena.ofConfined()
        return mpv_get_property_string(handle.pointer, name.memorySegment(arena)).getString()
    }
    actual inline fun <reified T> setProperty(handle: MpvHandle, name: String, data: T?): Int {
        if (T::class == String::class) {
            return setPropertyString(handle, name, data as String)
        }

        val arena: Arena = Arena.ofConfined()
        val pointer: MemorySegment =
            if (data is Boolean) getPointerOf(arena, if (data) 1 else 0)
            else getPointerOf(arena, data)
        val format: Int = getMpvFormatOf(T::class)
        return mpv_set_property(handle.pointer, name.memorySegment(arena), format, pointer)
    }
    actual fun setPropertyString(handle: MpvHandle, name: String, data: String): Int {
        val arena: Arena = Arena.ofConfined()
        return mpv_set_property_string(handle.pointer, name.memorySegment(arena), data.memorySegment(arena))
    }
    actual inline fun <reified T> setOption(handle: MpvHandle, name: String, data: T?): Int {
        val arena: Arena = Arena.global()
        val pointer: MemorySegment = getPointerOf(arena, data)
        val format: Int = getMpvFormatOf(T::class)
        return mpv_set_option(handle.pointer, name.memorySegment(arena), format, pointer)
    }
    actual fun setOptionString(handle: MpvHandle, name: String, data: String): Int {
        val arena: Arena = Arena.ofConfined()
        return mpv_set_option_string(handle.pointer, name.memorySegment(arena), data.memorySegment(arena))
    }
    actual fun observeProperty(handle: MpvHandle, reply_userdata: Long, name: String, format: Int): Int {
        val arena: Arena = Arena.ofConfined()
        return mpv_observe_property(handle.pointer, reply_userdata, name.memorySegment(arena), format)
    }
    actual fun waitEvent(handle: MpvHandle, timeout: Double): MpvEvent {
        val arena: Arena = Arena.ofConfined()
        return InternalMpvEvent(mpv_wait_event(handle.pointer, timeout))
    }
    actual fun requestLogMessages(handle: MpvHandle, min_level: String): Int {
        val arena: Arena = Arena.ofConfined()
        return mpv_request_log_messages(handle.pointer, min_level.memorySegment(arena))
    }
    actual fun hookAdd(handle: MpvHandle, reply_userdata: Long, name: String, priority: Int): Int {
        val arena: Arena = Arena.ofConfined()
        return mpv_hook_add(handle.pointer, reply_userdata, name.memorySegment(arena), priority)
    }
    actual fun hookContinue(handle: MpvHandle, id: Long): Int {
        val arena: Arena = Arena.ofConfined()
        return mpv_hook_continue(handle.pointer, id)
    }

    inline fun <reified T> getPointerOf(arena: Arena, v: T? = null): MemorySegment =
        when (T::class) {
            Boolean::class -> arena.allocate(ValueLayout.JAVA_BOOLEAN.byteSize(), 1L).apply { if (v != null) set(ValueLayout.JAVA_BOOLEAN, 0L, v as Boolean) }
            Int::class -> arena.allocate(ValueLayout.JAVA_INT.byteSize(), 1L).apply { if (v != null) set(ValueLayout.JAVA_INT, 0L, v as Int) }
            Double::class -> arena.allocate(ValueLayout.JAVA_DOUBLE.byteSize(), 1L).apply { if (v != null) set(ValueLayout.JAVA_DOUBLE, 0L, v as Double) }
            else -> throw NotImplementedError("getPointerOf not implemented for type '${T::class}'")
        }
}
