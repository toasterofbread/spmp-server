package dev.toastbits.spms.mpv

import java.lang.foreign.*
import libmpv.*

class InternalMpvEvent(private val event_data: MemorySegment): MpvEvent() {
    override val event_id: Int get() = mpv_event.event_id(event_data)
    override val error: Int get() = mpv_event.error(event_data)
    override val reply_userdata: Long get() = mpv_event.reply_userdata(event_data)
    override val data: MpvEventData? get() = MpvEventData(mpv_event.data(event_data))
}

actual class MpvEventData(val data: MemorySegment)

actual class MpvEventStartFile actual constructor(private val event_data: MpvEventData) {
    actual val playlist_entry_id: Long get() = mpv_event_start_file.playlist_entry_id(event_data.data)
}

actual class MpvEventProperty actual constructor(private val event_data: MpvEventData) {
    actual val name: String? get() = mpv_event_property.name(event_data.data).getString()
    actual val format: Int get() = mpv_event_property.format(event_data.data)
    actual val data: MpvEventPropertyData?
        get() = mpv_event_property.data(event_data.data).takeIf { it.address() != 0L }?.let {
            object : MpvEventPropertyData {
                override fun toBoolean(): Boolean = it.get(ValueLayout.JAVA_BOOLEAN, 0)
            }
        }
}

actual class MpvEventHook actual constructor(private val event_data: MpvEventData) {
    actual val name: String? get() = mpv_event_hook.name(event_data.data).getString()
    actual val id: Long get() = mpv_event_hook.id(event_data.data)
}

actual class MpvEventLogMessage actual constructor(private val event_data: MpvEventData) {
    actual val prefix: String? get() = mpv_event_log_message.prefix(event_data.data).getString()
    actual val level: String? get() = mpv_event_log_message.level(event_data.data).getString()
    actual val text: String? get() = mpv_event_log_message.text(event_data.data).getString()
    actual val log_level: Int get() = mpv_event_log_message.log_level(event_data.data)
}
