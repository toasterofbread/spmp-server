package controller

import Command
import com.github.ajalt.clikt.core.requireObject
import zmq.ZmqSocket

abstract class SpMsControllerModeCommand(name: String): Command(name) {
    private val context: SpMpControllerCommandContext by requireObject()
    protected val socket: ZmqSocket get() = context.socket

    fun releaseSocket() {
        context.release()
    }
}
