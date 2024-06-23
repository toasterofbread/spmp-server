// @file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package dev.toastbits.spms.client.cli

import dev.toastbits.spms.zmq.ZmqSocket

data class CommandLineModeContext(
    val socket: ZmqSocket,
    // val mem_scope: MemScope,
    val client_name: String
) {
    fun release() {
        socket.release()
        // mem_scope.clearImpl()
    }
}
