package controller.modes

import controller.SpMsControllerModeCommand

class Run: SpMsControllerModeCommand("run") {
    override fun run() {
        TODO()

        releaseSocket()
    }
}
