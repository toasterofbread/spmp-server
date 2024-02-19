package spms

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import spms.localisation.SpMsLocalisation
import spms.localisation.loc
import spms.server.SpMs
import spms.socketapi.shared.SpMsLanguage
import spms.socketapi.shared.SPMS_API_VERSION
import kotlin.system.exitProcess

typealias LocalisedMessageProvider = SpMsLocalisation.() -> String

abstract class Command(
    name: String,
    private val help: LocalisedMessageProvider?,
    is_default: Boolean = false,
    hidden: Boolean = false,
    help_tags: Map<String, String> = emptyMap()
): CliktCommand(
    name = name,
    invokeWithoutSubcommand = is_default,
    hidden = hidden,
    helpTags = help_tags
) {
    protected val output_version: Boolean by option("-v", "--version").flag().help { context.loc.cli.option_help_version }
    protected val silent: Boolean by option("-s", "--silent").flag().help { context.loc.cli.option_help_silent }
    protected val language: String by option("-l", "--lang").default("").help { context.loc.cli.option_help_language }
    protected val halt: Boolean by option("--halt", hidden = true, envvar = "SPMS_HALT").flag().help { context.loc.cli.option_help_halt }

    protected fun log(message: Any?) {
        SpMs.log(message)
    }

    protected fun getVersionInfoText(): String =
        localisation.versionInfoText(SPMS_API_VERSION)

    override fun commandHelpEpilog(context: Context): String = context.loc.cli.bug_report_notice
    override fun commandHelp(context: Context): String = help?.invoke(context.loc).orEmpty()

    override fun run() {
        SpMs.logging_enabled = !silent

        val lang: SpMsLanguage? = SpMsLanguage.fromCode(language)
        if (lang != null && lang != localisation.language) {
            localisation = SpMsLocalisation.get(lang)
        }
        context {
            localization = localisation
        }

        if (output_version || !silent) {
            println(getVersionInfoText())

            if (output_version) {
                exitProcess(0)
            }
        }
    }

    init {
        context {
            localization = localisation
        }
    }

    companion object {
        var localisation: SpMsLocalisation = SpMsLocalisation.get()
    }
}

