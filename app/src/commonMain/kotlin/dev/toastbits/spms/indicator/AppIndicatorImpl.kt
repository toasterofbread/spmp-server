package dev.toastbits.spms.indicator

import dev.toastbits.kjna.runtime.KJnaTypedPointer
import dev.toastbits.kjna.runtime.KJnaFunctionPointer
import dev.toastbits.kjna.runtime.KJnaMemScope
import dev.toastbits.kjna.runtime.KJnaPointer
import dev.toastbits.kjna.runtime.KJnaUtils
import gen.libappindicator.LibAppIndicator
import kjna.struct._AppIndicator
import kjna.struct._GtkMenu
import kjna.struct._GtkMenuShell
import kjna.struct._GMainLoop
import kjna.struct._GtkWidget
import kjna.enum.GConnectFlags
import kjna.enum.AppIndicatorCategory
import kjna.enum.AppIndicatorStatus
import kjna.enum.GLogLevelFlags
import kjna.enum.GLogWriterOutput

class AppIndicatorImpl(name: String, icon_path: List<String>): TrayIndicator {
    private val indicator: KJnaTypedPointer<_AppIndicator>
    private val menu: KJnaTypedPointer<_GtkMenu>
    private var main_loop: KJnaTypedPointer<_GMainLoop>? = null

    private val library: LibAppIndicator = LibAppIndicator()

    override fun show() {
        library.app_indicator_set_menu(indicator, menu)

        main_loop = library.g_main_loop_new(null, 0)
        library.g_main_loop_run(main_loop)
        main_loop = null
    }

    override fun hide() {
        main_loop?.also { loop ->
            library.g_main_loop_quit(loop)
        }
    }

    override fun release() {
        hide()
    }

    override fun addClickCallback(onClick: ClickCallback) {
        // TODO
    }

    override fun addButton(label: String, onClick: ButtonCallback?) {
        val item: KJnaTypedPointer<_GtkWidget> = library.gtk_menu_item_new_with_label(label)!!

        if (onClick != null) {
            library.g_signal_connect_data(
                item,
                "activate",
                KJnaFunctionPointer.createDataParamFunction1(),
                KJnaFunctionPointer.getDataParam(onClick),
                null,
                GConnectFlags.G_CONNECT_DEFAULT
            )
        }

        library.gtk_menu_shell_append(menu as KJnaTypedPointer<_GtkMenuShell>, item)
        library.gtk_widget_show(item)
    }

    class AppIndicatorScrollEvent {
        fun invoke(p0: KJnaPointer, delta: Int, direction: UInt, function: KJnaFunctionPointer) {

        }
    }

    override fun addScrollCallback(onScroll: (delta: Int, direction: Int) -> Unit) {
        val (function, data) = getScrollEventFunction(onScroll)

        library.g_signal_connect_data(
            indicator,
            "scroll-event",
            function,
            data,
            null,
            GConnectFlags.G_CONNECT_DEFAULT
        )
    }

    init {
        getLogWriterFunction(
            { level: GLogLevelFlags ->
                if (level == GLogLevelFlags.G_LOG_LEVEL_ERROR || level == GLogLevelFlags.G_LOG_LEVEL_CRITICAL) {
                    GLogWriterOutput.G_LOG_WRITER_UNHANDLED
                }
                else {
                    GLogWriterOutput.G_LOG_WRITER_HANDLED
                }
            }
        ).also { (function, data) ->
            // Effectively disables GTK warnings
            library.g_log_set_writer_func(
                function,
                data,
                null
            )
        }

        KJnaMemScope.confined {
            val gtk_argc: KJnaTypedPointer<Int> = alloc()
            if (library.gtk_init_check(gtk_argc, null) == 0) {
                throw RuntimeException("Call to gtk_init_check failed")
            }
        }

        indicator = library.app_indicator_new("spms", "indicator-messages", AppIndicatorCategory.APP_INDICATOR_CATEGORY_APPLICATION_STATUS)!!

        library.app_indicator_set_title(indicator, name)
        library.app_indicator_set_status(indicator, AppIndicatorStatus.APP_INDICATOR_STATUS_ACTIVE)

        val path: MutableList<String> = icon_path.toMutableList()

        var filename: String = path.removeLast()
        val last_dot: Int = filename.lastIndexOf('.')
        if (last_dot != -1) {
            filename = filename.substring(0, last_dot)
        }

        library.app_indicator_set_icon_theme_path(indicator, '/' + path.joinToString("/"))
        library.app_indicator_set_icon_full(indicator, filename, name)

        menu = library.gtk_menu_new() as KJnaTypedPointer<_GtkMenu>
    }

    companion object {
        fun isAvailable(): Boolean {
            return KJnaUtils.getEnv("XDG_CURRENT_DESKTOP") != null
        }
    }
}

internal expect fun getScrollEventFunction(onScroll: (delta: Int, direction: Int) -> Unit): Pair<KJnaFunctionPointer, KJnaPointer?>
internal expect fun getLogWriterFunction(handleMessage: (GLogLevelFlags) -> GLogWriterOutput): Pair<KJnaFunctionPointer, KJnaPointer?>
