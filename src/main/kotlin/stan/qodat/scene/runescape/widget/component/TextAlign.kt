package stan.qodat.scene.runescape.widget.component

sealed class TextAlign(val id: Int) {
    data object left : TextAlign(0)
    data object centre : TextAlign(1)
    data object right : TextAlign(2)
    data object justify : TextAlign(3)
    data object top : TextAlign(0)
    data object bottom : TextAlign(2)
}
