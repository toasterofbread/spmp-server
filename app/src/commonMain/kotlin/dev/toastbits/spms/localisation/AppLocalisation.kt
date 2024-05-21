package dev.toastbits.spms.localisation

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.output.Localization
import dev.toastbits.spms.Command
import dev.toastbits.spms.localisation.strings.CliLocalisation
import dev.toastbits.spms.localisation.strings.AppLocalisationEn
import dev.toastbits.spms.localisation.strings.AppLocalisationJa
import dev.toastbits.spms.socketapi.shared.SpMsLanguage

typealias AppLocalisedMessageProvider = AppLocalisation.() -> String
val Context.loc: AppLocalisation get() = Command.localisation

interface AppLocalisation: SpMsLocalisation, Localization {
    val cli: CliLocalisation

    companion object {
        fun get(language: SpMsLanguage = SpMsLanguage.current): AppLocalisation =
            when (language) {
                SpMsLanguage.EN -> AppLocalisationEn()
                SpMsLanguage.JA -> AppLocalisationJa()
            }
    }

    override fun usageError(): String
    override fun badParameter(): String
    override fun badParameterWithMessage(message: String): String
    override fun badParameterWithParam(paramName: String): String
    override fun badParameterWithMessageAndParam(paramName: String, message: String): String
    override fun missingOption(paramName: String): String
    override fun missingArgument(paramName: String): String
    override fun noSuchSubcommand(name: String, possibilities: List<String>): String
    override fun noSuchOption(name: String, possibilities: List<String>): String
    override fun incorrectOptionValueCount(name: String, count: Int): String
    override fun incorrectArgumentValueCount(name: String, count: Int): String
    override fun mutexGroupException(name: String, others: List<String>): String
    override fun fileNotFound(filename: String): String
    override fun invalidFileFormat(filename: String, message: String): String
    override fun invalidFileFormat(filename: String, lineNumber: Int, message: String): String
    override fun unclosedQuote(): String
    override fun fileEndsWithSlash(): String
    override fun extraArgumentOne(name: String): String
    override fun extraArgumentMany(name: String, count: Int): String
    override fun invalidFlagValueInFile(name: String): String
    override fun switchOptionEnvvar(): String
    override fun requiredMutexOption(options: String): String
    override fun invalidGroupChoice(value: String, choices: List<String>): String
    override fun floatConversionError(value: String): String
    override fun intConversionError(value: String): String
    override fun boolConversionError(value: String): String
    override fun rangeExceededMax(value: String, limit: String): String
    override fun rangeExceededMin(value: String, limit: String): String
    override fun rangeExceededBoth(value: String, min: String, max: String): String
    override fun invalidChoice(choice: String, choices: List<String>): String
    override fun pathTypeFile(): String
    override fun pathTypeDirectory(): String
    override fun pathTypeOther(): String
    override fun pathDoesNotExist(pathType: String, path: String): String
    override fun pathIsFile(pathType: String, path: String): String
    override fun pathIsDirectory(pathType: String, path: String): String
    override fun pathIsNotWritable(pathType: String, path: String): String
    override fun pathIsNotReadable(pathType: String, path: String): String
    override fun pathIsSymlink(pathType: String, path: String): String
    override fun defaultMetavar(): String
    override fun stringMetavar(): String
    override fun floatMetavar(): String
    override fun intMetavar(): String
    override fun pathMetavar(): String
    override fun fileMetavar(): String
    override fun usageTitle(): String
    override fun optionsTitle(): String
    override fun argumentsTitle(): String
    override fun commandsTitle(): String
    override fun optionsMetavar(): String
    override fun commandMetavar(): String
    override fun argumentsMetavar(): String
    override fun helpTagDefault(): String
    override fun helpTagRequired(): String
    override fun helpOptionMessage(): String
}
