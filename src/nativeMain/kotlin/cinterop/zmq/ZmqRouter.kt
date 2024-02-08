package cinterop.zmq

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toCValues
import libzmq.*

@OptIn(ExperimentalForeignApi::class)
abstract class ZmqRouter(mem_scope: MemScope) {
    protected class Message(val client_id: ByteArray, val parts: List<String>) {
        override fun toString(): String =
            "Message(client_id=${client_id.contentHashCode()}, parts=$parts)"
    }

    private val socket: ZmqSocket = ZmqSocket(mem_scope, ZMQ_ROUTER, is_binder = true)
    var bound_port: Int? = null
        private set

    fun bind(port: Int) {
        socket.connect("tcp://*:$port")
        bound_port = port
    }

    fun unbind() {
        socket.disconnect()
        bound_port = null
    }

    fun release() {
        if (socket.isConnected()) {
            unbind()
        }

        socket.release()
    }

    protected fun recvMultipart(timeout_ms: Long?): Message? {
        val parts: List<ByteArray> = socket.recvMultipart(timeout_ms) ?: return null

        var client_id: ByteArray? = null
        val message_parts: MutableList<String> = mutableListOf()
        var current_part: StringBuilder = StringBuilder()

        for (part in parts) {
            if (client_id == null) {
                client_id = part
                continue
            }

            val decoded_part: String = part.decodeToString()
            if (decoded_part.lastOrNull() == '\u0000') {
                current_part.appendRange(decoded_part, 0, decoded_part.length - 1)
                message_parts.add(current_part.toString())
                current_part.clear()
            }
            else {
                current_part.append(decoded_part)
            }
        }

        if (client_id == null) {
            return null
        }

        if (current_part.isNotEmpty()) {
            message_parts.add(current_part.toString())
        }

        return Message(client_id, message_parts)
    }

    protected fun sendMultipart(message: Message) {
        socket.sendMultipart(
            listOf(message.client_id.toCValues()) + message.parts.flatMap { part ->
                val chunks: List<String> = part.chunked(MESSAGE_MAX_SIZE - 8)
                chunks.mapIndexed { i, chunk ->
                    (if (i + 1 == chunks.size) chunk else chunk + '\u0000').cstr
                }
            }
        )
    }
}
