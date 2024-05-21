package dev.toastbits.spms

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import dev.toastbits.spms.localisation.SpMsLocalisation
import dev.toastbits.spms.localisation.loc
import dev.toastbits.spms.server.SpMs
import dev.toastbits.spms.socketapi.shared.SpMsLanguage
import dev.toastbits.spms.socketapi.shared.SPMS_API_VERSION
import kotlin.system.exitProcess
import dev.toastbits.spms.localisation.AppLocalisedMessageProvider
import dev.toastbits.spms.localisation.AppLocalisation

abstract class Command(
    name: String,
    private val help: AppLocalisedMessageProvider?,
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

    override fun commandHelpEpilog(context: Context): String = context.loc.cli.bug_report_notice
    override fun commandHelp(context: Context): String = help?.invoke(context.loc).orEmpty()

    override fun run() {
        SpMs.logging_enabled = !silent

        val lang: SpMsLanguage? = SpMsLanguage.fromCode(language)
        if (lang != null && lang != localisation.language) {
            localisation = AppLocalisation.get(lang)
        }
        context {
            localization = localisation
        }

        if (output_version) {
            SpMs.printVersionInfo(localisation)
            exitProcess(0)
        }
    }

    init {
        context {
            localization = localisation
        }
    }

    companion object {
        var localisation: AppLocalisation = AppLocalisation.get()
    }
}

