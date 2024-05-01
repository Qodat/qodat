package stan.qodat.scene.runescape.widget.component

sealed class Pos(val id: Int) {

    data object abs_left : Pos(0)
    data object abs_centre : Pos(1)
    data object abs_right : Pos(2)
    data object proportion_left : Pos(3)
    data object proportion_centre : Pos(4)
    data object proportion_right : Pos(5)

    data object abs_top : Pos(0)
    data object abs_bottom : Pos(2)
    data object proportion_top : Pos(3)
    data object proportion_bottom : Pos(5)

    companion object {
        fun fromIdHor(id: Int) = when (id) {
            0 -> abs_left
            1 -> abs_centre
            2 -> abs_right
            3 -> proportion_left
            4 -> proportion_centre
            5 -> proportion_right
            else -> throw IllegalArgumentException("Invalid Pos id: $id")
        }
        fun fromIdVer(id: Int) = when (id) {
            0 -> abs_top
            1 -> abs_centre
            2 -> abs_bottom
            3 -> proportion_top
            4 -> proportion_centre
            5 -> proportion_bottom
            else -> throw IllegalArgumentException("Invalid Pos id: $id")
        }
    }
}
