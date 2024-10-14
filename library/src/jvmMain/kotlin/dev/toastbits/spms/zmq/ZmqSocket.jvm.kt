package dev.toastbits.spms.zmq

import dev.toastbits.spms.socketapi.shared.SpMsSocketApi
import dev.toastbits.spms.socketapi.shared.SPMS_MESSAGE_MAX_SIZE
import kotlin.time.Duration
// import org.zeromq.ZContext
// import org.zeromq.ZMQ.SNDMORE
// import org.zeromq.ZMQ.Msg
// import org.zeromq.SocketType as ZSocketType
// import org.zeromq.ZMQ.Socket as ZSocket
// import org.zeromq.ZMQ.Poller as ZPoller
import org.zeromq.*

actual class ZmqSocket actual constructor(type: ZmqSocketType, val is_binder: Boolean) {
    private val context: ZContext = ZContext()
    private val socket: ZMQ.Socket = context.createSocket(
        when (type) {
            ZmqSocketType.REQ -> SocketType.REQ
            ZmqSocketType.REP -> SocketType.REP
            ZmqSocketType.DEALER -> SocketType.DEALER
            ZmqSocketType.ROUTER -> SocketType.ROUTER
        }
    )
    private val poller: ZMQ.Poller = context.createPoller(0)

    private var current_address: String? = null

    init {
        context.setLinger(0)
    }

    actual fun isConnected(): Boolean = current_address != null

    actual fun connect(address: String) {
        check(current_address == null) {
            if (is_binder) "Already bound to address $current_address"
            else "Already connected to address $current_address"
        }

        val result: Boolean =
            if (is_binder) socket.bind(address)
            else socket.connect(address)

        check(result) {
            if (is_binder) "Binding to $address failed ($result)"
            else "Connecting to $address failed ($result)"
        }

        poller.register(socket, ZPoller.POLLIN)

        current_address = address
    }

    actual fun disconnect() {
        val address: String? = current_address
        check(address != null) {
            if (is_binder) "Not bound"
            else "Not connected"
        }

        poller.unregister(socket)

        if (is_binder) socket.unbind(address)
        else socket.disconnect(address)

        current_address = null
    }

    actual fun release() {
        if (current_address != null) {
            disconnect()
            check(current_address == null)
        }

        socket.close()
        context.destroy()
    }

    actual fun recvStringMultipart(timeout: Duration?): List<String>? {
        val message: List<ByteArray> = recvMultipart(timeout) ?: return null
        return SpMsSocketApi.decode(message.map { it.decodeToString() })
    }

    actual fun recvMultipart(timeout: Duration?): List<ByteArray>? {
        if (poller.poll(timeout?.inWholeMilliseconds ?: 1) <= 0) {
            return null
        }

        val parts: MutableList<ByteArray> = mutableListOf()

        while (true) {
            for (part in ZMsg.recvMsg(socket)) {
                parts.add(part.data)
            }

            if (!socket.hasReceiveMore()) {
                break
            }
        }

        return parts
    }

    actual fun sendStringMultipart(parts: List<String>) =
        sendMultipart(SpMsSocketApi.encode(parts))

    actual fun sendMultipart(parts: List<ByteArray>) {
        if (parts.isEmpty()) {
            return
        }

        for ((i, part) in parts.withIndex()) {
            sendBytes(part, part.size, if (i + 1 == parts.size) null else ZMQ.SNDMORE)
        }
    }

    private fun sendBytes(bytes: ByteArray, size: Int, flags: Int? = null) {
        val msg: zmq.Msg = zmq.Msg(bytes)
        socket.sendMsg(msg, flags ?: 0)
    }
}
