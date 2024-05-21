package dev.toastbits.spms.mpv

// import com.sun.jna.Native
// import com.sun.jna.Pointer
// import com.sun.jna.Structure
// import com.sun.jna.Library
// import com.sun.jna.ptr.ByReference
// import com.sun.jna.ptr.IntByReference
// import com.sun.jna.ptr.DoubleByReference
import dev.toastbits.spms.mpv.client_h
import dev.toastbits.spms.mpv.mpv_event
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.foreign.Arena
import java.nio.charset.StandardCharsets

actual class MpvHandle(val data: MemorySegment)
actual class MpvEventData(val data: MemorySegment)

// private class JnaMpvEvent(private val event: InternalMpvEvent): MpvEvent {
//     override val event_id: Int get() = event.event_id
//     override val error: Int get() = event.error
//     override val reply_userdata: Long get() = event.reply_userdata
//     override val data: MpvEventData? get() = event.data
// }

class InternalMpvEvent(private val event_data: MemorySegment): MpvEvent {
    override val event_id: Int get() = mpv_event.event_id(event_data)
    override val error: Int get() = mpv_event.error(event_data)
    override val reply_userdata: Long get() = mpv_event.reply_userdata(event_data)
    override val data: MpvEventData? get() = MpvEventData(mpv_event.data(event_data))
}

// class InternalMpvEvent: Structure() {
//     @JvmField var event_id: Int = 0
//     @JvmField var error: Int = 0
//     @JvmField var reply_userdata: Long = 0
//     @JvmField var data: Pointer? = null

//     override fun getFieldOrder(): List<String> =
//         listOf("event_id", "error", "reply_userdata", "data")
// }

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
fun String.memorySegment(arena: Arena, extra_zero_hack: Boolean = false): MemorySegment {
    val bytes: ByteArray = if (extra_zero_hack) (this + '*').encodeToByteArray() else this.encodeToByteArray()
    val item: MemorySegment = arena.allocate(bytes.size.toLong(), 1L)

    for (i in 0 until (if (extra_zero_hack) (bytes.size - 1) else bytes.size)) {
        item.set(ValueLayout.JAVA_BYTE, i.toLong(), bytes[i])
    }
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

        // mpv reports receiving a string with an extra zero byte on the end
        // Yet when skipping the last byte of the string, mpv reports the last actual character missing (and no extra zero byte)
        // So the workaround is to add an extra unneeded character to the string and have that be dropped instead
        // No idea what's going on here, I might just be really tired
        val item: MemorySegment = string.memorySegment(arena, extra_zero_hack = true)
        array.set(ValueLayout.ADDRESS, index * address_size, item)
    }
    return array
}

fun MemorySegment.getString(): String? {
    if (address() == 0L) {
        return null
    }

    val finite: Boolean = byteSize() < Long.MAX_VALUE

    val bytes: ByteArray = ByteArray((if (finite) byteSize() else 256L).toInt())
    for (i in bytes.indices) {
        val byte: Byte = get(ValueLayout.JAVA_BYTE, i.toLong())
        if (byte == 0.toByte()) {
            return bytes.decodeToString(endIndex = i).also { println("STR $it") }
        }

        bytes[i] = byte
    }

    if (finite) {
        return bytes.decodeToString().also { println("STR $it") }
    }

    throw RuntimeException("String not terminated or array too short")
}

actual class LibMpv private constructor() {
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
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_initialize(handle.data)
    }

    actual fun command(handle: MpvHandle, args: Array<String?>): Int {
        val arena: Arena = Arena.ofConfined()
        return client_h.mpv_command(handle.data, args.memorySegment(arena))
    }

    actual inline fun <reified T> getProperty(handle: MpvHandle, name: String): T {
        val arena: Arena = Arena.global()

        if (T::class == String::class) {
            val string: String = client_h.mpv_get_property_string(handle.data, name.memorySegment(arena, extra_zero_hack = true)).getString().also { println("GETPROP RESULT $name '$it'") }
                ?: throw NullPointerException("Getting string property '$name' failed")
            return string as T
        }

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
        val arena: Arena = Arena.ofConfined()
        val pointer: MemorySegment = getPointerOf(arena, data)
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
    actual fun waitEvent(handle: MpvHandle, timeout: Double): MpvEvent? {
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
            else -> throw NotImplementedError(T::class.toString())
        }

    inline fun <reified T> MemorySegment.getValue(): T? =
        when (T::class) {
            Boolean::class -> this.get(ValueLayout.JAVA_BOOLEAN, 0L)
            Int::class -> this.get(ValueLayout.JAVA_INT, 0L)
            Double::class -> this.get(ValueLayout.JAVA_DOUBLE, 0L)
            else -> throw NotImplementedError(T::class.toString())
        } as T

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
}

// interface LibMpvInterface: Library {
//     companion object {
//         private var instance: LibMpvInterface? = null
//         private var created: Boolean = false
//         internal fun getInstance(): LibMpvInterface? {
//             if (!created) {
//                 try {
//                     instance = Native.load("libmpv.so", LibMpvInterface::class.java)
//                 }
//                 catch (_: Throwable) {}
//                 created = true
//             }
//             return instance
//         }
//     }

//     fun setlocale(category: Int, locale: String): String

//     fun mpv_create_client(ctx: MpvHandle?, name: String?): MpvHandle
//     fun mpv_terminate_destroy(handle: MpvHandle)
//     fun mpv_initialize(handle: MpvHandle): Long
//     fun mpv_command(handle: MpvHandle, args: Array<String?>): Int
//     fun mpv_get_property(handle: MpvHandle, name: String, format: Int, data: Pointer?): Int
//     fun mpv_get_property_string(handle: MpvHandle, name: String): String?
//     fun mpv_set_property(handle: MpvHandle, name: String, format: Int, data: Pointer?): Int
//     fun mpv_set_property_string(handle: MpvHandle, name: String, data: String): Int
//     fun mpv_set_option(handle: MpvHandle, name: String, format: Int, data: Pointer?): Int
//     fun mpv_set_option_string(handle: MpvHandle, name: String, data: String): Int
//     fun mpv_observe_property(handle: MpvHandle, reply_userdata: Long, name: String, format: Int): Int
//     fun mpv_wait_event(handle: MpvHandle, timeOut: Double): InternalMpvEvent
//     fun mpv_request_log_messages(handle: MpvHandle, min_level: String): Int
//     fun mpv_hook_add(handle: MpvHandle, reply_userdata: Long, name: String, priority: Int): Int
//     fun mpv_hook_continue(handle: MpvHandle, id: Long): Int
// }


