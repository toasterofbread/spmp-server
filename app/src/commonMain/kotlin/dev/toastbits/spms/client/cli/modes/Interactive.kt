package dev.toastbits.spms.client.cli.modes

import dev.toastbits.spms.client.cli.CommandLineClientMode
import com.github.ajalt.clikt.core.parse

class Interactive: CommandLineClientMode("interactive", { "TODO" }) {
    override fun run() {
        super.run()

        connectSocket()

        println("Running in interactive mode")

        // TODO
        Poll().parse(emptyList())
    }
}
