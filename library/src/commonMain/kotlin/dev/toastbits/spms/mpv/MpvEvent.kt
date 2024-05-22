package dev.toastbits.spms.mpv

abstract class MpvEvent {
    abstract val event_id: Int
    abstract val error: Int
    abstract val reply_userdata: Long
    abstract val data: MpvEventData?

    override fun toString(): String =
        "MpvEvent(event_id=$event_id, error=$error, reply_userdata=$reply_userdata)"
}

expect class MpvEventData

expect class MpvEventStartFile(event_data: MpvEventData) {
    val playlist_entry_id: Long
}

expect class MpvEventProperty(event_data: MpvEventData) {
    val name: String?
    val format: Int
    val data: MpvEventPropertyData?
}

interface MpvEventPropertyData {
    fun toBoolean(): Boolean
}

expect class MpvEventHook(event_data: MpvEventData) {
    val name: String?
    val id: Long
}

expect class MpvEventLogMessage(event_data: MpvEventData) {
    val prefix: String?
    val level: String?
    val text: String?
    val log_level: Int
}
