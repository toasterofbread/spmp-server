// package dev.toastbits.spms.mpv

// import kotlinx.coroutines.*
// import kotlinx.serialization.json.JsonPrimitive
// import dev.toastbits.spms.server.SpMs
// import dev.toastbits.spms.socketapi.shared.SpMsPlayerEvent
// import com.sun.jna.Pointer
// import com.sun.jna.Structure

// actual internal suspend fun MpvClientImpl.eventLoop() = withContext(Dispatchers.IO) {
//     observeProperty("core-idle", Boolean::class)
//     observeProperty("seeking", Boolean::class)

//     var waiting_for_seek_end: Boolean = false

//     while (true) {
//         val event: MpvEvent? = waitForEvent()

//         when (event?.event_id) {
//             LibMpv.MPV_EVENT_START_FILE -> {
//                 val data: mpv_event_start_file = mpv_event_start_file(event.data)
//                 if (data.playlist_entry_id.toInt() != current_item_playlist_id) {
//                     continue
//                 }

//                 onEvent(SpMsPlayerEvent.ItemTransition(current_item_index), clientless = true)
//             }
//             LibMpv.MPV_EVENT_PLAYBACK_RESTART -> {
//                 onEvent(SpMsPlayerEvent.PropertyChanged("state", JsonPrimitive(state.ordinal)), clientless = true)
//                 onEvent(SpMsPlayerEvent.PropertyChanged("duration_ms", JsonPrimitive(duration_ms)), clientless = true)
//                 onEvent(SpMsPlayerEvent.SeekedToTime(current_position_ms), clientless = true)
//             }
//             LibMpv.MPV_EVENT_END_FILE -> {
//                 onEvent(SpMsPlayerEvent.PropertyChanged("state", JsonPrimitive(state.ordinal)), clientless = true)
//             }
//             LibMpv.MPV_EVENT_FILE_LOADED -> {
//                 onEvent(SpMsPlayerEvent.ReadyToPlay(), clientless = true)

//                 song_initial_seek_time?.also { time ->
//                     seekToTime(time.elapsedNow().inWholeMilliseconds)
//                     song_initial_seek_time = null
//                 }
//             }
//             LibMpv.MPV_EVENT_SHUTDOWN -> {
//                 onShutdown()
//             }
//             LibMpv.MPV_EVENT_PROPERTY_CHANGE -> {
//                 val data: mpv_event_property = mpv_event_property(event.data)

//                 when (data.name) {
//                     "core-idle" -> {
//                         val playing: Boolean = !data.data!!.toBoolean()
//                         onEvent(SpMsPlayerEvent.PropertyChanged("is_playing", JsonPrimitive(playing)), clientless = true)
//                     }
//                     "seeking" -> {
//                         if (waiting_for_seek_end && !getProperty<Boolean>("seeking")) {
//                             waiting_for_seek_end = false
//                             onEvent(SpMsPlayerEvent.ReadyToPlay(), clientless = true)
//                         }
//                     }
//                 }
//             }

//             LibMpv.MPV_EVENT_SEEK -> {
//                 if (getProperty<Boolean>("seeking")) {
//                     waiting_for_seek_end = true
//                 }
//                 else {
//                     onEvent(SpMsPlayerEvent.ReadyToPlay(), clientless = true)
//                 }
//             }

//             LibMpv.MPV_EVENT_HOOK -> {
//                 val data: mpv_event_hook = mpv_event_hook(event.data)
//                 launch {
//                     onMpvHook(data.name, data.id.toULong())
//                 }
//             }

//             LibMpv.MPV_EVENT_LOG_MESSAGE -> {
//                 if (SpMs.logging_enabled) {
//                     val message: mpv_event_log_message = mpv_event_log_message(event.data)
//                     SpMs.log("From mpv (${message.prefix}): ${message.text}")
//                 }
//             }
//         }
//     }
// }
