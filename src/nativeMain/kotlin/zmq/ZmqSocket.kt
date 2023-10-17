package zmq

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValues
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.ULongVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import libzmq.ZMQ_LINGER
import libzmq.ZMQ_NOBLOCK
import libzmq.ZMQ_POLLIN
import libzmq.ZMQ_RCVMORE
import libzmq.ZMQ_SNDMORE
import libzmq.zmq_bind
import libzmq.zmq_close
import libzmq.zmq_connect
import libzmq.zmq_ctx_destroy
import libzmq.zmq_ctx_new
import libzmq.zmq_ctx_term
import libzmq.zmq_disconnect
import libzmq.zmq_getsockopt
import libzmq.zmq_msg_close
import libzmq.zmq_msg_data
import libzmq.zmq_msg_init_size
import libzmq.zmq_msg_send
import libzmq.zmq_msg_t
import libzmq.zmq_poller_add
import libzmq.zmq_poller_event_t
import libzmq.zmq_poller_new
import libzmq.zmq_poller_remove
import libzmq.zmq_poller_wait
import libzmq.zmq_recv
import libzmq.zmq_setsockopt
import libzmq.zmq_socket
import libzmq.zmq_unbind
import platform.posix.memcpy

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
            val linger: IntVar = alloc()
            linger.value = 0
            zmq_setsockopt(socket, ZMQ_LINGER, linger.ptr, sizeOf<IntVar>().toULong())

            message_buffer = allocArray(MESSAGE_SIZE)
            message_buffer_size = (sizeOf<ByteVar>() * MESSAGE_SIZE).toULong()

            has_more = alloc()
            has_more_size = alloc<ULongVar>().apply { value = sizeOf<IntVar>().toULong() }
        }
    }

    fun isConnected(): Boolean = current_address != null

    fun connect(address: String) {
        check(current_address == null) {
            if (is_binder) "Already bound to address $current_address"
            else "Already connected to address $current_address"
        }

        val rc: Int =
            if (is_binder) zmq_bind(socket, address)
            else zmq_connect(socket, address)

        check(rc == 0) {
            if (is_binder) "Binding to $address failed ($rc)"
            else "Connecting to $address failed ($rc)"
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

    fun recvStringMultipart(timeout_ms: Long?): List<String>? =
        recvMultipart(timeout_ms)?.mapNotNull { part ->
            part.decodeToString().removeSuffix("\u0000").takeIf { it.isNotEmpty() }
        }

    fun recvMultipart(timeout_ms: Long?): List<ByteArray>? = memScoped {
        val event: zmq_poller_event_t = alloc()
        zmq_poller_wait(poller, event.ptr, timeout_ms ?: ZMQ_NOBLOCK.toLong())

        if (event.events.toInt() != ZMQ_POLLIN) {
            return null
        }

        val parts: MutableList<ByteArray> = mutableListOf()

        do {
            val size = zmq_recv(socket, message_buffer, message_buffer_size, 0)
            check(size >= 0)

            parts.add(message_buffer.pointed.ptr.readBytes(size))

            val rc = zmq_getsockopt(socket, ZMQ_RCVMORE, has_more.ptr, has_more_size.ptr)
            check(rc == 0)
        }
        while (has_more.value == 1)

        return parts
    }

    fun sendStringMultipart(parts: List<String>) =
        sendMultipart(parts.map { it.cstr })

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
