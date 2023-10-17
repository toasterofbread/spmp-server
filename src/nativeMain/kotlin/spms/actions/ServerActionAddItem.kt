package spms.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.SpMpServer

class ServerActionAddItem: ServerAction(
    identifier = "addItem",
    help = "Add an item to the queue",
    parameters = listOf(
        Parameter(
            Parameter.Type.String,
            true,
            "item_id",
            "YouTube ID of the item to add"
        ),
        Parameter(
            Parameter.Type.Int,
            true,
            "index",
            "The queue index at which to insert the item"
        )
    )
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        val item_id: String = context.getParameterValue("item_id")!!.content
        val index: Int = context.getParameterValue("index")?.int ?: -1

        server.mpv.addItem(item_id, index)
        return null
    }
}
