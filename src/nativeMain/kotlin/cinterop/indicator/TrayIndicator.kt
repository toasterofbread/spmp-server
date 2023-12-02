package cinterop.indicator

typealias ClickCallback = () -> Unit
typealias ButtonCallback = () -> Unit
typealias ScrollCallback = (delta: Int, direction: Int) -> Unit

interface TrayIndicator {
    fun show()
    fun hide()

    fun release() {}

    fun addClickCallback(onClick: ClickCallback)
    fun addButton(label: String, onClick: ButtonCallback)
    fun addScrollCallback(onScroll: ScrollCallback)

    companion object {
        fun create(name: String, icon_path: List<String>): TrayIndicator? =
            when (Platform.osFamily) {
                OsFamily.LINUX -> LibAppIndicator(name, icon_path)
                else -> null
            }
    }
}
