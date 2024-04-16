package stan.qodat.scene.runescape.widget.component

sealed class Colour(val value: Int) {
    object red : Colour(16711680)
    object green : Colour(65280)
    object blue : Colour(255)
    object yellow : Colour(16776960)
    object magenta : Colour(16711935)
    object cyan : Colour(65535)
    object white : Colour(16777215)
    object black : Colour(0)
    class Hex(value: Int) : Colour(value)
}
