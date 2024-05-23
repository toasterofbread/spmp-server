package dev.toastbits.spms.mpv

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.foreign.Arena

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

fun MemorySegment.getString(): String? = synchronized(string_array) {
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
