package dev.toastbits.spms.mpv

import cnames.structs.mpv_handle
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPrimitiveVar
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.cinterop.pointed
import kotlinx.cinterop.MemScope
import libmpv.*

actual class MpvHandle(val pointer: CPointer<mpv_handle>)

actual class LibMpv private constructor() {
    actual fun create(): MpvHandle? {
        return mpv_create_client(null, null)?.let { MpvHandle(it) }
    }

    actual fun terminateDestroy(handle: MpvHandle) {
        return mpv_terminate_destroy(handle.pointer)
    }

    actual fun initialize(handle: MpvHandle): Int {
        return mpv_initialize(handle.pointer)
    }

    actual fun command(handle: MpvHandle, args: Array<String?>): Int = memScoped {
        return mpv_command(handle.pointer, args.map { it?.cstr?.getPointer(this) }.toCValues())
    }

    actual inline fun <reified T> getProperty(handle: MpvHandle, name: String): T = memScoped {
        if (T::class == String::class) {
            return getPropertyString(handle, name) as T
        }

        val pointer: CPrimitiveVar
        val format: Int
        val extractor: () -> T

        when (T::class) {
            Boolean::class -> {
                pointer = alloc<BooleanVar>()
                format = MPV_FORMAT_FLAG
                extractor = { pointer.value as T }
            }
            Int::class -> {
                pointer = alloc<IntVar>()
                format = MPV_FORMAT_INT64
                extractor = { pointer.value as T }
            }
            Double::class -> {
                pointer = alloc<DoubleVar>()
                format = MPV_FORMAT_DOUBLE
                extractor = { pointer.value as T }
            }
            else -> throw NotImplementedError(T::class.toString())
        }

        mpv_get_property(handle.pointer, name, format.toUInt(), pointer.ptr)

        return extractor()
    }
    actual fun getPropertyString(handle: MpvHandle, name: String): String? {
        return mpv_get_property_string(handle.pointer, name)?.toKString()
    }
    actual inline fun <reified T> setProperty(handle: MpvHandle, name: String, data: T?): Int {
        if (T::class == String::class) {
            return setPropertyString(handle, name, data as String)
        }

        memScoped {

            val pointer: CPrimitiveVar = getPointerOf(data)
            val format: mpv_format = getMpvFormatOf(T::class).toUInt()
            return mpv_set_property(handle.pointer, name, format, pointer.ptr)
        }
    }
    actual fun setPropertyString(handle: MpvHandle, name: String, data: String): Int {
        return mpv_set_property_string(handle.pointer, name, data)
    }
    actual inline fun <reified T> setOption(handle: MpvHandle, name: String, data: T?): Int = memScoped {
        val pointer: CPrimitiveVar = getPointerOf(data)
        val format: mpv_format = getMpvFormatOf(T::class).toUInt()
        return mpv_set_option(handle.pointer, name, format, pointer.ptr)
    }
    actual fun setOptionString(handle: MpvHandle, name: String, data: String): Int {
        return mpv_set_option_string(handle.pointer, name, data)
    }
    actual fun observeProperty(handle: MpvHandle, reply_userdata: Long, name: String, format: Int): Int {
        return mpv_observe_property(handle.pointer, reply_userdata.toULong(), name, format.toUInt())
    }
    actual fun waitEvent(handle: MpvHandle, timeout: Double): MpvEvent {
        return InternalMpvEvent(mpv_wait_event(handle.pointer, timeout)!!.pointed)
    }
    actual fun requestLogMessages(handle: MpvHandle, min_level: String): Int {
        return mpv_request_log_messages(handle.pointer, min_level)
    }
    actual fun hookAdd(handle: MpvHandle, reply_userdata: Long, name: String, priority: Int): Int {
        return mpv_hook_add(handle.pointer, reply_userdata.toULong(), name, priority)
    }
    actual fun hookContinue(handle: MpvHandle, id: Long): Int {
        return mpv_hook_continue(handle.pointer, id.toULong())
    }

    inline fun <reified T> MemScope.getPointerOf(v: T? = null): CPrimitiveVar =
        when (T::class) {
            Boolean::class -> alloc<BooleanVar>().apply { if (v != null) value = v as Boolean }
            Int::class -> alloc<IntVar>().apply { if (v != null) value = v as Int }
            Double::class -> alloc<DoubleVar>().apply { if (v != null) value = v as Double }
            else -> throw NotImplementedError(T::class.toString())
        }

    actual companion object {
        actual fun create(): LibMpv? = LibMpv()
    }
}
