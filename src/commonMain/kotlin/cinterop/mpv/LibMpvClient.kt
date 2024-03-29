package cinterop.mpv

import kotlinx.cinterop.*
import libmpv.*
import spms.player.Player
import spms.player.toInt
import kotlin.reflect.KClass
import cnames.structs.mpv_handle as MpvHandle
import libmpv.mpv_format as MpvFormat

@OptIn(ExperimentalForeignApi::class)
abstract class LibMpvClient(val headless: Boolean = true): Player {
    protected val ctx: CPointer<MpvHandle>

    init {
        ctx = mpv_create_client(null, null)
            ?: throw NullPointerException("Creating MPV client failed")

        memScoped {
            val vid: BooleanVar = alloc()
            vid.value = !headless
            mpv_set_option(ctx, "vid", MPV_FORMAT_FLAG, vid.ptr)

            if (!headless) {
                mpv_set_option_string(ctx, "force-window", "immediate")

                val osd_level: IntVar = alloc()
                osd_level.value = 3
                mpv_set_option(ctx, "osd-level", MPV_FORMAT_INT64, osd_level.ptr)
            }
        }

        val init_result: Int = mpv_initialize(ctx)
        check(init_result == 0) { "Initialising MPV client failed ($init_result)" }
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
            val result: Int = mpv_command(ctx, buildArgs(listOf(name).plus(args)))

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
        if (value is String) {
            mpv_set_property_string(ctx, name, value)
            return@memScoped
        }

        val pointer: CPrimitiveVar = getPointerOf(value)
        val format: MpvFormat = getFormatOf(T::class)
        mpv_set_property(ctx, name, format, pointer.ptr)
    }

    protected inline fun <reified T: Any> observeProperty(name: String) {
        val format: MpvFormat = getFormatOf(T::class)
        mpv_observe_property(ctx, 0UL, name, format)
    }

    protected inline fun <reified T> MemScope.getPointerOf(v: T? = null): CPrimitiveVar =
        when (T::class) {
            Boolean::class -> alloc<BooleanVar>().apply { if (v != null) value = v as Boolean }
            Int::class -> alloc<IntVar>().apply { if (v != null) value = v as Int }
            Double::class -> alloc<DoubleVar>().apply { if (v != null) value = v as Double }
            else -> throw NotImplementedError(T::class.toString())
        }

    protected fun getFormatOf(cls: KClass<*>): MpvFormat =
        when (cls) {
            Boolean::class -> MPV_FORMAT_FLAG
            Int::class -> MPV_FORMAT_INT64
            Double::class -> MPV_FORMAT_DOUBLE
            else -> throw NotImplementedError(cls.toString())
        }

    protected fun waitForEvent(): mpv_event? =
        mpv_wait_event(ctx, -1.0)?.pointed

    protected fun requestLogMessages() {
        mpv_request_log_messages(ctx, "stats")
    }

    protected fun addHook(name: String, priority: Int = 0) {
        mpv_hook_add(ctx, 0UL, name, priority)
    }

    protected fun continueHook(id: ULong) {
        mpv_hook_continue(ctx, id)
    }
}
