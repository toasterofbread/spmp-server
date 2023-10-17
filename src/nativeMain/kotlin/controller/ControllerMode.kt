package controller

import Command
import com.github.ajalt.clikt.core.requireObject
import zmq.ZmqSocket

abstract class ControllerMode(name: String, help: String? = null): Command(name, help = help) {
    protected val context: ControllerModeContext by requireObject()
    protected val socket: ZmqSocket get() = context.socket

    fun releaseSocket() {
        context.release()
    }
}
