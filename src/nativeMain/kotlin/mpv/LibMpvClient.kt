package mpv

import cnames.structs.mpv_handle as MpvHandle
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPrimitiveVar
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import libmpv.MPV_FORMAT_DOUBLE
import libmpv.MPV_FORMAT_FLAG
import libmpv.MPV_FORMAT_INT64
import libmpv.mpv_command
import libmpv.mpv_create_client
import libmpv.mpv_format as MpvFormat
import libmpv.mpv_get_property
import libmpv.mpv_get_property_string
import libmpv.mpv_initialize
import libmpv.mpv_set_option
import libmpv.mpv_set_property
import libmpv.mpv_terminate_destroy

@OptIn(ExperimentalForeignApi::class)
abstract class LibMpvClient: MpvClient {
    protected val ctx: CPointer<MpvHandle>

    init {
        ctx = mpv_create_client(null, null)
            ?: throw NullPointerException("Creating MPV client failed")

        memScoped {
            val pointer = alloc<BooleanVar>()
            mpv_set_option(ctx, "vid", MPV_FORMAT_FLAG, pointer.ptr)
        }

        check(mpv_initialize(ctx) == 0) { "Initialising MPV client failed" }
    }

    override fun release() {
        mpv_terminate_destroy(ctx)
    }

    private fun MemScope.buildArgs(args: List<Any?>, terminate: Boolean = true) =
        Array(args.size + terminate.toInt()) { i ->
            args.getOrNull(i)?.toString()?.cstr?.getPointer(this)
        }.toCValues()

    protected fun runCommand(name: String, vararg args: Any?, check_result: Boolean = true): Int =
        memScoped {
            val result = mpv_command(ctx, buildArgs(listOf(name).plus(args)))

            if (check_result) {
                check(result == 0) {
                    "Command $name(${args.toList()}) failed ($result)"
                }
            }

            return result
        }

    protected inline fun <reified V> getProperty(name: String): V =
        memScoped {
            if (V::class == String::class) {
                val string: CPointer<ByteVarOf<Byte>> = mpv_get_property_string(ctx, name)
                    ?: throw NullPointerException("Getting string property '$name' failed")
                return string.toKString() as V
            }

            val pointer: CPrimitiveVar
            val format: MpvFormat
            val extractor: () -> V

            when (V::class) {
                Boolean::class -> {
                    pointer = alloc<BooleanVar>()
                    format = MPV_FORMAT_FLAG
                    extractor = { pointer.value as V }
                }
                Int::class -> {
                    pointer = alloc<IntVar>()
                    format = MPV_FORMAT_INT64
                    extractor = { pointer.value as V }
                }
                Double::class -> {
                    pointer = alloc<DoubleVar>()
                    format = MPV_FORMAT_DOUBLE
                    extractor = { pointer.value as V }
                }
                else -> throw NotImplementedError(V::class.toString())
            }

            mpv_get_property(ctx, name, format, pointer.ptr)

            return extractor()
        }

    protected inline fun <reified T: Any> setProperty(name: String, value: T) = memScoped {
        val pointer: CPrimitiveVar = getPointerOf(value)
        val format: MpvFormat = getFormatOf(value)
        mpv_set_property(ctx, name, format, pointer.ptr)
    }

    protected inline fun <reified T> MemScope.getPointerOf(v: T? = null): CPrimitiveVar =
        when (T::class) {
            Boolean::class -> alloc<BooleanVar>().apply { if (v != null) value = v as Boolean }
            Int::class -> alloc<IntVar>().apply { if (v != null) value = v as Int }
            else -> throw NotImplementedError(T::class.toString())
        }

//    protected fun MemScope.getPointerOf(v: Any): CPrimitiveVar =
//        when (v) {
//            is Boolean -> alloc<BooleanVar>().apply { value = v }
//            is Int -> alloc<IntVar>().apply { value = v }
//            else -> throw NotImplementedError(v::class.toString())
//        }

    protected fun getFormatOf(v: Any): MpvFormat =
        when (v) {
            is Boolean -> MPV_FORMAT_FLAG
            is Int -> MPV_FORMAT_INT64
            else -> throw NotImplementedError(v::class.toString())
        }
}
