// package dev.toastbits.spms.mpv

// import dev.toastbits.spms.player.Player
// import dev.toastbits.spms.player.toInt
// import kotlin.reflect.KClass
// import com.sun.jna.ptr.ByReference
// import com.sun.jna.ptr.IntByReference
// import com.sun.jna.ptr.DoubleByReference

// actual abstract class LibMpvClient actual constructor(
//     headless: Boolean,
//     playlist_auto_progress: Boolean
// ): Player {
//     actual companion object {
//         actual fun isAvailable(): Boolean = true
//     }

//     actual val is_headless: Boolean = headless

//     val library: LibMpv = LibMpv.getInstance()
//     val ctx: MpvHandle

//     init {
//         ctx = library.mpv_create_client(null, null)
//         check(ctx != 0L) { "Creating mpv client failed" }

//         val vid: BooleanByReference = BooleanByReference()
//         vid.value = !is_headless
//         library.mpv_set_option(ctx, "vid", LibMpv.MPV_FORMAT_FLAG, vid.pointer)

//         if (!is_headless) {
//             library.mpv_set_option_string(ctx, "force-window", "immediate")

//             val osd_level: IntByReference = IntByReference()
//             osd_level.value = 3
//             library.mpv_set_option(ctx, "osd-level", LibMpv.MPV_FORMAT_INT64, osd_level.pointer)
//         }

//         if (!playlist_auto_progress) {
//             library.mpv_set_option_string(ctx, "keep-open", "always")
//         }

//         val init_result: Long = library.mpv_initialize(ctx)
//         check(init_result == 0L) { "Initialising mpv client failed ($init_result)" }
//     }

//     actual override fun release() {
//         library.mpv_terminate_destroy(ctx)
//     }

//     private fun buildArgs(args: List<Any?>, terminate: Boolean = true): Array<String?> =
//         Array(args.size + terminate.toInt()) { i ->
//             args.getOrNull(i)?.toString()
//         }

//     actual fun runCommand(name: String, vararg args: Any?, check_result: Boolean): Int {
//         val result: Int = library.mpv_command(ctx, buildArgs(listOf(name).plus(args)))

//         if (check_result) {
//             check(result == 0) {
//                 "Command $name(${args.toList()}) failed ($result)"
//             }
//         }

//         return result
//     }

//     // actual inline fun <reified V> getProperty(name: String): V = librar

//     actual inline fun <reified T: Any> setProperty(name: String, value: T) {
//         if (value is String) {
//             library.mpv_set_property_string(ctx, name, value)
//             return
//         }

//         val pointer: ByReference = getPointerOf(value)
//         val format: Int = getFormatOf(T::class)
//         library.mpv_set_property(ctx, name, format, pointer.pointer)
//     }

//     internal fun observeProperty(name: String, cls: KClass<*>) {
//         val format: Int = getFormatOf(cls)

//         try {
//             library.mpv_observe_property(ctx, 0L, name, format)
//         }
//         catch (e: Throwable) {
//             throw RuntimeException("Call to library.mpv_observe_property for $name with format $format failed", e)
//         }
//     }

//     fun getFormatOf(cls: KClass<*>): Int =
//         when (cls) {
//             Boolean::class -> LibMpv.MPV_FORMAT_FLAG
//             Int::class -> LibMpv.MPV_FORMAT_INT64
//             Double::class -> LibMpv.MPV_FORMAT_DOUBLE
//             else -> throw NotImplementedError(cls.toString())
//         }

//     internal fun waitForEvent(): MpvEvent? =
//         library.mpv_wait_event(ctx, -1.0)

//     internal fun requestLogMessages() {
//         library.mpv_request_log_messages(ctx, "stats")
//     }

//     actual fun addHook(name: String, priority: Int) {
//         library.mpv_hook_add(ctx, 0L, name, priority)
//     }

//     actual fun continueHook(id: ULong) {
//         library.mpv_hook_continue(ctx, id.toLong())
//     }
// }
