package stan.qodat.scene.runescape.widget.component.impl

import stan.qodat.scene.runescape.widget.component.*

class Graphic(
    override var name: String? = null,
    override var x: Int = 0,
    override var y: Int = 0,
    override var width: Int = 0,
    override var height: Int = 0,
    override var hSize: Size = Size.abs,
    override var vSize: Size = Size.abs,
    override var hPos: Pos = Pos.abs_left,
    override var vPos: Pos = Pos.abs_top
) : Component<GraphicRef>() {

    var sprite: Sprites? = null
    var secondarySprite: Sprites? = null
    var repeatSprite: Boolean = false
    var spriteRotation: Int = 0

    var flipH: Boolean = false
    var flipV: Boolean = false
    var borderThickness: Int = 0

    var itemId: Int? = null
    var itemAmount: Int? = null
    var shadowColour: Colour? = null
    var colour: Colour? = null

    var trans: Int = 0
    var op1: String? = null

    override fun toReference(name: String) =
        GraphicRef(name, this)
}
