
// // ---
// expect class mpv_handle
// typealias uint64_t = Long

// enum class mpv_event_id(val value: Int) {
//     MPV_EVENT_NONE(0),
//     MPV_EVENT_SHUTDOWN(1),
//     MPV_EVENT_LOG_MESSAGE(2),
//     MPV_EVENT_GET_PROPERTY_REPLY(3),
//     MPV_EVENT_SET_PROPERTY_REPLY(4),
//     MPV_EVENT_COMMAND_REPLY(5),
//     MPV_EVENT_START_FILE(6),
//     MPV_EVENT_END_FILE(7),
//     MPV_EVENT_FILE_LOADED(8),
//     MPV_EVENT_CLIENT_MESSAGE(16),
//     MPV_EVENT_VIDEO_RECONFIG(17),
//     MPV_EVENT_AUDIO_RECONFIG(18),
//     MPV_EVENT_SEEK(20),
//     MPV_EVENT_PLAYBACK_RESTART(21),
//     MPV_EVENT_PROPERTY_CHANGE(22),
//     MPV_EVENT_QUEUE_OVERFLOW(24),
//     MPV_EVENT_HOOK(25)
// }
// // ---

// expect class KJnaPointer {
//     inline fun <reified T: Any> cast(): T
// }

// interface KJnaTypedPointer<T: Any> {
//     fun get(): T
// }

// expect class mpv_event {
//     var event_id: mpv_event_id
//     var error: Int
//     var reply_userdata: uint64_t
//     var data: KJnaPointer?
// }

// expect object testmpv {
//     fun mpv_terminate_destroy(ctx: mpv_handle?)

//     fun mpv_create(): mpv_handle?

//     fun mpv_create_client(ctx: mpv_handle?, name: String?): mpv_handle?

//     fun mpv_wait_event(ctx: mpv_handle?, timeout: Double): KJnaTypedPointer<mpv_event>?

//     fun mpv_set_wakeup_callback(ctx: mpv_handle?, cb: (() -> Unit)?)
// }
