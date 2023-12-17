package spms.serveraction

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.server.SpMs

class ServerActionSeekToItem: ServerAction(
    identifier = "seekToItem",
    name = { server_actions.seek_to_item_name },
    help = { server_actions.seek_to_item_help },
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "index",
            { server_actions.seek_to_item_param_index }
        )
    )
) {
    override fun execute(server: SpMs, context: ActionContext): JsonElement? {
        val index: Int = context.getParameterValue("index")!!.int
        server.player.seekToItem(index)
        return null
    }
}