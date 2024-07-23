package dev.toastbits.spms.zmq

import kotlin.time.Duration

actual class ZmqSocket actual constructor(type: ZmqSocketType, is_binder: Boolean) {
    actual fun isConnected(): Boolean = false

    actual fun connect(address: String) {
    }

    actual fun disconnect() {
    }

    actual fun release() {
    }

    actual fun recvStringMultipart(timeout: Duration?): List<String>? {
        return null
    }

    actual fun recvMultipart(timeout: Duration?): List<ByteArray>? {
        return null
    }

    actual fun sendStringMultipart(parts: List<String>) {

    }

    actual fun sendMultipart(parts: List<ByteArray>) {
    }
}
