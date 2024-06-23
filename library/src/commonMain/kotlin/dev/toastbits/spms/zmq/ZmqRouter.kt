package dev.toastbits.spms.zmq

import dev.toastbits.spms.socketapi.shared.SpMsSocketApi
import kotlin.time.Duration

open class ZmqRouter {
    private val socket: ZmqSocket = ZmqSocket(ZmqSocketType.ROUTER, is_binder = true)
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

    fun recvMultipart(timeout: Duration?): ZmqMessage? {
        val parts: List<ByteArray> = socket.recvMultipart(timeout) ?: return null

        var client_id: ByteArray? = null
        val message_parts: MutableList<String> = mutableListOf()

        for (part in parts) {
            if (client_id == null) {
                client_id = part
                continue
            }

            message_parts.add(part.decodeToString())
        }

        if (client_id == null) {
            return null
        }

        return ZmqMessage(client_id, SpMsSocketApi.decode(message_parts))
    }

    fun sendMultipart(message: ZmqMessage) {
        socket.sendMultipart(
            listOf(message.client_id) + SpMsSocketApi.encode(message.parts).map { it.encodeToByteArray() }
        )
    }
}
