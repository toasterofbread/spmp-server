package dev.toastbits.spms.mpv

import dev.toastbits.spms.player.Player
import toInt
import kotlin.reflect.KClass
import gen.libmpv.LibMpv
import kjna.struct.mpv_handle
import kjna.struct.mpv_event
import kjna.enum.mpv_format
import dev.toastbits.kjna.runtime.KJnaTypedPointer
import dev.toastbits.kjna.runtime.KJnaMemScope

abstract class LibMpvClient(
    val libmpv: LibMpv,
    headless: Boolean,
    playlist_auto_progress: Boolean
): Player {
    val is_headless: Boolean = headless

    protected val ctx: KJnaTypedPointer<mpv_handle>

    init {
        ctx = libmpv.mpv_create()
            ?: throw NullPointerException("Creating mpv client failed")

        KJnaMemScope.confined {
            val vid: KJnaTypedPointer<Boolean> = alloc<Boolean>()
            vid.set(!is_headless)
            libmpv.mpv_set_option(ctx, "vid", mpv_format.MPV_FORMAT_FLAG, vid)

            if (!is_headless) {
                libmpv.mpv_set_option_string(ctx, "force-window", "immediate")

                val osd_level: KJnaTypedPointer<Int> = alloc<Int>()
                osd_level.set(3)
                libmpv.mpv_set_option(ctx, "osd-level", mpv_format.MPV_FORMAT_INT64, osd_level)
            }

            if (!playlist_auto_progress) {
                libmpv.mpv_set_option_string(ctx, "keep-open", "always")
            }
        }

        val init_result: Int = libmpv.mpv_initialize(ctx)
        check(init_result == 0) { "Initialising mpv client failed ($init_result)" }
    }

    override fun release() {
        libmpv.mpv_terminate_destroy(ctx)
    }

    private fun buildArgs(args: List<Any?>, terminate: Boolean = true): Array<String?> =
        Array(args.size + terminate.toInt()) { i ->
            args.getOrNull(i)?.toString()
        }

    fun runCommand(name: String, vararg args: Any?, check_result: Boolean = true): Int {
        val result: Int = KJnaMemScope.confined {
            libmpv.mpv_command(ctx, allocStringArray(buildArgs(listOf(name).plus(args))))
        }

        if (check_result) {
            check(result == 0) {
                "Command $name(${args.toList()}) failed ($result)"
            }
        }

        return result
    }

    internal inline fun <reified T: Any> getProperty(name: String): T = KJnaMemScope.confined {
        if (T::class == String::class) {
            return libmpv.mpv_get_property_string(ctx, name)!! as T
        }

        val pointer: KJnaTypedPointer<T> = alloc<T>()
        val format: mpv_format = getMpvFormatOf(T::class)
        libmpv.mpv_get_property(ctx, name, format, pointer)
        return@confined pointer.get()
    }

    internal inline fun <reified T: Any> setProperty(name: String, value: T): Int = KJnaMemScope.confined {
        if (T::class == String::class) {
            return@confined libmpv.mpv_set_property_string(ctx, name, value as String)
        }

        val pointer: KJnaTypedPointer<T> = alloc<T>()
        val format: mpv_format = getMpvFormatOf(T::class)
        pointer.set(value)
        libmpv.mpv_set_property(ctx, name, format, pointer)
    }

    internal fun observeProperty(name: String, cls: KClass<*>) {
        val format: mpv_format = getMpvFormatOf(cls)

        try {
            val res: Int = libmpv.mpv_observe_property(ctx, 0UL, name, format)
            check(res == 0) { res }
        }
        catch (e: Throwable) {
            throw RuntimeException("Call to observeProperty for $name with format $format failed", e)
        }
    }

    internal fun waitForEvent(): mpv_event = libmpv.mpv_wait_event(ctx, -1.0)!!.get()

    internal fun requestLogMessages() {
        val result: Int = libmpv.mpv_request_log_messages(ctx, "info")
        check(result == 0) { "Call to requestLogMessages failed ($result)" }
    }

    fun addHook(name: String, priority: Int = 0) {
        val result: Int = libmpv.mpv_hook_add(ctx, 0UL, name, priority)
        check(result == 0) { "Call to hookAdd with name=$name and priority=$priority failed ($result)" }
    }

    fun continueHook(id: ULong) {
        libmpv.mpv_hook_continue(ctx, id)
    }
}

fun getMpvFormatOf(cls: KClass<*>): mpv_format =
    when (cls) {
        Boolean::class -> mpv_format.MPV_FORMAT_FLAG
        Int::class -> mpv_format.MPV_FORMAT_INT64
        Double::class -> mpv_format.MPV_FORMAT_DOUBLE
        String::class -> mpv_format.MPV_FORMAT_STRING
        else -> throw NotImplementedError(cls.toString())
    }
