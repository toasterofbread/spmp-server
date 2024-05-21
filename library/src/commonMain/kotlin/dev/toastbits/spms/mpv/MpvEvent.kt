package dev.toastbits.spms.mpv

interface MpvEvent {
    val event_id: Int
    val error: Int
    val reply_userdata: Long
    val data: MpvEventData?
}

expect class MpvEventStartFile {
    companion object {
        fun fromData(data: MpvEventData?): MpvEventStartFile
    }

    val playlist_entry_id: Long
}

expect class MpvEventProperty {
    companion object {
        fun fromData(data: MpvEventData?): MpvEventProperty
    }

    val name: String?
    val format: Int
    val data: MpvEventPropertyData?
}

interface MpvEventPropertyData {
    fun toBoolean(): Boolean
}

expect class MpvEventHook {
    companion object {
        fun fromData(data: MpvEventData?): MpvEventHook
    }

    val name: String?
    val id: Long
}

expect class MpvEventLogMessage {
    companion object {
        fun fromData(data: MpvEventData?): MpvEventLogMessage
    }

    val prefix: String?
    val level: String?
    val text: String?
    val log_level: Int
}
