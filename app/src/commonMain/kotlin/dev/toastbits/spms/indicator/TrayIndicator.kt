package dev.toastbits.spms.indicator

typealias ClickCallback = () -> Unit
typealias ButtonCallback = () -> Unit
typealias ScrollCallback = (delta: Int, direction: Int) -> Unit

expect class TrayIndicator(name: String, icon_path: List<String>) {
    fun show()
    fun hide()

    fun release()

    fun addClickCallback(onClick: ClickCallback)
    fun addButton(label: String, onClick: ButtonCallback?)
    fun addScrollCallback(onScroll: ScrollCallback)

    companion object {
        fun isAvailable(): Boolean
    }
}
