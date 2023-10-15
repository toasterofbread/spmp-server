package zmq

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
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
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.value
import libzmq.*
import platform.posix.memcpy

const val MESSAGE_SIZE = 255

@OptIn(ExperimentalForeignApi::class)
abstract class ZmqRouter(mem_scope: MemScope) {
    protected class Message(val client_id: ByteArray, val parts: List<String>)

    private val message_buffer: CPointer<ByteVarOf<Byte>>
    private val message_buffer_size: ULong

    private val has_more: IntVar
    private val has_more_size: ULongVarOf<ULong>

    private val context = zmq_ctx_new()
    private val socket = zmq_socket(context, ZMQ_ROUTER)
    private val poller = zmq_poller_new()
    private var bound_to_port: Int? = null

    init {
        with (mem_scope) {
            message_buffer = allocArray(MESSAGE_SIZE)
            message_buffer_size = (sizeOf<ByteVar>() * MESSAGE_SIZE).toULong()

            has_more = alloc()
            has_more_size = alloc<ULongVar>().apply { value = sizeOf<IntVar>().toULong() }
        }
    }

    private fun getBindAddress(port: Int): String =
        "tcp://*:$port"

    fun getBoundPort(): Int? = bound_to_port

    fun bind(port: Int) {
        check(bound_to_port == null) { "Server is already bound to port $bound_to_port" }

        val rc: Int = zmq_bind(socket, getBindAddress(port))
        check(rc == 0) { "Binding to ${getBindAddress(port)} failed ($rc)" }

        zmq_poller_add(poller, socket, null, ZMQ_POLLIN.toShort())
    }

    fun unbind() {
        val port: Int? = bound_to_port
        check(port != null) { "Server is not bound" }

        zmq_poller_remove(poller, socket)
        zmq_unbind(socket, getBindAddress(port))
    }

    fun release() {
        if (getBoundPort() != null) {
            unbind()
        }

        zmq_close(socket)
        zmq_ctx_destroy(context)
    }

    protected fun recvMultipart(timeout_ms: Long?): Message? = memScoped {
        val event: zmq_poller_event_t = alloc()
        zmq_poller_wait(poller, event.ptr, timeout_ms ?: ZMQ_NOBLOCK.toLong())

        if (event.events.toInt() != ZMQ_POLLIN) {
            return null
        }

        var client_id: ByteArray? = null
        val parts: MutableList<String> = mutableListOf()

        do {
            val size = zmq_recv(socket, message_buffer, message_buffer_size, 0)
            check(size >= 0)

            if (client_id == null) {
                client_id = message_buffer.pointed.ptr.readBytes(size)
            }
            else {
                val bytes = message_buffer.pointed.ptr.readBytes(size)
                parts.add(bytes.decodeToString())
            }

            val rc = zmq_getsockopt(socket, ZMQ_RCVMORE, has_more.ptr, has_more_size.ptr)
            check(rc == 0)
        }
        while (has_more.value == 1)

        if (client_id == null) {
            return null
        }

        return Message(client_id, parts)
    }

    protected fun sendMultipart(message: Message) = memScoped {
        require(message.parts.isNotEmpty())

        sendBytes(message.client_id.toCValues(), message.client_id.size, ZMQ_SNDMORE)

        for ((i, part) in message.parts.withIndex()) {
            val string = part.cstr
            sendBytes(string.ptr, string.size, if (i + 1 == message.parts.size) null else ZMQ_SNDMORE)
        }
    }

    private fun MemScope.sendBytes(bytes: CValuesRef<ByteVar>, size: Int, flags: Int? = null) {
        val msg: zmq_msg_t = alloc()

        var rc: Int = zmq_msg_init_size(msg.ptr, size.toULong())
        check(rc == 0) { "Could not init message ($rc)" }

        memcpy(zmq_msg_data(msg.ptr), bytes, size.toULong())

        rc = zmq_msg_send(msg.ptr, socket, flags ?: 0)
        check(rc == size) { "zmq_msg_send failed ($rc != $size)" }
    }
}
