package controller.modes

import controller.SpMsControllerModeCommand

class Interactive: SpMsControllerModeCommand("interactive") {
    override fun run() {
        println("Running in interactive mode")

        // TODO
        Poll().parse(emptyList(), currentContext)
    }
}
