package dev.toastbits.spms.zmq

import kotlin.time.Duration

enum class ZmqSocketType {
    REQ, REP, DEALER, ROUTER
}

expect class ZmqSocket(type: ZmqSocketType, is_binder: Boolean) {
    fun isConnected(): Boolean

    fun connect(address: String)
    fun disconnect()

    fun release()

    fun recvStringMultipart(timeout: Duration?): List<String>?
    fun recvMultipart(timeout: Duration?): List<ByteArray>?

    fun sendStringMultipart(parts: List<String>)
    fun sendMultipart(parts: List<ByteArray>)
}
