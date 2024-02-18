package spms.client.cli.modes

import spms.client.cli.CommandLineClientMode

class Interactive: CommandLineClientMode("interactive", { "TODO" }) {
    override fun run() {
        super.run()

        connectSocket()

        println("Running in interactive mode")

        // TODO
        Poll().parse(emptyList(), currentContext)
    }
}
