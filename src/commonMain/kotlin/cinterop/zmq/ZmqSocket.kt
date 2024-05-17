package cinterop.zmq

import kotlinx.cinterop.*
import libzmq.*
import platform.posix.memcpy
import spms.zmqPollerWait
import spms.socketapi.shared.SpMsSocketApi
import spms.socketapi.shared.SPMS_MESSAGE_MAX_SIZE
import kotlin.time.Duration

@OptIn(ExperimentalForeignApi::class)
class ZmqSocket(mem_scope: MemScope, type: Int, val is_binder: Boolean) {
    private val message_buffer: CPointer<ByteVarOf<Byte>>
    private val message_buffer_size: ULong

    private val has_more: IntVar
    private val has_more_size: ULongVarOf<ULong>

    private val context = zmq_ctx_new()
    private val socket = zmq_socket(context, type)
    private val poller = zmq_poller_new()

    private var current_address: String? = null

    init {
        with (mem_scope) {
            setSocketOption(ZMQ_LINGER, 0)

            message_buffer = allocArray(SPMS_MESSAGE_MAX_SIZE)
            message_buffer_size = (sizeOf<ByteVar>() * SPMS_MESSAGE_MAX_SIZE).toULong()

            has_more = alloc()
            has_more_size = alloc<ULongVar>().apply { value = sizeOf<IntVar>().toULong() }
        }
    }

    fun isConnected(): Boolean = current_address != null

    fun setSocketOption(option: Int, value: Int) {
        memScoped {
            val val_ptr: IntVar = alloc()
            val_ptr.value = value
            zmq_setsockopt(socket, option, val_ptr.ptr, sizeOf<IntVar>().toULong())
        }
    }

    fun connect(address: String) {
        check(current_address == null) {
            if (is_binder) "Already bound to address $current_address"
            else "Already connected to address $current_address"
        }

        val result: Int =
            if (is_binder) zmq_bind(socket, address)
            else zmq_connect(socket, address)

        check(result == 0) {
            if (is_binder) "Binding to $address failed ($result)"
            else "Connecting to $address failed ($result)"
        }

        zmq_poller_add(poller, socket, null, ZMQ_POLLIN.toShort())

        current_address = address
    }

    fun disconnect() {
        val address: String? = current_address
        check(address != null) {
            if (is_binder) "Not bound"
            else "Not connected"
        }

        zmq_poller_remove(poller, socket)

        if (is_binder) zmq_unbind(socket, address)
        else zmq_disconnect(socket, address)

        current_address = null
    }

    fun release() {
        if (current_address != null) {
            disconnect()
            check(current_address == null)
        }

        zmq_close(socket)
        zmq_ctx_destroy(context)
    }

    fun recvStringMultipart(timeout: Duration?): List<String>? {
        val message: List<ByteArray> = recvMultipart(timeout) ?: return null
        return SpMsSocketApi.decode(message.map { it.decodeToString() })
    }

    fun recvMultipart(timeout: Duration?): List<ByteArray>? = memScoped {
        val event: zmq_poller_event_t = alloc()
        zmqPollerWait(poller, event.ptr, timeout?.inWholeMilliseconds ?: ZMQ_NOBLOCK.toLong())

        if (event.events.toInt() != ZMQ_POLLIN) {
            return null
        }

        val parts: MutableList<ByteArray> = mutableListOf()

        do {
            val size: Int = zmq_recv(socket, message_buffer, message_buffer_size, 0)
            check(size >= 0) { "Size is $size" }

            parts.add(message_buffer.pointed.ptr.readBytes(size))

            val result: Int = zmq_getsockopt(socket, ZMQ_RCVMORE, has_more.ptr, has_more_size.ptr)
            check(result == 0) { "zmq_getsockopt failed ($result)" }
        }
        while (has_more.value == 1)

        return parts
    }

    fun sendStringMultipart(parts: List<String>) =
        sendMultipart(SpMsSocketApi.encode(parts).map { it.cstr })

    fun sendMultipart(parts: List<CValues<ByteVar>>) = memScoped {
        if (parts.isEmpty()) {
            return
        }

        for ((i, part) in parts.withIndex()) {
            sendBytes(part.ptr, part.size, if (i + 1 == parts.size) null else ZMQ_SNDMORE)
        }
    }

    private fun MemScope.sendBytes(bytes: CValuesRef<ByteVar>, size: Int, flags: Int? = null) {
        val msg: zmq_msg_t = alloc()

        var rc: Int = zmq_msg_init_size(msg.ptr, size.toULong())
        check(rc == 0) { "Could not init message ($rc)" }

        memcpy(zmq_msg_data(msg.ptr), bytes, size.toULong())

        rc = zmq_msg_send(msg.ptr, socket, flags ?: 0)
        check(rc == size) { "zmq_msg_send failed ($rc != $size)" }

        zmq_msg_close(msg.ptr)
    }
}
