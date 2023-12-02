@file:Suppress("INVISIBLE_MEMBER")
package spms.controller

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import cinterop.zmq.ZmqSocket

@OptIn(ExperimentalForeignApi::class)
data class ControllerModeContext(
    val socket: ZmqSocket,
    val mem_scope: MemScope,
    val client_name: String
) {
    fun release() {
        socket.release()
        mem_scope.clearImpl()
    }
}
