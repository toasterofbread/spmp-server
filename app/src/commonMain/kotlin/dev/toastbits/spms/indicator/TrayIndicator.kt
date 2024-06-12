package dev.toastbits.spms.indicator

interface TrayIndicator {
    fun show()
    fun hide()

    fun release()

    fun addClickCallback(onClick: ClickCallback)
    fun addButton(label: String, onClick: ButtonCallback?)
    fun addScrollCallback(onScroll: ScrollCallback)
}

typealias ClickCallback = () -> Unit
typealias ButtonCallback = () -> Unit
typealias ScrollCallback = (delta: Int, direction: Int) -> Unit
