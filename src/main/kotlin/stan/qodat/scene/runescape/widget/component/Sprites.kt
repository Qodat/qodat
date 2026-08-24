package stan.qodat.scene.runescape.widget.component

sealed class Sprites(open val groupId: Int, open val index: Int = 0) {

    data object RedCircleCrossedDiagonally : Sprites(940)
    data object OpenHandWithCoinPile : Sprites(1112)
    data object SearchIcon : Sprites(1113)

    sealed class RadioButton(spriteId: Int) : Sprites(spriteId) {
        data object DarkEmpty : RadioButton(697)
        data object DarkRedX : RadioButton(698)
        data object DarkGreenCheck : RadioButton(699)
        data object DarkRedCheck : RadioButton(1192)
        data object LightEmpty : RadioButton(1211)
        data object LightRedX : RadioButton(1212)
        data object LightGreenCheck : RadioButton(1213)
        data object LightRedCheck : RadioButton(1214)
    }

    data class ById(override val groupId: Int) : Sprites(groupId)
}
