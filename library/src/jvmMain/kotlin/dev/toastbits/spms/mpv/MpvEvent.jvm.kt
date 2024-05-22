package dev.toastbits.spms.mpv

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import dev.toastbits.spms.mpv.libmpv.*

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

// class mpv_event_start_file(data: MpvEventData?): Structure(data!!.data) {
//     @JvmField var playlist_entry_id: Long = -1
//     override fun getFieldOrder(): List<String> = listOf("playlist_entry_id")
// }

// class mpv_event_property(data: MpvEventData?): Structure(data!!.data) {
//     @JvmField var name: String? = null
//     @JvmField var format: Int = -1
//     @JvmField var data: MpvEventData? = null
//     override fun getFieldOrder(): List<String> = listOf("name", "format", "data")
// }

// class mpv_event_hook(data: MpvEventData?): Structure(data!!.data) {
//     @JvmField val name: String? = null
//     @JvmField val id: Long = 0
//     override fun getFieldOrder(): List<String> = listOf("name", "id")
// }

// class mpv_event_log_message(data: MpvEventData?): Structure(data!!.data) {
//     @JvmField var prefix: MpvEventData? = null
//     @JvmField var level: MpvEventData? = null
//     @JvmField var text: MpvEventData? = null
//     @JvmField var log_level: MpvEventData? = null
//     override fun getFieldOrder(): List<String> = listOf("prefix", "level", "text", "log_level")
// }
