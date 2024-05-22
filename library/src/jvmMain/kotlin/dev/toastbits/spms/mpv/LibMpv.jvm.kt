package dev.toastbits.spms.mpv

import dev.toastbits.spms.mpv.libmpv.*
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.foreign.Arena
import java.nio.charset.StandardCharsets

actual class MpvHandle(val data: MemorySegment)
actual class MpvEventData(val data: MemorySegment)

class InternalMpvEvent(private val event_data: MemorySegment): MpvEvent() {
    override val event_id: Int get() = mpv_event.event_id(event_data)
    override val error: Int get() = mpv_event.error(event_data)
    override val reply_userdata: Long get() = mpv_event.reply_userdata(event_data)
    override val data: MpvEventData? get() = MpvEventData(mpv_event.data(event_data))
}

fun Long.memorySegment(arena: Arena): MemorySegment {
    val item: MemorySegment = arena.allocate(ValueLayout.JAVA_LONG.byteSize(), 1L)
    item.set(ValueLayout.JAVA_LONG, 0L, this)
    return item
}
fun Int.memorySegment(arena: Arena): MemorySegment {
    val item: MemorySegment = arena.allocate(ValueLayout.JAVA_INT.byteSize(), 1L)
    item.set(ValueLayout.JAVA_INT, 0L, this)
    return item
}
fun String.memorySegment(arena: Arena): MemorySegment {
    val bytes: ByteArray = this.encodeToByteArray()
    val item: MemorySegment = arena.allocate(bytes.size.toLong() + 1, 1L)

    for (i in 0 until bytes.size) {
        item.set(ValueLayout.JAVA_BYTE, i.toLong(), bytes[i])
    }
    item.set(ValueLayout.JAVA_BYTE, bytes.size.toLong(), 0.toByte())

    return item
}
fun Array<String?>.memorySegment(arena: Arena): MemorySegment {
    val address_size: Long = ValueLayout.ADDRESS.byteSize()
    val array: MemorySegment = arena.allocate(this.size.toLong() * address_size, 1L)
    for ((index, string) in this.withIndex()) {
        if (string == null) {
            array.set(ValueLayout.ADDRESS, index * address_size, MemorySegment.NULL)
            continue
        }

        val item: MemorySegment = string.memorySegment(arena)
        array.set(ValueLayout.ADDRESS, index * address_size, item)
    }
    return array
}

private val string_array: ByteArray = ByteArray(1024)

fun MemorySegment.getString(): String? {
    if (address() == 0L) {
        return null
    }

    val finite: Boolean = byteSize() < Int.MAX_VALUE
    val size: Int = if (finite) byteSize().toInt().coerceAtMost(string_array.size) else string_array.size

    for (i in 0 until size) {
        val byte: Byte = get(ValueLayout.JAVA_BYTE, i.toLong())
        if (byte == 0.toByte()) {
            return string_array.decodeToString(endIndex = i)
        }

        string_array[i] = byte
    }

    if (finite) {
        return string_array.decodeToString(endIndex = size)
    }

    throw RuntimeException("String not terminated or array too short ${byteSize().toInt()} '${string_array.decodeToString()}'")
}

actual class LibMpv private constructor() {
    actual companion object {
        actual fun create(): LibMpv? {
            try {
                client_h.mpv_client_api_version()
            }
            catch (_: Throwable) {
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
        return MpvHandle(client_h.mpv_create_client(MemorySegment.NULL, MemorySegment.NULL))
    }

    actual fun terminateDestroy(handle: MpvHandle) {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_terminate_destroy(handle.data)
    }

    actual fun initialize(handle: MpvHandle): Int {
        return client_h.mpv_initialize(handle.data)
    }

    actual fun command(handle: MpvHandle, args: Array<String?>): Int {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_command(handle.data, args.memorySegment(arena))
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

        client_h.mpv_get_property(handle.data, name.memorySegment(arena), format, pointer)

        return extractor()
    }
    actual fun getPropertyString(handle: MpvHandle, name: String): String? {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_get_property_string(handle.data, name.memorySegment(arena)).getString()
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
        return client_h.mpv_set_property(handle.data, name.memorySegment(arena), format, pointer)
    }
    actual fun setPropertyString(handle: MpvHandle, name: String, data: String): Int {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_set_property_string(handle.data, name.memorySegment(arena), data.memorySegment(arena))
    }
    actual inline fun <reified T> setOption(handle: MpvHandle, name: String, data: T?): Int {
        val arena: Arena = Arena.global()
        val pointer: MemorySegment = getPointerOf(arena, data)
        val format: Int = getMpvFormatOf(T::class)
        return client_h.mpv_set_option(handle.data, name.memorySegment(arena), format, pointer)
    }
    actual fun setOptionString(handle: MpvHandle, name: String, data: String): Int {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_set_option_string(handle.data, name.memorySegment(arena), data.memorySegment(arena))
    }
    actual fun observeProperty(handle: MpvHandle, reply_userdata: Long, name: String, format: Int): Int {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_observe_property(handle.data, reply_userdata, name.memorySegment(arena), format)
    }
    actual fun waitEvent(handle: MpvHandle, timeout: Double): MpvEvent {
        val arena: Arena = Arena.ofConfined()
        return InternalMpvEvent(client_h.mpv_wait_event(handle.data, timeout))
    }
    actual fun requestLogMessages(handle: MpvHandle, min_level: String): Int {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_request_log_messages(handle.data, min_level.memorySegment(arena))
    }
    actual fun hookAdd(handle: MpvHandle, reply_userdata: Long, name: String, priority: Int): Int {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_hook_add(handle.data, reply_userdata, name.memorySegment(arena), priority)
    }
    actual fun hookContinue(handle: MpvHandle, id: Long): Int {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_hook_continue(handle.data, id)
    }

    inline fun <reified T> getPointerOf(arena: Arena, v: T? = null): MemorySegment =
        when (T::class) {
            Boolean::class -> arena.allocate(ValueLayout.JAVA_BOOLEAN.byteSize(), 1L).apply { if (v != null) set(ValueLayout.JAVA_BOOLEAN, 0L, v as Boolean) }
            Int::class -> arena.allocate(ValueLayout.JAVA_INT.byteSize(), 1L).apply { if (v != null) set(ValueLayout.JAVA_INT, 0L, v as Int) }
            Double::class -> arena.allocate(ValueLayout.JAVA_DOUBLE.byteSize(), 1L).apply { if (v != null) set(ValueLayout.JAVA_DOUBLE, 0L, v as Double) }
            else -> throw NotImplementedError("getPointerOf not implemented for type '${T::class}'")
        }
}
