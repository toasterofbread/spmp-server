package spms.localisation.strings

import spms.localisation.Language
import spms.localisation.SpMsLocalisation

class LocalisationEn: SpMsLocalisation {
    override val language: Language = Language.EN

    override val server: ServerLocalisation = ServerLocalisationEn()
    override val server_actions: ServerActionLocalisation = ServerActionLocalisationEn()
    override val player_actions: PlayerActionLocalisation = PlayerActionLocalisationEn()
    override val cli: CliLocalisation = CliLocalisationEn()

    override fun usageError(): String =
        "Error:"

    override fun badParameter(): String =
        "invalid value"

    override fun badParameterWithMessage(message: String): String = 
        "invalid value: $message"

    override fun badParameterWithParam(paramName: String): String = 
        "invalid value for $paramName"

    override fun badParameterWithMessageAndParam(paramName: String, message: String): String = 
        "invalid value for $paramName: $message"

    override fun missingOption(paramName: String): String = 
        "missing option $paramName"

    override fun missingArgument(paramName: String): String = 
        "missing argument $paramName"

    override fun noSuchSubcommand(name: String, possibilities: List<String>): String =
        "No such subcommand '$name'" + when (possibilities.size) {
            0 -> ""
            1 -> ". Did you mean ${possibilities[0]}?"
            else -> possibilities.joinToString(prefix = ". (Possible subcommands: ", postfix = ")")
        }

    override fun noSuchOption(name: String, possibilities: List<String>): String =
        "no such option $name" + when (possibilities.size) {
            0 -> ""
            1 -> ". Did you mean ${possibilities[0]}?"
            else -> possibilities.joinToString(prefix = ". (Possible options: ", postfix = ")")
        }

    override fun incorrectOptionValueCount(name: String, count: Int): String =
        when (count) {
            0 -> "option $name does not take a value"
            1 -> "option $name requires a value"
            else -> "option $name requires $count values"
        }

    override fun incorrectArgumentValueCount(name: String, count: Int): String =
        when (count) {
            0 -> "argument $name does not take a value"
            1 -> "argument $name requires a value"
            else -> "argument $name requires $count values"
        }

    override fun mutexGroupException(name: String, others: List<String>): String = 
        "option $name cannot be used with ${others.joinToString(" or ")}"

    override fun fileNotFound(filename: String): String = 
        "$filename not found"

    override fun invalidFileFormat(filename: String, message: String): String =
        "incorrect format in file $filename: $message"

    override fun invalidFileFormat(filename: String, lineNumber: Int, message: String): String =
        "incorrect format in file $filename line $lineNumber: $message"

    override fun unclosedQuote(): String =
        "unclosed quote"

    override fun fileEndsWithSlash(): String = 
        "file ends with \\"

    override fun extraArgumentOne(name: String): String = 
        "got unexpected extra argument $name"

    override fun extraArgumentMany(name: String, count: Int): String = 
        "got unexpected extra arguments $name"

    override fun invalidFlagValueInFile(name: String): String = 
        "invalid flag value in file for option $name"

    override fun switchOptionEnvvar(): String = 
        "environment variables not supported for switch options"

    override fun requiredMutexOption(options: String): String = 
        "must provide one of $options"

    override fun invalidGroupChoice(value: String, choices: List<String>): String = 
        "invalid choice: $value. (choose from ${choices.joinToString()})"

    override fun floatConversionError(value: String): String = 
        "$value is not a valid floating point value"

    override fun intConversionError(value: String): String = 
        "$value is not a valid integer"

    override fun boolConversionError(value: String): String = 
        "$value is not a valid boolean"

    override fun rangeExceededMax(value: String, limit: String): String = 
        "$value is larger than the maximum valid value of $limit."

    override fun rangeExceededMin(value: String, limit: String): String = 
        "$value is smaller than the minimum valid value of $limit."

    override fun rangeExceededBoth(value: String, min: String, max: String): String = 
        "$value is not in the valid range of $min to $max."

    override fun invalidChoice(choice: String, choices: List<String>): String = 
        "invalid choice: $choice. (choose from ${choices.joinToString()})"

    override fun pathTypeFile(): String = 
        "file"

    override fun pathTypeDirectory(): String = 
        "directory"

    override fun pathTypeOther(): String = 
        "path"

    override fun pathDoesNotExist(pathType: String, path: String): String = 
        "$pathType \"$path\" does not exist."

    override fun pathIsFile(pathType: String, path: String): String = 
        "$pathType \"$path\" is a file."

    override fun pathIsDirectory(pathType: String, path: String): String = 
        "$pathType \"$path\" is a directory."

    override fun pathIsNotWritable(pathType: String, path: String): String = 
        "$pathType \"$path\" is not writable."

    override fun pathIsNotReadable(pathType: String, path: String): String = 
        "$pathType \"$path\" is not readable."

    override fun pathIsSymlink(pathType: String, path: String): String = 
        "$pathType \"$path\" is a symlink."

    override fun defaultMetavar(): String = 
        "value"

    override fun stringMetavar(): String = 
        "text"

    override fun floatMetavar(): String = 
        "float"

    override fun intMetavar(): String = 
        "int"

    override fun pathMetavar(): String = 
        "path"

    override fun fileMetavar(): String = 
        "file"

    override fun usageTitle(): String = 
        "Usage:"

    override fun optionsTitle(): String = 
        "Options"

    override fun argumentsTitle(): String = 
        "Arguments"

    override fun commandsTitle(): String = 
        "Commands"

    override fun optionsMetavar(): String = 
        "options"

    override fun commandMetavar(): String = 
        "command"

    override fun argumentsMetavar(): String = 
        "args"

    override fun helpTagDefault(): String = 
        "default"

    override fun helpTagRequired(): String = 
        "required"

    override fun helpOptionMessage(): String = 
        "Show this message and exit"
}
