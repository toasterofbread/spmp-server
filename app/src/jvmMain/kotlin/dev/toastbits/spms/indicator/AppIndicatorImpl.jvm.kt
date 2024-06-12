package dev.toastbits.spms.indicator

import dev.toastbits.kjna.runtime.KJnaPointer
import dev.toastbits.kjna.runtime.KJnaFunctionPointer
import kjna.enum.GLogWriterOutput
import kjna.enum.GLogLevelFlags
import kjna.enum.fromJvm
import java.lang.foreign.MemorySegment

class ScrollEventFunction(val onScroll: (delta: Int, direction: Int) -> Unit) {
    fun invoke(p0: MemorySegment, delta: Int, direction: Int, function: MemorySegment) {
        onScroll(delta, if (direction == 1) 1 else -1)
    }
}

class LogWriterFunction(val handleMessage: (GLogLevelFlags) -> GLogWriterOutput) {
    fun invoke(level: Int, p1: MemorySegment, p2: Int, function: MemorySegment): Int {
        return handleMessage(GLogLevelFlags.fromJvm(level)).value
    }
}

actual fun getScrollEventFunction(onScroll: (delta: Int, direction: Int) -> Unit): Pair<KJnaFunctionPointer, KJnaPointer?> =
    Pair(
        KJnaFunctionPointer.bindObjectMethod(ScrollEventFunction(onScroll), "invoke", Unit::class, listOf(MemorySegment::class, Int::class, Int::class, MemorySegment::class)),
        null
    )

actual fun getLogWriterFunction(handleMessage: (GLogLevelFlags) -> GLogWriterOutput): Pair<KJnaFunctionPointer, KJnaPointer?> =
    Pair(
        KJnaFunctionPointer.bindObjectMethod(LogWriterFunction(handleMessage), "invoke", Int::class, listOf(Int::class, MemorySegment::class, Int::class, MemorySegment::class)),
        null
    )
