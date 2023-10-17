@file:Suppress("INVISIBLE_MEMBER")
package controller

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import zmq.ZmqSocket

@OptIn(ExperimentalForeignApi::class)
data class SpMpControllerCommandContext(val socket: ZmqSocket, val mem_scope: MemScope) {
    fun release() {
        println("Releasing socket...")
        socket.release()
        mem_scope.clearImpl()
    }
}
