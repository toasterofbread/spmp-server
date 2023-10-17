@file:Suppress("INVISIBLE_MEMBER")
package controller

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import zmq.ZmqSocket

@OptIn(ExperimentalForeignApi::class)
data class ControllerModeContext(val socket: ZmqSocket, val mem_scope: MemScope, val verbose: Boolean) {
    fun release() {
        logVerbose("Releasing socket...")
        socket.release()
        mem_scope.clearImpl()
    }

    fun log(message: Any?) {
        println(message)
    }

    fun logVerbose(message: Any?) {
        if (verbose) {
            log(message)
        }
    }
}
