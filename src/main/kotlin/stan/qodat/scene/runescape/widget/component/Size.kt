package stan.qodat.scene.runescape.widget.component

sealed class Size(val id: Int) {
    object abs : Size(0)
    object minus : Size(1)
    object proportion : Size(2)
    object unknown : Size(3)
    object aspect : Size(4)

    companion object {
        fun fromId(id: Int) = when (id) {
            0 -> abs
            1 -> minus
            2 -> proportion
            3 -> unknown
            4 -> aspect
            else -> throw IllegalArgumentException("Invalid Size id: $id")
        }
    }
}
