@file:Suppress("INVISIBLE_MEMBER")
package spms.client.cli

import cinterop.zmq.ZmqSocket
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope

@OptIn(ExperimentalForeignApi::class)
data class CommandLineModeContext(
    val socket: ZmqSocket,
    val mem_scope: MemScope,
    val client_name: String
) {
    fun release() {
        socket.release()
        mem_scope.clearImpl()
    }
}
