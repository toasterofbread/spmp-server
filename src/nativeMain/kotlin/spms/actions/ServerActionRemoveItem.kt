package spms.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.SpMpServer

class ServerActionRemoveItem: ServerAction(
    identifier = "removeItem",
    help = "Remove the item at index `from` from the queue",
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "from",
            "Queue index of the item to remove"
        )
    )
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        val from: Int = context.getParameterValue("from")!!.int
        server.mpv.removeItem(from)
        return null
    }
}
