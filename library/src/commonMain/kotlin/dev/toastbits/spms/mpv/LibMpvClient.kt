package dev.toastbits.spms.mpv

import dev.toastbits.spms.player.Player
import toInt
import kotlin.reflect.KClass

abstract class LibMpvClient(
    val libmpv: LibMpv,
    headless: Boolean,
    playlist_auto_progress: Boolean
): Player {
    val is_headless: Boolean = headless

    protected val ctx: MpvHandle

    init {
        ctx = libmpv.create()
            ?: throw NullPointerException("Creating mpv client failed")

        libmpv.setOption(ctx, "vid", !is_headless)

        if (!is_headless) {
            libmpv.setOptionString(ctx, "force-window", "immediate")
            libmpv.setOption(ctx, "osd-level", 3)
        }

        if (!playlist_auto_progress) {
            libmpv.setOptionString(ctx, "keep-open", "always")
        }

        val init_result: Int = libmpv.initialize(ctx)
        check(init_result == 0) { "Initialising mpv client failed ($init_result)" }
    }

    override fun release() {
        libmpv.terminateDestroy(ctx)
    }

    private fun buildArgs(args: List<Any?>, terminate: Boolean = true): Array<String?> =
        Array(args.size + terminate.toInt()) { i ->
            args.getOrNull(i)?.toString()
        }

    fun runCommand(name: String, vararg args: Any?, check_result: Boolean = true): Int {
        val result: Int = libmpv.command(ctx, buildArgs(listOf(name).plus(args)))

        if (check_result) {
            check(result == 0) {
                "Command $name(${args.toList()}) failed ($result)"
            }
        }

        return result
    }

    internal inline fun <reified T> getProperty(name: String): T = libmpv.getProperty(ctx, name)

    internal inline fun <reified T: Any> setProperty(name: String, value: T) = libmpv.setProperty(ctx, name, value)

    internal fun observeProperty(name: String, cls: KClass<*>) {
        val format: Int = getMpvFormatOf(cls)

        try {
            val res: Int = libmpv.observeProperty(ctx, 0L, name, format)
            check(res == 0) { res }
        }
        catch (e: Throwable) {
            throw RuntimeException("Call to observeProperty for $name with format $format failed", e)
        }
    }

    internal fun waitForEvent(): MpvEvent = libmpv.waitEvent(ctx, -1.0)

    internal fun requestLogMessages() {
        val result: Int = libmpv.requestLogMessages(ctx, "info")
        check(result == 0) { "Call to requestLogMessages failed ($result)" }
    }

    fun addHook(name: String, priority: Int = 0) {
        val result: Int = libmpv.hookAdd(ctx, 0L, name, priority)
        check(result == 0) { "Call to hookAdd with name=$name and priority=$priority failed ($result)" }
    }

    fun continueHook(id: Long) {
        libmpv.hookContinue(ctx, id)
    }
}
