package dev.toastbits.spms.socketapi

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import dev.toastbits.spms.localisation.LocalisedMessageProvider
import dev.toastbits.spms.localisation.SpMsLocalisation

abstract class Action {
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

    protected inner class ActionContext(private val parameter_values: List<JsonElement>) {
        fun getParameterValue(identifier: String): JsonElement? {
            val index: Int = parameters.indexOfFirst { it.identifier == identifier }
            val parameter: Parameter = parameters[index]

            val value: JsonElement? = parameter_values.getOrNull(index)
            if (value == null && parameter.required) {
                throw InvalidParameterException(parameter, value)
            }

            return value
        }
    }

    open fun formatResult(result: JsonElement, localisation: SpMsLocalisation): String = result.toString()
}
