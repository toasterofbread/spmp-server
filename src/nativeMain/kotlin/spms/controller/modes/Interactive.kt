package spms.controller.modes

import spms.controller.ControllerMode

class Interactive: ControllerMode("interactive", { "TODO" }) {
    override fun run() {
        super.run()

        connectSocket()

        println("Running in interactive mode")

        // TODO
        Poll().parse(emptyList(), currentContext)
    }
}
