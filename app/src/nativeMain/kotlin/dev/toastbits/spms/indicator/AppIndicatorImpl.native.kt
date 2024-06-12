@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package dev.toastbits.spms.indicator

import dev.toastbits.kjna.runtime.KJnaPointer
import dev.toastbits.kjna.runtime.KJnaFunctionPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import kjna.enum.GLogWriterOutput
import kjna.enum.GLogLevelFlags
import kjna.enum.fromNative
import gen.libappindicator.cinterop.gint
import gen.libappindicator.cinterop.guint
import gen.libappindicator.cinterop.gsize

actual fun getScrollEventFunction(onScroll: (delta: Int, direction: Int) -> Unit): Pair<KJnaFunctionPointer, KJnaPointer?> =
    Pair(
        KJnaFunctionPointer(
            staticCFunction { _: CPointer<*>, delta: gint, direction: guint, function: COpaquePointer ->
                function.asStableRef<(Int, Int) -> Unit>().get().invoke(delta, if (direction == 1U) 1 else -1)
            }.reinterpret()
        ),
        KJnaPointer(StableRef.create(onScroll).asCPointer()),
    )

actual fun getLogWriterFunction(handleMessage: (GLogLevelFlags) -> GLogWriterOutput): Pair<KJnaFunctionPointer, KJnaPointer?> =
    Pair(
        KJnaFunctionPointer(
            staticCFunction { level: UInt, _: CPointer<*>, _: gsize, function: COpaquePointer ->
                function.asStableRef<(GLogLevelFlags) -> GLogWriterOutput>().get().invoke(GLogLevelFlags.fromNative(level)).value
            }.reinterpret()
        ),
        KJnaPointer(StableRef.create(handleMessage).asCPointer()),
    )
