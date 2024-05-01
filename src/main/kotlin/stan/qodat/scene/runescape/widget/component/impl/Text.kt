package stan.qodat.scene.runescape.widget.component.impl

import stan.qodat.scene.runescape.widget.component.*

class Text(
    override var name: String? = null,
    override var x: Int = 0,
    override var y: Int = 0,
    override var width: Int = 0,
    override var height: Int = 0,
    override var hSize: Size = Size.abs,
    override var vSize: Size = Size.abs,
    override var hPos: Pos = Pos.abs_left,
    override var vPos: Pos = Pos.abs_top
) : Component<TextRef>() {
    var trans: Int = 0
    var text: String? = null
    var itemId: Int? = null
    var itemAmount: Int? = null
    var shadowed: Boolean = false
    var colour: Colour? = null
    var font: Font? = null
    var hAlign: TextAlign = TextAlign.left
    var vAlign: TextAlign = TextAlign.top
    var lineHeight: Int = 0
    override fun toReference(name: String) =
        TextRef(name, this)
}
