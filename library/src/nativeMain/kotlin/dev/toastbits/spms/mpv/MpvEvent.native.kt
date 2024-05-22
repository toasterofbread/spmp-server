package dev.toastbits.spms.mpv

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.value
import kotlinx.cinterop.toKString
import libmpv.*

class InternalMpvEvent(private val event_data: mpv_event): MpvEvent() {
    override val event_id: Int get() = event_data.event_id.toInt()
    override val error: Int get() = event_data.error
    override val reply_userdata: Long get() = event_data.reply_userdata.toLong()
    override val data: MpvEventData? get() = event_data.data?.let { MpvEventData(it) }
}
actual class MpvEventData(val pointer: CPointer<*>)

actual class MpvEventStartFile actual constructor(event_data: MpvEventData) {
    private val event: mpv_event_start_file = event_data.pointer.pointedAs()
    actual val playlist_entry_id: Long get() = event.playlist_entry_id
}

actual class MpvEventProperty actual constructor(event_data: MpvEventData) {
    private val event: mpv_event_property = event_data.pointer.pointedAs()
    actual val name: String? get() = event.name?.toKString()
    actual val format: Int get() = event.format.toInt()
    actual val data: MpvEventPropertyData? get() = event.data?.let {
        object : MpvEventPropertyData {
            override fun toBoolean(): Boolean = it.pointedAs<BooleanVar>().value
        }
    }
}

actual class MpvEventHook actual constructor(event_data: MpvEventData) {
    private val event: mpv_event_hook = event_data.pointer.pointedAs()
    actual val name: String? get() = event.name?.toKString()
    actual val id: Long get() = event.id.toLong()
}

actual class MpvEventLogMessage actual constructor(event_data: MpvEventData) {
    private val event: mpv_event_log_message = event_data.pointer.pointedAs()
    actual val prefix: String? get() = event.prefix?.toKString()
    actual val level: String? get() = event.level?.toKString()
    actual val text: String? get() = event.text?.toKString()
    actual val log_level: Int get() = event.log_level.toInt()
}

@OptIn(ExperimentalForeignApi::class)
private inline fun <reified T: CPointed> CPointer<*>?.pointedAs(): T =
    this!!.reinterpret<T>().pointed
