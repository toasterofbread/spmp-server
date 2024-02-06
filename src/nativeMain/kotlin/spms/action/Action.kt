package spms.action

abstract class Action<ExecutorBase>
    abstract val type: Type
    abstract val identifier: String
    abstract val name: LocalisedMessageProvider
    abstract val help: LocalisedMessageProvider
    abstract val parameters: List<Parameter>
    abstract val hidden: Boolean
    
    enum class Type {
        PLAYER, SERVER
    }

    data class Parameter(
        val type: Type,
        val required: Boolean,
        val identifier: String,
        val help: LocalisedMessageProvider
    ) {
        enum class Type {
            String, Int, Float
        }
    }
    class InvalidParameterException(val parameter: Parameter, val value: JsonPrimitive?): RuntimeException()

    protected inner class ActionContext(val client: SpMsClientID, private val parameter_values: List<JsonPrimitive>) {
        fun getParameterValue(identifier: String): JsonPrimitive? {
            val index: Int = parameters.indexOfFirst { it.identifier == identifier }
            val parameter: Parameter = parameters[index]

            val value: JsonPrimitive? = parameter_values.getOrNull(index)
            if (value == null && parameter.required) {
                throw InvalidParameterException(parameter, value)
            }

            return value
        }
    }

    open fun formatResult(result: JsonElement, context: Context) = result.toString()
    
    protected abstract fun execute(base: ExecutorBase, context: ActionContext): JsonElement?
    fun execute(base: ExecutorBase, client: SpMsClientID, parameter_values: List<JsonPrimitive>): JsonElement? =
        execute(base, ActionContext(client, parameter_values))
}
