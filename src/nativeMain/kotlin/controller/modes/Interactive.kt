package controller.modes

import controller.ControllerMode

class Interactive: ControllerMode("interactive") {
    override fun run() {
        println("Running in interactive mode")

        // TODO
        Poll().parse(emptyList(), currentContext)
    }
}
