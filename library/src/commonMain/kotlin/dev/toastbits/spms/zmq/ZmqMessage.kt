package dev.toastbits.spms.zmq

class ZmqMessage(val client_id: ByteArray, val parts: List<String>) {
    override fun toString(): String =
        "Message(client_id=${client_id.contentHashCode()}, parts=$parts)"
}
