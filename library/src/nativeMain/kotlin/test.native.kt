// import kotlinx.cinterop.CPointer
// import kotlinx.cinterop.ExperimentalForeignApi
// import kotlinx.cinterop.reinterpret
// import kotlinx.cinterop.CPointed
// import kotlinx.cinterop.pointed
// import kotlinx.cinterop.convert
// import kotlinx.cinterop.ptr
// import kotlinx.cinterop.staticCFunction
// import kotlinx.cinterop.COpaquePointer
// import cnames.structs.mpv_handle as libmpv_handle
// import libmpv.mpv_event as libmpv_event
// import kotlin.reflect.KClass
// import kotlin.reflect.KCallable
// import kotlinx.cinterop.StableRef
// import kotlinx.cinterop.asStableRef

// actual class KJnaPointer(val pointer: CPointer<*>) {
//     actual inline fun <reified T: Any> cast(): T = TODO()
// }

// // actual abstract class KJnaTypedPointer<T: Any>(internal val pointer: CPointer<*>) {
// //     actual abstract fun get(): T
// // }

// inline fun <T: Any, reified R: CPointed> NativeKJnaTypedPointer(pointer: CPointer<R>, noinline construct: (R) -> T) =
//     object : KJnaTypedPointer<T> {
//         override fun get(): T {
//             val r: R = pointer.pointedAs()
//             return construct(r)
//         }
//     }

// actual class mpv_handle(internal val pointer: CPointer<libmpv_handle>)

// actual class mpv_event(internal val native_data: libmpv_event) {
//     actual var event_id: mpv_event_id
//         get() = mpv_event_id.entries.first { it.value == native_data.event_id.convert<Int>() }
//         set(value) { native_data.event_id = value.value.convert() }
//     actual var error: Int
//         get() = native_data.error
//         set(value) { native_data.error = value }
//     actual var reply_userdata: uint64_t
//         get() = native_data.reply_userdata.convert()
//         set(value) { native_data.reply_userdata = value.convert() }
//     actual var data: KJnaPointer?
//         get() = native_data.data?.let { KJnaPointer(it) }
//         set(value) { native_data.data = value?.pointer }

//     override fun toString(): String = "mpv_event(event_id=$event_id, error=$error, reply_userdata=$reply_userdata, data=$data)"
// }

// actual object testmpv {
//     actual fun mpv_terminate_destroy(ctx: mpv_handle?) {
//         return libmpv.mpv_terminate_destroy(ctx?.pointer)
//     }

//     actual fun mpv_create(): mpv_handle? {
//         return libmpv.mpv_create()?.let { mpv_handle(it) }
//     }

//     actual fun mpv_create_client(ctx: mpv_handle?, name: String?): mpv_handle? {
//         return libmpv.mpv_create_client(ctx?.pointer, name)?.let { mpv_handle(it) }
//     }

//     actual fun mpv_wait_event(ctx: mpv_handle?, timeout: Double): KJnaTypedPointer<mpv_event>? {
//         return libmpv.mpv_wait_event(ctx?.pointer, timeout)?.let { NativeKJnaTypedPointer(it) { mpv_event(it) } }
//     }

//     actual fun mpv_set_wakeup_callback(ctx: mpv_handle?, cb: (() -> Unit)?) {
//         if (cb == null) {
//             libmpv.mpv_set_wakeup_callback(ctx?.pointer, null, null)
//         }
//         else {
//             libmpv.mpv_set_wakeup_callback(
//                 ctx?.pointer,
//                 staticCFunction { d: COpaquePointer? ->
//                     val cb: () -> Unit = d!!.asStableRef<() -> Unit>().get()
//                     cb.invoke()
//                 },
//                 StableRef.create(cb).asCPointer()
//             )
//         }
//     }
// }

// @OptIn(ExperimentalForeignApi::class)
// inline fun <reified T: CPointed> CPointer<*>.pointedAs(): T =
//     this.reinterpret<T>().pointed
