@file:OptIn(KJnaUnimplementedFunctionPointer::class)
@file:Suppress("INVISIBLE_MEMBER")
package dev.toastbits.spms.indicator

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.IntVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.posix.LC_NUMERIC
import platform.posix.setlocale
import platform.posix.getenv
import dev.toastbits.kjna.runtime.KJnaTypedPointer
import dev.toastbits.kjna.runtime.KJnaFunction
import dev.toastbits.kjna.runtime.KJnaUnimplementedFunctionPointer
import dev.toastbits.kjna.runtime.KJnaMemScope
import gen.libappindicator.LibAppIndicator
import gen.libappindicator.cinterop.*
import kjna.struct._AppIndicator
import kjna.struct._GtkMenu
import kjna.struct._GtkMenuShell
import kjna.struct._GMainLoop
import kjna.struct._GtkWidget
import kjna.enum.GConnectFlags
import kjna.enum.AppIndicatorCategory
import kjna.enum.AppIndicatorStatus

@OptIn(ExperimentalForeignApi::class)
actual class TrayIndicator actual constructor(name: String, icon_path: List<String>) {
    private val mem_scope = KJnaMemScope()
    private val indicator: KJnaTypedPointer<_AppIndicator>
    private val menu: KJnaTypedPointer<_GtkMenu>
    private var main_loop: KJnaTypedPointer<_GMainLoop>? = null

    private val library: LibAppIndicator = LibAppIndicator()

    actual fun show() {
        library.app_indicator_set_menu(indicator, menu)

        main_loop = library.g_main_loop_new(null, 0)
        library.g_main_loop_run(main_loop)
        main_loop = null
    }

    actual fun hide() {
        main_loop?.also { loop ->
            library.g_main_loop_quit(loop)
        }
    }

    actual fun release() {
        hide()
    }

    actual fun addClickCallback(onClick: ClickCallback) {
        // TODO
    }

    actual fun addButton(label: String, onClick: ButtonCallback?) {
        val item: KJnaTypedPointer<_GtkWidget> = library.gtk_menu_item_new_with_label(label)!!

        if (onClick != null) {
            // val callback_index: CPointer<IntVar> = button_callbacks.addCallback(onClick, mem_scope)
            library.g_signal_connect_data(
                item,
                "activate",
                KJnaFunction.singleParamFunction(),
                KJnaFunction.singleParamData(onClick),
                null,
                GConnectFlags.G_CONNECT_DEFAULT
            )
        }

        library.gtk_menu_shell_append(menu as KJnaTypedPointer<_GtkMenuShell>, item)
        library.gtk_widget_show(item)
    }

    actual fun addScrollCallback(onScroll: (delta: Int, direction: Int) -> Unit) {
        val callback_index: KJnaTypedPointer<Int> = scroll_callbacks.addCallback(onScroll, mem_scope)

        library.g_signal_connect_data(
            indicator,
            "scroll-event",
            KJnaFunction(
                staticCFunction { _: CPointer<*>, delta: gint, direction: guint, index: CPointer<IntVar> ->
                    scroll_callbacks.getCallback(index).invoke(delta, if (direction == 1U) 1 else -1)
                }.reinterpret()
            ),
            callback_index,
            null,
            GConnectFlags.G_CONNECT_DEFAULT
        )
    }

    init {
        // Effectively disables GTK warnings
        library.g_log_set_writer_func(
            KJnaFunction(
                staticCFunction { level: GLogLevelFlags ->
                    if (level == G_LOG_LEVEL_ERROR || level == G_LOG_LEVEL_CRITICAL) {
                        G_LOG_WRITER_UNHANDLED
                    }
                    else {
                        G_LOG_WRITER_HANDLED
                    }
                }.reinterpret()
            ),
            null,
            null
        )

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

        setlocale(LC_NUMERIC, "C")
    }

    actual companion object {
        actual fun isAvailable(): Boolean {
            return getenv("XDG_CURRENT_DESKTOP") != null
        }

        // private val click_callbacks: MutableList<ClickCallback> = mutableListOf()
        // private val button_callbacks: MutableList<ButtonCallback> = mutableListOf()
        private val scroll_callbacks: MutableList<ScrollCallback> = mutableListOf()

        private fun <T> MutableList<T>.addCallback(callback: T, mem_scope: KJnaMemScope): KJnaTypedPointer<Int> {
            val callback_index: KJnaTypedPointer<Int> = mem_scope.alloc()
            callback_index.set(size)
            add(callback)
            return callback_index
        }

        private fun <T> MutableList<T>.getCallback(index: CPointer<IntVar>): T =
            get(index.pointed.value)
    }
}
