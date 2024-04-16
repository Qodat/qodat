package stan.qodat.scene.runescape.widget.component.impl

import stan.qodat.scene.runescape.widget.component.Component
import stan.qodat.scene.runescape.widget.component.LineRef
import stan.qodat.scene.runescape.widget.component.Pos
import stan.qodat.scene.runescape.widget.component.Size

class Line(
    override var name: String? = null,
    override var x: Int = 0,
    override var y: Int = 0,
    override var width: Int = 0,
    override var height: Int = 0,
    override var hSize: Size = Size.abs,
    override var vSize: Size = Size.abs,
    override var hPos: Pos = Pos.abs_left,
    override var vPos: Pos = Pos.abs_top
) : Component<LineRef>() {

    override fun toReference(name: String) =
        LineRef(name, this)
}
