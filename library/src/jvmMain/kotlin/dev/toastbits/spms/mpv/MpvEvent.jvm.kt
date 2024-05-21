package dev.toastbits.spms.mpv

import java.lang.foreign.MemorySegment

// import com.sun.jna.Structure
// import com.sun.jna.MpvEventData

actual class MpvEventStartFile private constructor(private val event_data: MemorySegment) {
    actual companion object {
        actual fun fromData(data: MpvEventData?): MpvEventStartFile = MpvEventStartFile(data!!.data)
    }

    actual val playlist_entry_id: Long get() = mpv_event_start_file.playlist_entry_id(event_data)
}

actual class MpvEventProperty private constructor(private val event_data: MemorySegment) {
    actual companion object {
        actual fun fromData(data: MpvEventData?): MpvEventProperty = MpvEventProperty(data!!.data)
    }

    actual val name: String? get() = mpv_event_property.name(event_data).getString()
    actual val format: Int get() = mpv_event_property.format(event_data)
    actual val data: MpvEventPropertyData? get() = TODO()//structure.data?.let {
    //     object : MpvEventPropertyData {
    //         override fun toBoolean(): Boolean = it.toBoolean()
    //     }
    // }
}

actual class MpvEventHook private constructor(private val event_data: MemorySegment) {
    actual companion object {
        actual fun fromData(data: MpvEventData?): MpvEventHook = MpvEventHook(data!!.data)
    }

    actual val name: String? get() = TODO()//structure.name
    actual val id: Long get() = TODO()//structure.id
}

actual class MpvEventLogMessage private constructor(private val event_data: MemorySegment) {
    actual companion object {
        actual fun fromData(data: MpvEventData?): MpvEventLogMessage = MpvEventLogMessage(data!!.data)
    }

    actual val prefix: String? get() = mpv_event_log_message.prefix(event_data).getString()
    actual val level: String? get() = mpv_event_log_message.level(event_data).getString()
    actual val text: String? get() = mpv_event_log_message.text(event_data).getString()
    actual val log_level: Int get() = mpv_event_log_message.log_level(event_data)
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
