package dev.toastbits.spms.indicator

actual class TrayIndicator actual constructor(name: String, icon_path: List<String>) {
    actual fun show() { TODO() }
    actual fun hide() { TODO() }
    
    actual fun release() { TODO() }

    actual fun addClickCallback(onClick: ClickCallback) { TODO() }
    actual fun addButton(label: String, onClick: ButtonCallback?) { TODO() }
    actual fun addScrollCallback(onScroll: (delta: Int, direction: Int) -> Unit) { TODO() }
    
    actual companion object {
        actual fun isAvailable(): Boolean = false
    }
}
