// import java.lang.foreign.MemorySegment
// import java.lang.foreign.ValueLayout
// import java.lang.foreign.Arena
// import java.lang.foreign.Linker
// import java.lang.foreign.FunctionDescriptor
// import java.lang.invoke.MethodHandles
// import java.lang.invoke.MethodType
// import libmpv.mpv_event as internal_mpv_event
// import dev.toastbits.spms.mpv.*

// actual class KJnaPointer(val pointer: MemorySegment) {
//     actual inline fun <reified T: Any> cast(): T = TODO()//pointer.pointedAs()
// }

// fun <T: Any> NativeKJnaTypedPointer(pointer: MemorySegment, construct: (MemorySegment) -> T) =
//     object : KJnaTypedPointer<T> {
//         override fun get(): T {
//             return construct(pointer)
//         }
//     }

// actual class mpv_handle(internal val pointer: MemorySegment)

// actual class mpv_event(internal val native_data: MemorySegment) {
//     actual var event_id: mpv_event_id
//         get() = mpv_event_id.entries.first { it.value == internal_mpv_event.event_id(native_data) }
//         set(value) { internal_mpv_event.event_id(native_data, value.value) }
//     actual var error: Int
//         get() = internal_mpv_event.error(native_data)
//         set(value) { internal_mpv_event.error(native_data, value) }
//     actual var reply_userdata: uint64_t
//         get() = internal_mpv_event.reply_userdata(native_data)
//         set(value) { internal_mpv_event.reply_userdata(native_data, value) }
//     actual var data: KJnaPointer?
//         get() = internal_mpv_event.data(native_data)?.let { KJnaPointer(it) }
//         set(value) { internal_mpv_event.data(native_data, value?.pointer) }

//     override fun toString(): String = "mpv_event(event_id=$event_id, error=$error, reply_userdata=$reply_userdata, data=$data)"
// }

// actual object testmpv {
//     actual fun mpv_terminate_destroy(ctx: mpv_handle?) {
//         return libmpv.client.mpv_terminate_destroy(ctx?.pointer)
//     }

//     actual fun mpv_create(): mpv_handle? {
//         dev.toastbits.spms.mpv.setlocale(
//             1, // LC_NUMERIC
//             "C"
//         )

//         return libmpv.client.mpv_create()?.let { mpv_handle(it) }
//     }

//     actual fun mpv_create_client(ctx: mpv_handle?, name: String?): mpv_handle? {
//         val arena: Arena = Arena.ofConfined()
//         return libmpv.client.mpv_create_client(ctx?.pointer, name?.memorySegment(arena))?.let { mpv_handle(it) }
//     }

//     actual fun mpv_wait_event(ctx: mpv_handle?, timeout: Double): KJnaTypedPointer<mpv_event>? {
//         return libmpv.client.mpv_wait_event(ctx?.pointer, timeout)?.let { NativeKJnaTypedPointer(it) { mpv_event(it) } }
//     }

//     actual fun mpv_set_wakeup_callback(ctx: mpv_handle?, cb: (() -> Unit)?) {
//         if (cb == null) {
//             libmpv.client.mpv_set_wakeup_callback(ctx?.pointer, null, null)
//             return
//         }

//         val linker: Linker = Linker.nativeLinker()

//         val cb_handle = MethodHandles.lookup().bind(Callback(cb), "invoke", MethodType.methodType(Void.TYPE))
//         val cb_desc = FunctionDescriptor.ofVoid()
//         val cb_func = linker.upcallStub(cb_handle, cb_desc, CallbackData.callback_arena)

//         libmpv.client.mpv_set_wakeup_callback(
//             ctx?.pointer,
//             cb_func,
//             cb_func
//         )
//     }

//     class Callback(val cb: () -> Unit) {
//         fun invoke(): Unit = cb()
//     }
// }

// object CallbackData {
//     val callback_arena: Arena = Arena.ofAuto()

//     @JvmStatic
//     fun forwardCallback(callback: MemorySegment) {
//         println("forwardCallback called")
//     }

//     fun testFunc() {
//         println("testFunc called")
//     }
// }
