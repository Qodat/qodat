package stan.qodat.scene.runescape.widget.component

sealed class ButtonType(val id: Int) {
    data object None : ButtonType(0)
    data object Ok : ButtonType(1)
    data object Target : ButtonType(2)
    data object Close : ButtonType(3)
    data object Check : ButtonType(4)
    data object Toggle : ButtonType(5)
    data object Pause : ButtonType(6)
}
