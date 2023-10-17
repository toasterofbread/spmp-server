package spms.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.SpMpServer

class ServerActionMoveItem: ServerAction(
    identifier = "moveItem",
    help = "Move the item at index `from` in the queue to index `to`",
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "from",
            "Initial queue index of the item to move"
        ),
        Parameter(
            Parameter.Type.Int,
            true,
            "to",
            "Destination queue index"
        )
    )
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        val from: Int = context.getParameterValue("from")!!.int
        val to: Int = context.getParameterValue("to")!!.int

        server.mpv.moveItem(from, to)
        return null
    }
}
