package dev.toastbits.spms.client

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import dev.toastbits.spms.localisation.loc
import dev.toastbits.spms.server.DEFAULT_ADDRESS
import dev.toastbits.spms.socketapi.shared.SPMS_DEFAULT_PORT

class ClientOptions: OptionGroup() {
    val ip: String by option("-i", "--ip").default(DEFAULT_ADDRESS).help { context.loc.cli.option_help_server_ip }
    val port: Int by option("-p", "--port").int().default(SPMS_DEFAULT_PORT).help { context.loc.cli.option_help_server_port }

    fun getAddress(protocol: String): String =
        "$protocol://$ip:$port"
}
